package com.neo.sk.breakout.core

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import UserActor
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * create by zhaoyin
  * 2019/2/1  5:34 PM
  */
object RoomManager {
  private val log=LoggerFactory.getLogger(this.getClass)

  trait Command
  case object TimeKey
  case object TimeOut extends Command

  def create(): Behavior[Command] ={
    log.debug(s"RoomManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            val roomIdGenerator = new AtomicLong(1L)
            //一开始没有可用房间
            val roomInUse = mutable.HashMap.empty
            idle(roomIdGenerator,roomInUse)
        }
    }
  }

  def idle(roomIdGenerator:AtomicLong,roomInUse:mutable.HashMap[Long,List[(String,String)]])(implicit timer:TimerScheduler[Command]) =
    Behaviors.receive[Command]{
      (ctx, msg) =>
        msg match {
          case JoinRoom(playerInfo,roomIdOpt,userActor) =>
            roomIdOpt match{
              case Some(roomId) =>
                roomInUse.get(roomId) match{
                  case Some(ls) =>
                    //TODO 考虑Bot加入时人数满上限的情况 返回加入失败消息
                    roomInUse.put(roomId,(playerInfo.playerId,playerInfo.nickname) :: ls)
                  case None =>
                    roomInUse.put(roomId,List((playerInfo.playerId,playerInfo.nickname)))
                }
                getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(playerInfo,roomId,userActor)
              case None =>
                //                val a= roomInUse.find(p => p._2.length < AppSettings.limitCount).toList.sortBy(a=>a._1)
                //                val b= roomInUse.find(p => p._2.length < AppSettings.limitCount)
                //                roomInUse.find(p => p._2.length < AppSettings.limitCount).toList.sortBy(_._1).headOption match{
                val botNum = if(AppSettings.addBotPlayer) AppSettings.botNum else 0
                roomInUse.find(p => p._2.length + botNum < AppSettings.limitCount) match{
                  case Some(t) =>
                    //                    log.info(s"RoomSize :  ${t._2.length} ======== ")
                    roomInUse.find(_._2.exists(_._1 == playerInfo.playerId)) match {
                      case Some(t) => /**此时是relive的情况**/
                      case None =>
                        roomInUse.put(t._1,(playerInfo.playerId,playerInfo.nickname) :: t._2)
                    }
                    getRoomActor(ctx,t._1) ! RoomActor.JoinRoom(playerInfo,t._1,userActor)
                  case None =>
                    var roomId = roomIdGenerator.getAndIncrement()
                    while(roomInUse.exists(_._1 == roomId))roomId = roomIdGenerator.getAndIncrement()
                    roomInUse.put(roomId,List((playerInfo.playerId,playerInfo.nickname)))
                    getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(playerInfo,roomId,userActor)
                }
            }
            log.debug(s"now roomInUse:$roomInUse")
            Behaviors.same

          case LeftRoom(playerInfo) =>
            roomInUse.find(_._2.exists(_._1 == playerInfo.playerId)) match{
              case Some(t) =>
                roomInUse.put(t._1,t._2.filterNot(_._1 == playerInfo.playerId))
                getRoomActor(ctx,t._1) ! UserActor.Left(playerInfo)
                if(roomInUse(t._1).isEmpty && t._1 > 1l)roomInUse.remove(t._1)
              case None => log.debug(s"该玩家不在任何房间")
            }
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
