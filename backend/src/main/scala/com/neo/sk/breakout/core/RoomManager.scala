package com.neo.sk.breakout.core

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.neo.sk.breakout.common.AppSettings
import org.slf4j.LoggerFactory
import akka.stream.scaladsl.Flow
import scala.collection.mutable
import com.neo.sk.breakout.ptcl.UserProtocol.BaseUserInfo
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import com.neo.sk.breakout.shared.ptcl.Protocol
import akka.util.ByteString
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.stream.OverflowStrategy
import com.neo.sk.breakout.Boot.{executor, roomManager, timeout, userManager}
import com.neo.sk.breakout.core.UserActor.JoinRoom
import org.seekloud.byteobject.ByteObject._

/**
  * create by zhaoyin
  * 2019/2/1  5:34 PM
  */
object RoomManager {

  import org.seekloud.byteobject.MiddleBufferInJvm

  private val log=LoggerFactory.getLogger(this.getClass)

  trait Command
  case object TimeKey
  case object TimeOut extends Command
  case class LeftRoom(playerInfo: BaseUserInfo) extends Command
  case class FrontActor(value: ActorRef[Protocol.WsMsgSource]) extends Command
  case class WorldLeft[U](actorRef: ActorRef[U]) extends Command
  private case object UnKnowAction extends Command
  case class WebSocketMsg(reqOpt: Option[Protocol.UserAction]) extends Command
  case object CompleteMsgFront extends Command
  case class DispatchMsg(msg:Protocol.WsMsgSource) extends Command

  case class FailMsgFront(ex: Throwable) extends Command

  /**创建websocket**/
  final case class GetWorldWebSocketFlow(replyTo:ActorRef[Flow[Message,Message,Any]]) extends Command

  private def sink(actor: ActorRef[RoomManager.Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = CompleteMsgFront,
    onFailureMessage = FailMsgFront.apply
  )

  private def getWebSocketFlow(roomManager: ActorRef[RoomManager.Command]):Flow[Message,Message,Any] = {
    import org.seekloud.byteobject.ByteObject._

    import scala.language.implicitConversions

    implicit def parseJsonString2WsMsgFront(s:String): Option[Protocol.UserAction] = {

      try {
        import io.circe.generic.auto._
        import io.circe.parser._
        val wsMsg = decode[Protocol.UserAction](s).right.get
        Some(wsMsg)
      }catch {
        case e: Exception =>
          log.warn(s"parse front msg failed when json parse,s=${s}")
          None
      }
    }

    Flow[Message]
      .collect {
        case BinaryMessage.Strict(msg)=>
          val buffer = new MiddleBufferInJvm(msg.asByteBuffer)
          bytesDecode[Protocol.UserAction](buffer) match {
            case Right(req) =>  RoomManager.WebSocketMsg(Some(req))
            case Left(e) =>
              log.error(s"decode binaryMessage failed,error:${e.message}")
              RoomManager.WebSocketMsg(None)
          }
        case TextMessage.Strict(msg) =>
          log.debug(s"msg from webSocket: $msg")
          RoomManager.WebSocketMsg(None)

        // unpack incoming WS text messages...
        // This will lose (ignore) messages not received in one chunk (which is
        // unlikely because chat messages are small) but absolutely possible
      }
      .via(RoomManager.flow(roomManager))
      .map{
        case t:Protocol.Wrap =>
          BinaryMessage.Strict(ByteString(t.ws))
        case x =>
          log.debug(s"akka stream receive unknown msg=${x}")
          TextMessage.apply("")
      }.withAttributes(ActorAttributes.supervisionStrategy(decider))
  }

  private val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      log.error(s"ws stream failed with $e")
      Supervision.Resume
  }

  def flow(actor:ActorRef[RoomManager.Command]):Flow[WebSocketMsg, Protocol.WsMsgSource,Any] = {
    val in = Flow[RoomManager.WebSocketMsg]
      .map {a=>
        val req = a.reqOpt.get
        req match{
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
      ).mapMaterializedValue(outActor => actor ! FrontActor(outActor))
    Flow.fromSinkAndSource(in, out)
  }

  /**************/

  def create(): Behavior[Command] ={
    log.debug(s"RoomManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            val roomIdGenerator = new AtomicLong(1L)
            //一开始没有可用房间  roomId->(playerId,playerId)
            val roomInUse = mutable.HashMap[Long,List[String]]()
            val subscribersMap = mutable.HashMap[String,ActorRef[Protocol.WsMsgSource]]()
            idle(roomIdGenerator,roomInUse,subscribersMap)
        }
    }
  }

  def idle(roomIdGenerator:AtomicLong,
           roomInUse:mutable.HashMap[Long,List[String]],
           subscribersMap:mutable.HashMap[String,ActorRef[Protocol.WsMsgSource]]
          )
          (implicit timer:TimerScheduler[Command]) =
    Behaviors.receive[Command]{
      (ctx, msg) =>
        msg match {
          case JoinRoom(playerInfo,roomIdOpt,userActor) =>
            if(roomIdOpt.isDefined){
              //加入房间
              roomInUse.get(roomIdOpt.get) match{
                case Some(ls) =>
                  //该房间是否满员
                  if(ls.length < AppSettings.limitCount){
                    roomInUse.put(roomIdOpt.get,playerInfo.userId :: ls)
                    getRoomActor(ctx,roomIdOpt.get) ! RoomActor.JoinRoom(playerInfo,userActor)
                  }else{
                    //TODO 告诉用户，该房间已满

                  }
                case None =>
                  //TODO 告诉用户，该房间不存在or直接创建一个新房间？
              }
            }else{
              //创建房间
              var roomId = roomIdGenerator.getAndIncrement()
              while(roomInUse.exists(_._1 == roomId))roomId = roomIdGenerator.getAndIncrement()
              roomInUse.put(roomId,List(playerInfo.userId))
              getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(playerInfo,userActor)
            }
            //TODO
            Behaviors.same

          case LeftRoom(playerInfo) =>
            roomInUse.find(_._2.contains(playerInfo.userId)) match{
              case Some(t) =>
                roomInUse.put(t._1,t._2.filterNot(_ == playerInfo.userId))
                getRoomActor(ctx,t._1) ! UserActor.Left(playerInfo)
                if(roomInUse(t._1).isEmpty) roomInUse.remove(t._1)
              case None => log.debug(s"该玩家不在任何房间")
            }
            //TODO
            Behaviors.same

          case GetWorldWebSocketFlow(replyTo) =>
            replyTo ! getWebSocketFlow(roomManager)
            Behaviors.same

          case FrontActor(frontActor) =>
            //TODO 向前端发送房间数
            ctx.watchWith(frontActor,WorldLeft(frontActor))
            subscribersMap.put()
            frontActor !
            Behaviors.same

          case x=>
            log.debug(s"msg can't handle with ${x}")
            Behaviors.unhandled
        }
    }


  private def getRoomActor(ctx: ActorContext[Command],roomId:Long):ActorRef[RoomActor.Command] = {
    val childName = s"RoomActor-$roomId"
    ctx.child(childName).getOrElse{
      ctx.spawn(RoomActor.create(roomId),childName)
    }.upcast[RoomActor.Command]
  }

  def dispatch(subscribers:mutable.HashMap[String,ActorRef[UserActor.Command]])(msg:Protocol.GameMessage)(implicit sendBuffer:MiddleBufferInJvm) = {
    val isKillMsg = msg.isInstanceOf[Protocol.UserDeadMessage]
    subscribers.values.foreach( _ ! UserActor.DispatchMsg(Protocol.Wrap(msg.asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result(),isKillMsg)))
  }


}
