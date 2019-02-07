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
    Behaviors.receive[Command]{(ctx, msg) =>z
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
          val b = ByteString(t.ws)
          val buffer = new MiddleBufferInJvm(b.asByteBuffer)
          val msg = bytesDecode[Protocol.GameMessage](buffer) match{
            case Right(req) =>
              val sendBuffer = new MiddleBufferInJvm(409600)
              val a = req.fillMiddleBuffer(sendBuffer).result()
              req match{
                case m:GridDataSync=>
                  sync = sync + a.length
                case m:FeedApples=>
                  food = food + a.length
                case m:Id =>
                  connect = connect + a.length
                case m:PlayerJoin =>
                  connect = connect + a.length
                case m:Ranks =>
                  rank = rank + a.length
                case m:MyRank =>
                  rank = rank + a.length
                case m:UserMerge=>
                  merge = merge + a.length
                case m:UserCrash=>
                  crash = crash + a.length
                case m:Pong =>
                  ping = ping + a.length
                case m:MP =>
                  mouse = mouse + a.length
                case m:KC=>
                  key = key + a.length
                case m:AddVirus=>
                  virus = virus + a.length
                case m:PlayerSplit=>
                  split = split + a.length
                case x =>
                  other = other + a.length

              }
              if(System.currentTimeMillis() - timer > period){
                timer = System.currentTimeMillis()
                val total =  sync + food + connect + rank + merge +crash +virus + ping + mouse + key + other

                log.info(s"STATISTICS:" +
                  s"TOTAL:$total" +
                  s"SYNC:$sync" +
                  s"FOOD:$food" +
                  s"VIRUS:$virus" +
                  s"MOUSE:$mouse" +
                  s"KEY:$key" +
                  s"CONNECT:$connect" +
                  s"PING:$ping" +
                  s"RANK:$rank" +
                  s"MERGE:$merge" +
                  s"CRASH:$crash" +
                  s"SPLIT:$split" +
                  s"OTHER:$other")
                sync = 0.0
                food = 0.0
                connect = 0.0
                rank = 0.0
                merge = 0.0
                crash = 0.0
                virus = 0.0
                ping = 0.0
                mouse = 0.0
                key = 0.0
                split = 0.0
                other = 0.0
              }

            case Left(e) =>
          }
          BinaryMessage.Strict(ByteString(t.ws))
        case t: Protocol.ReplayFrameData =>
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