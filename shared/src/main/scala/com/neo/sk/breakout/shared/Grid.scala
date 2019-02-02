package com.neo.sk.breakout.shared

import java.awt.event.KeyEvent
import scala.util.Random

import com.neo.sk.breakout.shared.ptcl.Game._
import com.neo.sk.breakout.shared.ptcl.Protocol._


/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 5:34 PM
  */
trait Grid {

  val boundary: Point

  def debug(msg: String): Unit

  def info(msg: String): Unit

  val random = new Random(System.nanoTime())

  var myId = ""
  var frameCount = 0
  //玩家列表
  var playerMap = Map.empty[String,Player]
  //操作列表  帧数->(用户ID -> 操作)
  var actionMap = Map.empty[Int, Map[String, KC]]

  //用户离开，从列表中去掉
  def removePlayer(id: String): Option[Player] = {
    val r = playerMap.get(id)
    if (r.isDefined) {
      playerMap -= id
    }
    r
  }

  def addActionWithFrame(id: String, keyCode: KC) : Unit

  def removeActionWithFrame(id: String, userAction: UserAction, frame: Int): Unit


  def update() = {
    updateSpots()
    updatePlayer()
    actionMap -= frameCount
    frameCount += 1
  }


  private[this] def updateSpots() = {
  }

  private[this] def updatePlayer() = {

  }





}
