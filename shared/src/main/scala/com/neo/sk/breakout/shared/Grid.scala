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
    checkCrash()
  }
  //更新小球的位置
  def updateBall():Unit = {
    playerMap = playerMap.map{ player =>
      //小球从木板中弹出
      val mouseAct = ballMouseActionMap.getOrElse(frameCount, Map.empty[String, MP]).get(player._2.id)
      if(mouseAct.isDefined){
        //

      }
      val ball = player._2.ball
      var newX = (ball.x + ball.speedX).toInt
      var newY = (ball.y + ball.speedY).toInt
      /**边界碰撞检测**/
      if(newX > window.x/2 + boundary.x/2 - ball.radius) newX = boundary.x/2 + window.x/2 - ball.radius
      if(newX < window.x/2 - boundary.x/2 + ball.radius) newX = window.x/2 - boundary.x/2 + ball.radius
      if(newY < window.y/2 - boundary.y/2 + ball.radius) newY = window.y/2 - boundary.y/2 + ball.radius
      player._1 -> player._2.copy(ball = ball.copy(x = newX,y = newY))
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
//    val deg = atan2(player.targetY - initHeight /2, player.targetX - player.x)
//    val degX = if(cos(deg).isNaN) 0 else cos(deg)
    //鼠标始终在木板的中间
    var newX = player.x + player.speedX
    if(newX > window.x/2 + boundary.x/2 - initWidth/2 ) newX = window.x/2 + boundary.x/2 - initWidth/2
    if(newX < window.x/2 - boundary.x/2 + initWidth/2 ) newX = window.x/2 - boundary.x/2 + initWidth/2
    player.copy(x = newX.toInt, targetX = mouseAct.cX, targetY = mouseAct.cY)
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
        brickMap.foreach{
          brick =>
            checkCollision(Point(ball.x,ball.y),brick._1) match {
              case 1=>
                //x方向转向，砖块消失，用户得分
                brickMap -= brick._1
                newspeedX = - newspeedX
              case 2=>
                //y方向转向
                brickMap -= brick._1
                newspeedY = - newspeedY
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
        checkTouchBoundary(Point(ball.x,ball.y),boundary,window) match{
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

  //检查小球和木板碰撞
  def checkBallPlayerCrash() = {
    val newPlayerMap = playerMap.values.map{
      player =>
        val ball = player.ball
        var newspeedY= ball.speedY
        var ballY = ball.y
        checkTouchPlayer(Point(ball.x,ball.y),Point(player.x,window.y/2 + boundary.y/2)) match{
          case 1=>
            newspeedY = - newspeedY
            ballY = window.y/2 + boundary.y/2 + initHeight + initBallRadius
          case _=>
        }
        player.copy(ball = ball.copy(y = ballY, speedY = newspeedY))
    }
    playerMap = newPlayerMap.map(s=>(s.id,s)).toMap
  }






}
