package com.neo.sk.breakout.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.scaladsl.Behaviors
import org.slf4j.LoggerFactory
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import scala.concurrent.duration._
import scala.collection.mutable
import com.neo.sk.breakout.breakoutServer.GameServer
import com.neo.sk.breakout.common.AppSettings
import com.neo.sk.breakout.core.UserActor.JoinRoomSuccess
import com.neo.sk.breakout.ptcl.UserProtocol.BaseUserInfo
import com.neo.sk.breakout.shared.ptcl.Game._
import com.neo.sk.breakout.shared.ptcl.GameConfig._
import com.neo.sk.breakout.shared.ptcl.Protocol
/**
  * create by zhaoyin
  * 2019/2/1  5:35 PM
  */
object RoomActor {

  val log = LoggerFactory.getLogger(this.getClass)

  val bounds = Point(Boundary.w, Boundary.h)

  val window = Point(Boundary.w, Boundary.h) //没有用到

  trait Command

  private case object SyncTimeKey

  private case object Sync extends Command

  case class JoinRoom(playerInfo: BaseUserInfo, userActor:ActorRef[UserActor.Command]) extends Command


  def create(roomId:Long):Behavior[Command] = {
    log.debug(s"RoomActor-$roomId start...")
    Behaviors.setup[Command]{ ctx =>
      Behaviors.withTimers[Command] {
        implicit timer =>
          implicit val sendBuffer = new MiddleBufferInJvm(81920)
          val subscribersMap = mutable.HashMap[String,ActorRef[UserActor.Command]]()
          val playerMap = mutable.HashMap[String,String]()
          /**每个房间都有一个自己的gird**/
          val grid = new GameServer(bounds,window)
          grid.setRoomId(roomId)

          /**后台的逻辑帧**/
          timer.startPeriodicTimer(SyncTimeKey, Sync, frameRate millis)
          idle(roomId, grid,playerMap, subscribersMap, 0l)
      }
    }
  }

  def idle(roomId: Long,
           grid: GameServer,
           playerMap:mutable.HashMap[String,String], // [PlayId,nickName]  记录房间玩家数（包括等待复活） (仅人类，包括在玩及等待复活)
           subscribersMap:mutable.HashMap[String,ActorRef[UserActor.Command]],
           tickCount:Long,
          )(
            implicit timer:TimerScheduler[Command],
            sendBuffer:MiddleBufferInJvm
          ):Behavior[Command] = {
    Behaviors.receive{ (ctx, msg) =>
      msg match {
        case JoinRoom(playerInfo,userActor) =>
          playerMap.put(playerInfo.userId, playerInfo.userName)
          subscribersMap.put(playerInfo.userId, userActor)
          grid.addPlayer(playerInfo.userId, playerInfo.userName)
          userActor ! JoinRoomSuccess(roomId, ctx.self)
          dispatchTo(subscribersMap)(playerInfo.userId, Protocol.Id(playerInfo.userId))
          //TODO 给新加入的玩家发送全部的信息

          Behaviors.same

        case UserActor.Left(playerInfo) =>
          log.info(s"RoomActor----player Left $msg")
          grid.removePlayer(playerInfo.userId)
          playerMap.remove(playerInfo.userId)
          subscribersMap.remove(playerInfo.userId)
          Behaviors.same

        case Sync =>
          grid.getSubscribersMap(subscribersMap)
          grid.update()
          //每隔40帧发送全量数据
          if(tickCount % AppSettings.SyncCount == 0){
            val gridData = grid.getAllGridData
            dispatch(subscribersMap)(gridData)
          }
          idle(roomId, grid, playerMap, subscribersMap, tickCount + 1)

        case x =>
          log.warn(s"got unknown msg: $x")
          Behaviors.unhandled
      }

    }
  }


  def dispatch(subscribers:mutable.HashMap[String,ActorRef[UserActor.Command]])(msg:Protocol.GameMessage)(implicit sendBuffer:MiddleBufferInJvm) = {
    val isKillMsg = msg.isInstanceOf[Protocol.UserDeadMessage]
    subscribers.values.foreach( _ ! UserActor.DispatchMsg(Protocol.Wrap(msg.asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result(),isKillMsg)))
  }

  def dispatchTo(subscribers:mutable.HashMap[String,ActorRef[UserActor.Command]])(id:String,msg:Protocol.GameMessage)(implicit sendBuffer:MiddleBufferInJvm) = {
    val isKillMsg = msg.isInstanceOf[Protocol.UserDeadMessage]
    subscribers.get(id).foreach( _ ! UserActor.DispatchMsg(Protocol.Wrap(msg.asInstanceOf[Protocol.GameMessage].fillMiddleBuffer(sendBuffer).result(),isKillMsg)))

  }



}
