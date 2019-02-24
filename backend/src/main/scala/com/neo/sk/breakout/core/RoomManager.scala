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
import com.neo.sk.breakout.shared.ptcl.{ApiProtocol, Protocol}
import akka.util.ByteString
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.stream.OverflowStrategy
import com.neo.sk.breakout.Boot.{executor, roomManager, timeout, userManager}
import com.neo.sk.breakout.core.UserActor.{CreateRoom, JoinRoom}
import com.neo.sk.breakout.shared.ptcl.ApiProtocol.RoomInUse
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
  case class CreateRoom(roomName:String,types:Int,playerInfo: BaseUserInfo,userActor:ActorRef[UserActor.Command]) extends Command
  case class GetRoomList(replyTo:ActorRef[RoomInUse]) extends Command


  def create(): Behavior[Command] ={
    log.debug(s"RoomManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            val roomIdGenerator = new AtomicLong(1L)
            //一开始没有可用房间  roomId->(playerId,playerId)
            val roomInUse = mutable.HashMap[Long,(String,Int,List[String])]()
            val subscribersMap = mutable.HashMap[String,ActorRef[Protocol.WsMsgSource]]()
            idle(roomIdGenerator,roomInUse,subscribersMap)
        }
    }
  }

  def idle(roomIdGenerator:AtomicLong,
           roomInUse:mutable.HashMap[Long,(String,Int,List[String])], //roomId  roomName types playerIdentity
           subscribersMap:mutable.HashMap[String,ActorRef[Protocol.WsMsgSource]]
          )
          (implicit timer:TimerScheduler[Command]) =
    Behaviors.receive[Command]{
      (ctx, msg) =>
        msg match {
          case JoinRoom(playerInfo,roomInfo,userActor) =>
            //如果roomIdOpt不存在，则是创建房间，否则加入房间
            roomInfo.roomId match{
              case Some(roomId) =>
                roomInUse.get(roomId) match {
                  case Some(ls) =>
                    if(ls._3.length<2){
                      roomInUse.put(roomId,(ls._1,ls._2,playerInfo.userId::ls._3))
                      getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(playerInfo,userActor)
                    }
                  case None =>

                }
              case None =>
                var roomId = roomIdGenerator.getAndIncrement()
                while(roomInUse.exists(_._1 == roomId))roomId = roomIdGenerator.getAndIncrement()
                roomInUse.put(roomId,(roomInfo.roomName.get,roomInfo.roomType.get,List(playerInfo.userId)))
                getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(playerInfo,userActor)

            }
            Behaviors.same

          case LeftRoom(playerInfo) =>
            roomInUse.find(_._2._3.contains(playerInfo.userId)) match{
              case Some(t) =>
                roomInUse.put(t._1,(t._2._1,t._2._2,t._2._3.filterNot(_ == playerInfo.userId)))
                getRoomActor(ctx,t._1) ! UserActor.Left(playerInfo)
                if(roomInUse(t._1)._3.isEmpty) roomInUse.remove(t._1)
              case None => log.debug(s"该玩家不在任何房间")
            }
            Behaviors.same

          case msg:GetRoomList =>
            msg.replyTo ! ApiProtocol.RoomInUse(0,roomInUse.toMap)
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



}
