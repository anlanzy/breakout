package com.neo.sk.breakout.front.breakoutClient

import com.neo.sk.breakout.shared.Grid
import com.neo.sk.breakout.shared.ptcl.GameConfig._
import com.neo.sk.breakout.shared.ptcl.Protocol._
import com.neo.sk.breakout.shared.ptcl._
import com.neo.sk.breakout.shared.ptcl.Game._
/**
  * create by zhaoyin
  * 2019/2/2  11:23 AM
  */
class GameClient(override val boundary: Point,override val window: Point) extends Grid{

  override def debug(msg: String): Unit = println(msg)

  override def info(msg: String): Unit = println(msg)

  var myId = ""


  def setSyncGridData(data:GridDataSync): Unit = {
    frameCount = data.frameCount
    actionMap = actionMap.filterKeys(_ > data.frameCount- maxDelayFrame)
    playerMap = data.playerDetails.map(s => s.id -> s).toMap
    brickMap = data.brickDetails.map(b => Point(b.x,b.y) -> b.nums).toMap
  }

  def getGridData() = {
    Protocol.GridDataSync(
      frameCount,
      playerMap.values.toList,
      brickMap.map(i=>Brick(i._1.x,i._1.y,i._2)).toList,
      addBallList
    )
  }

  //检查小球和木板碰撞，有动量守恒
  def checkBallPlayerCrash() = {}

  def checkBallAddBllCrash() = {}


}
