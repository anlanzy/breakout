package com.neo.sk.breakout.core

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

import com.neo.sk.breakout.breakoutServer.GameServer
import com.neo.sk.breakout.shared.ptcl.Game._
import com.neo.sk.breakout.shared.ptcl.GameConfig._
/**
  * create by zhaoyin
  * 2019/2/1  5:35 PM
  */
object RoomActor {

  val log = LoggerFactory.getLogger(this.getClass)
  val bounds = Point(Boundary.w, Boundary.h)

  trait Command

  private case object SyncTimeKey

  private case object Sync extends Command

  def create(roomId:Long):Behavior[Command] = {
    log.debug(s"RoomActor-$roomId start...")
    Behaviors.setup[Command]{ ctx =>
      Behaviors.withTimers[Command] {
        implicit timer =>
          implicit val sendBuffer = new MiddleBufferInJvm(81920)
          /**每个房间都有一个自己的gird**/
          val grid = new GameServer(bounds)
          grid.setRoomId(roomId)

          /**后台的逻辑帧**/
          timer.startPeriodicTimer(SyncTimeKey, Sync, frameRate millis)
          idle(roomId, grid)
      }

    }
  }

  def idle(roomId: Long,
           grid: GameServer
          )(
            implicit timer:TimerScheduler[Command],
            sendBuffer:MiddleBufferInJvm
          ):Behavior[Command] = {
    Behaviors.receive{ (ctx, msg) =>
      msg match {
        case JoinRoom(playerInfo,roomId,userActor) =>

        case UserActor.Left(playerInfo) =>

        case Sync =>

        case x =>
          log.warn(s"got unknown msg: $x")
          Behaviors.unhandled
      }

    }
  }



}
