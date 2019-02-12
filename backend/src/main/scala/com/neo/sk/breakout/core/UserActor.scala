package com.neo.sk.breakout.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.stream.OverflowStrategy
import org.slf4j.LoggerFactory
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import scala.concurrent.duration._
import scala.language.implicitConversions

import com.neo.sk.breakout.shared.ptcl.Protocol._
import com.neo.sk.breakout.ptcl.UserProtocol._
import com.neo.sk.breakout.Boot.roomManager
import com.neo.sk.breakout.shared.ptcl.Protocol
import com.neo.sk.breakout.core.UserActor.StartGame


/**
  * create by zhaoyin
  * 2019/2/1  5:35 PM
  */
object UserActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  private final val InitTime = Some(5.minutes)

  private final case object BehaviorChangeKey

  trait Command

  case class UserFrontActor(actor: ActorRef[Protocol.WsMsgSource]) extends Command

  case object ChangeBehaviorToInit extends Command

  case class DispatchMsg(msg:Protocol.WsMsgSource) extends Command

  case class JoinRoom(playerInfo: BaseUserInfo,roomIdOpt:Option[Long] = None,userActor:ActorRef[UserActor.Command]) extends Command with RoomManager.Command

  case class WebSocketMsg(reqOpt: Option[Protocol.UserAction]) extends Command

  case class TimeOut(msg: String) extends Command

  case class StartGame(roomIdOpt:Option[Long]) extends Command

  case object CompleteMsgFront extends Command

  case class FailMsgFront(ex: Throwable) extends Command

  case class Key(keyCode: Int,frame:Int,n:Int) extends Command with RoomActor.Command

  case class Mouse(clientX:Short,clientY:Short,frame:Int,n:Int) extends Command with RoomActor.Command

  case class NetTest(id: String, createTime: Long) extends Command with RoomActor.Command

  case class UserLeft[U](actorRef: ActorRef[U]) extends Command

  case class JoinRoomSuccess(roomId:Long, roomActor: ActorRef[RoomActor.Command]) extends Command with RoomManager.Command

  case class Left(playerInfo: BaseUserInfo) extends Command with RoomActor.Command

  private case object UnKnowAction extends Command

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
  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  def flow(id:String,name:String,actor:ActorRef[UserActor.Command]):Flow[WebSocketMsg, Protocol.WsMsgSource,Any] = {
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

          case Protocol.CreateRoom =>
            CreateRoom

          case Protocol.JoinRoom(roomIdOp) =>
            log.info("JoinRoom!!!!!!")
            StartGame(roomIdOp)

          case _=>
            UnKnowAction
        }
      }
      .to(sink(actor))

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

  def create(userInfo:BaseUserInfo):Behavior[Command] = {
    Behaviors.setup[Command]{ctx =>
      log.debug(s"${ctx.self.path} is starting...")
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command]{ implicit timer =>
        implicit val sendBuffer = new MiddleBufferInJvm(8192)
        switchBehavior(ctx,"init",init(userInfo),InitTime,TimeOut("init"))
      }
    }
  }
  private def init(userInfo:BaseUserInfo)(
    implicit stashBuffer:StashBuffer[Command],
    sendBuffer:MiddleBufferInJvm,
    timer:TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case UserFrontActor(frontActor) =>
          ctx.watchWith(frontActor,UserLeft(frontActor))
          switchBehavior(ctx,"idle", idle(userInfo,frontActor))

        case UserLeft(actor) =>
          ctx.unwatch(actor)
          Behaviors.stopped

        case UnKnowAction =>
          Behavior.same

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

  private def idle(
                    userInfo:BaseUserInfo,
                    frontActor: ActorRef[Protocol.WsMsgSource]
                  )(
                    implicit stashBuffer:StashBuffer[Command],
                    sendBuffer:MiddleBufferInJvm,
                    timer:TimerScheduler[Command]
                  ):Behavior[Command] =
    Behaviors.receive[Command]{(ctx,msg) =>
      msg match {
        case StartGame(roomIdOpt) =>
          roomManager ! UserActor.JoinRoom(userInfo,roomIdOpt,ctx.self)
          Behaviors.same

        case JoinRoomSuccess(roomId,roomActor)=>
          frontActor ! Protocol.Wrap(Protocol.JoinRoomSuccess(userInfo.userId,roomId).asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result())
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
                    userInfo:BaseUserInfo,
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
