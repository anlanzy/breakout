package com.neo.sk.breakout.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.OverflowStrategy
import org.slf4j.LoggerFactory
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.neo.sk.breakout.shared.ptcl.Protocol
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration

/**
  * create by zhaoyin
  * 2019/2/1  5:35 PM
  */
object UserActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  private final case object BehaviorChangeKey


  trait Command

  case class UserFrontActor(actor: ActorRef[Protocol.WsMsgSource]) extends Command

  case object ChangeBehaviorToInit extends Command

  case class WebSocketMsg(reqOpt: Option[Protocol.UserAction]) extends Command

  case object StartGame extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut  = TimeOut("busy time error")
                                  )(
                                    implicit stashBuffer: StashBuffer[Command],
                                    timer:TimerScheduler[Command]) = {
    log.debug(s"${ctx.self.path}  becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey,timeOut,_))
    stashBuffer.unstashAll(ctx,behavior)
  }
  private def sink(actor: ActorRef[Command],recordId:Long) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  def flow(id:String,name:String,recordId:Long,actor:ActorRef[UserActor.Command]):Flow[WebSocketMsg, Protocol.WsMsgSource,Any] = {
    val in = Flow[UserActor.WebSocketMsg]
      .map {a=>
        val req = a.reqOpt.get
        req match{
          case KC(_,keyCode,f,n)=>
            log.debug(s"键盘事件$keyCode")
            Key(keyCode,f,n)

          case MP(_,clientX,clientY,f,n)=>
            Mouse(clientX,clientY,f,n)

          case Ping(timestamp)=>
            NetTest(id,timestamp)

//          case ReLiveMsg(frame) =>
//            UserReLiveMsg(frame)

          case Protocol.CreateRoom =>
            CreateRoom

          case Protocol.JoinRoom(roomIdOp) =>
            log.info("JoinRoom!!!!!!")
            StartGame(roomIdOp)

          case _=>
            UnKnowAction
        }
      }
      .to(sink(actor,recordId))

    val out =
      ActorSource.actorRef[Protocol.WsMsgSource](
        completionMatcher = {
          case Protocol.CompleteMsgServer ⇒
        },
        failureMatcher = {
          case Protocol.FailMsgServer(e)  ⇒ e
        },
        bufferSize = 128,
        overflowStrategy = OverflowStrategy.dropHead
      ).mapMaterializedValue(outActor => actor ! UserFrontActor(outActor))
    Flow.fromSinkAndSource(in, out)
  }

  def create(userInfo:PlayerInfo):Behavior[Command] = {
    Behaviors.setup[Command]{ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command]{ implicit timer =>
        implicit val sendBuffer = new MiddleBufferInJvm(8192)
        switchBehavior(ctx,"idle", idle(userInfo,frontActor))
      }
    }
  }

  private def idle(
                    userInfo:PlayerInfo,
                    frontActor: ActorRef[Protocol.WsMsgSource]
                  )(
                    implicit stashBuffer:StashBuffer[Command],
                    sendBuffer:MiddleBufferInJvm,
                    timer:TimerScheduler[Command]
                  ):Behavior[Command] =
    Behaviors.receive[Command]{(ctx,msg) =>
      msg match {
        case UserFrontActor(frontActor) =>
          ctx.watchWith(frontActor,UserLeft(frontActor))
          idle(userInfo,System.currentTimeMillis(),frontActor)

        case StartGame(roomIdOp) =>
          roomManager ! UserActor.JoinRoom(userInfo,ctx.self)
          Behaviors.same

        case JoinRoomSuccess(roomId,roomActor)=>
          frontActor ! Protocol.Wrap(Protocol.JoinRoomSuccess(userInfo.playerId,roomId).asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result())
          //          frontActor ! Protocol.JoinRoomSuccess(userInfo.playerId,roomId)
          switchBehavior(ctx,"play",play(userInfo,frontActor,roomActor))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          Behaviors.stopped

        case ChangeBehaviorToInit=>
          Behaviors.same

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy,msg=${m}")
          Behaviors.stopped

        case unknowMsg =>
          stashBuffer.stash(unknowMsg)
          Behavior.same

      }

    }

  private def play(
                    userInfo:PlayerInfo,
                    frontActor:ActorRef[Protocol.WsMsgSource],
                    roomActor: ActorRef[RoomActor.Command])(
                    implicit stashBuffer:StashBuffer[Command],
                    timer:TimerScheduler[Command],
                    sendBuffer:MiddleBufferInJvm
                  ): Behavior[Command] =
    Behaviors.receive[Command]{ (ctx, msg) =>
      msg match {

        case ChangeBehaviorToInit=>
          frontActor ! Protocol.Wrap(Protocol.RebuildWebSocket.asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result())
          roomManager ! RoomManager.LeftRoom(userInfo)
          ctx.unwatch(frontActor) //这句是必须的，将不会受到UserLeft消息
          switchBehavior(ctx,"init",init(userInfo),InitTime,TimeOut("init"))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          roomManager ! RoomManager.LeftRoom(userInfo)
          Behaviors.stopped

        case unKnowMsg =>
          stashBuffer.stash(unKnowMsg)
          Behavior.same
      }
    }

}
