package com.neo.sk.breakout.shared

import java.awt.event.KeyEvent
import scala.math._

import scala.util.Random
import com.neo.sk.breakout.shared.ptcl.Game._
import com.neo.sk.breakout.shared.ptcl.Protocol._
import com.neo.sk.breakout.shared.ptcl.GameConfig._
import com.neo.sk.breakout.shared.util.utils._
/**
  * User: Taoz
  * Date: 9/1/2016
  * Time: 5:34 PM
  */
trait Grid {

  val boundary: Point

  val window: Point

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
  var ballMouseActionMap = Map.empty[Int, Map[String, MC]]


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

  def addBallMouseActionWithFrame(id:String, mc:MC) = {
    val map = ballMouseActionMap.getOrElse(mc.f, Map.empty)
    val tmc = map + (id -> mc)
    ballMouseActionMap += (mc.f -> tmc)
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
    checkCrash()
  }
  //更新小球的位置
  def updateBall() = {
    playerMap = playerMap.map{ player =>
      //小球从木板中弹出
      val ball = player._2.ball
      val newbeforeX = ball.x
      val newbeforeY = ball.y
      var newX = (ball.x + ball.speedX).toInt
      var newY = (ball.y + ball.speedY).toInt
      var newspeedX = ball.speedX
      var newspeedY = ball.speedY
      var newonBoard = ball.onBoard
      val mouseAct = ballMouseActionMap.getOrElse(frameCount, Map.empty[String, MC]).get(player._2.id)
      if(mouseAct.isDefined){
        val mouse = mouseAct.get
        val deg = atan2(mouse.cY - ball.y, mouse.cX - ball.x)
        newspeedX = (cos(deg) * initBallSpeed).toFloat
        newspeedY = (sin(deg) *initBallSpeed).toFloat
        newonBoard = false
      }
      /**边界碰撞检测**/
      if(newX > boundary.x - ball.radius) newX = boundary.x - ball.radius
      if(newX < ball.radius) newX = ball.radius
      if(newY < ball.radius) newY = ball.radius
      player._1 -> player._2.copy(ball = ball.copy(x = newX,y = newY,beforeX = newbeforeX,beforeY = newbeforeY, speedX = newspeedX,speedY = newspeedY,onBoard = newonBoard))
    }

  }

  private[this] def updatePlayer() = {

    def updatePlayerMap(player: Player, mouseActMap: Map[String, MP])={
      updatePlayerMove(player,mouseActMap)
    }
    val mouseAct = mouseActionMap.getOrElse(frameCount, Map.empty[String, MP])
    playerMap = playerMap.values.map(updatePlayerMap(_,mouseAct)).map(s=>(s.id,s)).toMap
  }

  private[this] def updatePlayerMove(player: Player, mouseActMap: Map[String, MP]) = {
    val mouseAct = mouseActMap.get(player.id)
    //鼠标始终在木板的中间，从而来算它的速度
    var newX = if(mouseAct.isDefined) mouseAct.get.cX else player.x
    if(newX > boundary.x - initWidth/2 ) newX = (boundary.x - initWidth/2).toShort
    if(newX < initWidth/2 ) newX = (initWidth/2).toShort
    val newSpeedX = newX - player.x
    player.copy(x = newX, speedX = newSpeedX)
  }

  def checkCrash()= {
    checkBallBrickCrash()
    checkBallBoundaryCrash()
    checkBallPlayerCrash()
  }

  //检查小球和砖块碰撞
  def checkBallBrickCrash() = {
    val newPlayerMap = playerMap.values.map{
      player =>
        val ball = player.ball
        var newspeedX = ball.speedX
        var newspeedY = ball.speedY
        var newX = ball.x
        var newY = ball.y
        val brick = brickMap.toList.sortBy(brick =>(sqrt(pow(brick._1.x - ball.x, 2.0) + pow(brick._1.y - ball.y, 2.0)))).reverse.headOption
        if(brick.isDefined){
          //TODO 碰撞算法需要修改
          /**   3
            * 1   2
            *   4
            */
          checkCollision(Point(ball.beforeX,ball.beforeY),Point(ball.x,ball.y),brick.get._1) match {
            case 1 =>
              //x方向转向，砖块消失，用户得分
              brickMap -= brick.get._1
              newspeedX = - newspeedX
              newX = brick.get._1.x - brickW/2 - initBallRadius
            case 2 =>
              //y方向转向
              brickMap -= brick.get._1
              newspeedX = - newspeedX
              newX = brick.get._1.x + brickW/2 + initBallRadius
            case 3 =>
              brickMap -= brick.get._1
              newspeedY = - newspeedY
              newY = brick.get._1.y - brickH/2 - initBallRadius
            case 4 =>
              brickMap -= brick.get._1
              newspeedY = - newspeedY
              newY = brick.get._1.y + brickH/2 + initBallRadius
            case x=>
          }
        }
        player.copy(ball = ball.copy(speedX = newspeedX, speedY = newspeedY))
    }
    playerMap = newPlayerMap.map(s=>(s.id, s)).toMap
  }

  //检查小球和边界碰撞
  def checkBallBoundaryCrash() = {
    val newPlayerMap = playerMap.values.map{
      player =>
        val ball = player.ball
        var newspeedX = ball.speedX
        var newspeedY = ball.speedY
        checkTouchBoundary(Point(ball.x,ball.y),boundary) match{
          case 1=>
            newspeedX = - newspeedX
          case 2=>
            newspeedY = - newspeedY
          case _=>
        }
        player.copy(ball = ball.copy(speedX = newspeedX, speedY = newspeedY))
    }
    playerMap = newPlayerMap.map(s=>(s.id,s)).toMap
  }

  //检查小球和木板碰撞，有动量守恒
  def checkBallPlayerCrash() = {
    val newPlayerMap = playerMap.values.map{
      player =>
        val ball = player.ball
        var newspeedY= ball.speedY
        var newspeedX = ball.speedX
        var ballY = ball.y
        checkTouchPlayer(Point(ball.x,ball.y),Point(player.x, boundary.y - initHeight/2)) match{
          case 1=>
            newspeedX = newspeedX + ball.speedX
            newspeedY = - newspeedY
            ballY = boundary.y - initHeight - initBallRadius
          case _=>
        }
        player.copy(ball = ball.copy(y = ballY, speedX = newspeedX, speedY = newspeedY))
    }
    playerMap = newPlayerMap.map(s=>(s.id,s)).toMap
  }






}
