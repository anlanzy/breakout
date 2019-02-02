package com.neo.sk.breakout.core

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.neo.sk.breakout.common.AppSettings
import org.slf4j.LoggerFactory

import scala.collection.mutable
import com.neo.sk.breakout.core.UserActor._
import com.neo.sk.breakout.ptcl.UserProtocol.BaseUserInfo


/**
  * create by zhaoyin
  * 2019/2/1  5:34 PM
  */
object RoomManager {
  private val log=LoggerFactory.getLogger(this.getClass)

  trait Command
  case object TimeKey
  case object TimeOut extends Command
  case class LeftRoom(playerInfo: BaseUserInfo) extends Command


  def create(): Behavior[Command] ={
    log.debug(s"RoomManager start...")
    Behaviors.setup[Command]{
      ctx =>
        Behaviors.withTimers[Command]{
          implicit timer =>
            val roomIdGenerator = new AtomicLong(1L)
            //一开始没有可用房间  roomId->(playerId,playerId)
            val roomInUse = mutable.HashMap[Long,List[String]]()
            idle(roomIdGenerator,roomInUse)
        }
    }
  }

  def idle(roomIdGenerator:AtomicLong,roomInUse:mutable.HashMap[Long,List[String]])(implicit timer:TimerScheduler[Command]) =
    Behaviors.receive[Command]{
      (ctx, msg) =>
        msg match {
          case JoinRoom(playerInfo,roomIdOpt,userActor) =>
//            var roomId = roomIdGenerator.getAndIncrement()
            if(roomIdOpt.isDefined){
              //加入房间
              roomInUse.get(roomIdOpt.get) match{
                case Some(ls) =>
                  //该房间是否满员
                  if(ls.length < AppSettings.limitCount){
                    roomInUse.put(roomIdOpt.get,playerInfo.userId :: ls)
                    getRoomActor(ctx,roomIdOpt.get) ! RoomActor.JoinRoom(playerInfo,roomIdOpt.get,userActor)
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
              getRoomActor(ctx,roomId) ! RoomActor.JoinRoom(playerInfo,roomId,userActor)
            }
            log.debug(s"now roomInUse:$roomInUse")
            Behaviors.same

          case LeftRoom(playerInfo) =>
            roomInUse.find(_._2.contains(playerInfo.userId)) match{
              case Some(t) =>
                roomInUse.put(t._1,t._2.filterNot(_ == playerInfo.userId))
                getRoomActor(ctx,t._1) ! UserActor.Left(playerInfo)
                if(roomInUse(t._1).isEmpty) roomInUse.remove(t._1)
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
