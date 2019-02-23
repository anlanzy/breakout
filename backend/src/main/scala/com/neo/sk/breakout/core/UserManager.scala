package com.neo.sk.breakout.core

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.Flow
import akka.stream.{ActorAttributes, Supervision}
import akka.util.ByteString
import org.slf4j.LoggerFactory
import com.neo.sk.breakout.ptcl.UserProtocol
import com.neo.sk.breakout.shared.ptcl.Protocol
/**
  * create by zhaoyin
  * 2019/2/1  5:35 PM
  */
object UserManager {

  import org.seekloud.byteobject.MiddleBufferInJvm

  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  final case class GetWebSocketFlow(playerInfo: Option[UserProtocol.BaseUserInfo] = None, roomIdOpt:Option[Long] = None, replyTo:ActorRef[Flow[Message,Message,Any]]) extends Command

  def create(): Behavior[Command] = {
    log.debug(s"UserManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            idle()
        }
    }
  }

  private def idle()(
    implicit timer: TimerScheduler[Command]
  ):Behavior[Command] = {
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case GetWebSocketFlow(playerInfoOpt,roomIdOpt,replyTo) =>
          val playerInfo = playerInfoOpt.get
          getUserActorOpt(ctx, playerInfo.userId) match {
            case Some(userActor) =>
              userActor ! UserActor.ChangeBehaviorToInit
            case None =>
          }
          val userActor = getUserActor(ctx,playerInfo)
          replyTo ! getWebSocketFlow(playerInfo,userActor)
          userActor ! UserActor.StartGame(roomIdOpt)
          Behaviors.same
      }
    }
  }

  private def getWebSocketFlow(playerInfo: UserProtocol.BaseUserInfo,userActor: ActorRef[UserActor.Command]):Flow[Message,Message,Any] = {
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
            case Right(req) =>  UserActor.WebSocketMsg(Some(req))
            case Left(e) =>
              log.error(s"decode binaryMessage failed,error:${e.message}")
              UserActor.WebSocketMsg(None)
          }
        case TextMessage.Strict(msg) =>
          log.debug(s"msg from webSocket: $msg")
          UserActor.WebSocketMsg(None)

        // unpack incoming WS text messages...
        // This will lose (ignore) messages not received in one chunk (which is
        // unlikely because chat messages are small) but absolutely possible
      }
      .via(UserActor.flow(playerInfo.userId,playerInfo.userName,userActor))
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




  private def getUserActor(ctx: ActorContext[Command],userInfo:UserProtocol.BaseUserInfo):ActorRef[UserActor.Command] = {
    val childName = s"UserActor-${userInfo.userId}"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(UserActor.create(userInfo),childName)
      ctx.watchWith(actor,ChildDead(childName,actor))
      actor
    }.upcast[UserActor.Command]
  }
  private def getUserActorOpt(ctx: ActorContext[Command],id:String):Option[ActorRef[UserActor.Command]] = {
    val childName = s"UserActor-${id}"
    ctx.child(childName).map(_.upcast[UserActor.Command])
  }



}
