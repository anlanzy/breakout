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

  def debug(msg: String): Unit

  def info(msg: String): Unit

  val random = new Random(System.nanoTime())

  var frameCount = 0
  //玩家列表 玩家Id -> 玩家信息
  var playerMap = Map.empty[String,Player]

  //砖块列表 位置->颜色
  var brickMap = Map.empty[Point, Short]

  //操作列表  帧数->(用户ID -> 操作)
  var actionMap = Map.empty[Int, Map[String, KC]]

  //木板的鼠标事件 帧数->(用户ID -> 操作)
  var mouseActionMap = Map.empty[Int, Map[String, MP]]

  //小球的鼠标事件 帧数->(用户ID -> 操作)
  var ballMouseActionMap = Map.empty[Int, Map[String, MP]]

  var ballOnBoard = true




  //用户离开，从列表中去掉
  def removePlayer(id: String): Option[Player] = {
    val r = playerMap.get(id)
    if (r.isDefined) {
      playerMap -= id
    }
    r
  }

  def addActionWithFrame(id: String, keyCode: KC) = {
    val map = actionMap.getOrElse(keyCode.f, Map.empty)
    val tmp = map + (id -> keyCode)
    actionMap += (keyCode.f -> tmp)
  }

  def addMouseActionWithFrame(id: String, mp:MP) = {
    val map = mouseActionMap.getOrElse(mp.f, Map.empty)
    val tmp = map + (id -> mp)
    mouseActionMap += (mp.f -> tmp)
  }

  def addBallMouseActionWithFrame(id:String, mp:MP) = {
    val map = mouseActionMap.getOrElse(mp.f, Map.empty)
    val tmp = map + (id -> mp)
    ballMouseActionMap += (mp.f -> tmp)
  }

  def removeActionWithFrame(id: String, userAction: UserAction, frame: Int) = {
    userAction match {
      case k:KC=>
        val map = actionMap.getOrElse(frame,Map.empty)
        val actionQueue = map.filterNot(t => t._1 == id && k.sN == t._2.sN)
        actionMap += (frame->actionQueue)
      case m:MP=>
        val map = mouseActionMap.getOrElse(frame,Map.empty)
        val actionQueue = map.filterNot(t => t._1 == id && m.sN == t._2.sN)
        mouseActionMap += (frame->actionQueue)
    }
  }




  def update() = {
    updateSpots()
//    updatePlayer()
    actionMap -= frameCount
    mouseActionMap -= frameCount
    ballMouseActionMap -= frameCount
    frameCount += 1
  }


  private[this] def updateSpots() = {
    /**更新木板的位置**/
    updatePlayer()
    /**更新小球的位置**/
    updateBall()
    /**碰撞检测**/

  }
  //更新小球的位置
  def updateBall():Unit = {
    playerMap = playerMap.map{player =>


    }
    massList = massList.map { mass =>
      val deg = Math.atan2(mass.targetY, mass.targetX)
      val deltaY = mass.speed * Math.sin(deg)
      val deltaX = mass.speed * Math.cos(deg)

      var newSpeed = mass.speed
      var newX = mass.x
      var newY = mass.y
      newSpeed -= massSpeedDecayRate
      if (newSpeed < 0) newSpeed = 0
      if (!(deltaY).isNaN) newY = (newY + deltaY).toShort
      if (!(deltaX).isNaN) newX = (newX + deltaX).toShort

      // val borderCalc = mass.radius.ceil.toInt

      val borderCalc = 0
      if (newX > boundary.x - borderCalc) newX = (boundary.x - borderCalc).toShort
      if (newY > boundary.y - borderCalc) newY = (boundary.y - borderCalc).toShort
      if (newX < borderCalc) newX = borderCalc.toShort
      if (newY < borderCalc) newY = borderCalc.toShort

      mass.copy(x = newX, y = newY, speed = newSpeed)
    }
  }

  private[this] def updatePlayer() = {

    def updatePlayerMap(player: Player, mouseActMap: Map[String, MP])={
      updatePlayerMove(player,mouseActMap)
    }
    val mouseAct = mouseActionMap.getOrElse(frameCount, Map.empty[String, MP])
    playerMap.values.map(updatePlayerMap(_,mouseAct)).map(s=>(s.id,s)).toMap
  }
  private[this] def updatePlayerMove(player: Player, mouseActMap: Map[String, MP]) = {
    val mouseAct = mouseActMap.getOrElse(player.id,MP(None,player.targetX,0,0,0))
    var newX = player.x + player.

  }






}
