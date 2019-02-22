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

  //砖块列表 位置->次数
  var brickMap = Map.empty[Point, Short]

  //增加小球符号位置
  var addBallList = List[Point]()

  //操作列表  帧数->(用户ID -> 操作)
  var actionMap = Map.empty[Int, Map[String, KC]]

  //小球的鼠标事件 帧数->(用户ID -> 操作)
  var ballMouseActionMap = Map.empty[String, MC]

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

  def addBallMouseActionWithFrame(id:String, mc:MC) = {
    ballMouseActionMap += (id-> mc)
  }


  def update() = {
    updateSpots()
    actionMap -= frameCount
    frameCount += 1
  }


  private[this] def updateSpots() = {
    /**更新小球的位置**/
    updateBall()
    /**碰撞检测**/
    checkCrash()
  }


  //更新小球的位置
  def updateBall() = {
    val newPlayerMap = playerMap.values.map{ player =>
      //小球从上方直线下落，落到y = initBallRadius的时候开始执行鼠标事件
      val newPlayerBall = player.ball.map(ball =>{
        val newbeforeX = ball.x
        val newbeforeY = ball.y
        var newX = (ball.x + ball.speedX).toInt
        var newY = (ball.y + ball.speedY).toInt
        var newspeedX = ball.speedX
        var newspeedY = ball.speedY
        var newonBoard = ball.onBoard
        val mouseAct = ballMouseActionMap.get(player.id)
        if(mouseAct.isDefined){
          if(newY<initBallRadius){
            newspeedY = initBallSpeed
          }
          if(newY == initBallRadius && newonBoard){
            val mouse = mouseAct.get
            val deg = atan2(mouse.cY - ball.y, mouse.cX - ball.x)
            newspeedX = (cos(deg) * initBallSpeed).toFloat
            newspeedY = (sin(deg) *initBallSpeed).toFloat
            newonBoard = false
          }
        }
        /**边界碰撞检测**/
        if(newX > boundary.x - ball.radius) newX = boundary.x - ball.radius
        if(newX < ball.radius) newX = ball.radius
        if(newY < ball.radius) newY = ball.radius
        if(newbeforeY < initBallRadius && newY>=initBallRadius){
          newY = initBallRadius
          newspeedX = 0
          newspeedY = 0
        }
        ball.copy(x= newX,y = newY,beforeX = newbeforeX,beforeY = newbeforeY,speedX = newspeedX,speedY = newspeedY,onBoard=newonBoard)
      })
      player.copy(ball = newPlayerBall)
    }
    playerMap = newPlayerMap.map(s=>(s.id,s)).toMap
  }

  def checkCrash()= {
    checkBallBoundaryCrash()
    checkBallBrickCrash()
    checkBallAddBllCrash()
  }

  //检查小球和砖块碰撞
  def checkBallBrickCrash() = {
    val newPlayerMap = playerMap.values.map{
      player =>
        val newPlayerBall = player.ball.map(ball =>{
          var newspeedX = ball.speedX
          var newspeedY = ball.speedY
          var newX = ball.x
          var newY = ball.y
          var pointMap = Map.empty[Point,(Point,Int)] //砖块 ->(交点 -> 类型)（上下左右）
          val distanceX = ball.x - ball.beforeX
          val distanceY = ball.y - ball.beforeY
          val deg = atan2(distanceY, distanceX)
          brickMap.toList.foreach{
            brick =>
              var x = checkCollisionNew(Point(ball.beforeX,ball.beforeY),Point(ball.x,ball.y),brick._1)
              if(x._2._2!=0){
                pointMap += x
              }
          }
          if(!pointMap.isEmpty){
            val i = pointMap.toList.sortBy(i=>sqrt(pow(i._2._1.y - ball.beforeY,2)+pow(i._2._1.x - ball.beforeX,2))).head
            //砖块i._1 ->(交点i._2._1 -> 类型i._2._2)（上下左右）
            i._2._2 match {
              //TODO 撞到角上的处理
              case 1 =>
                //上
                if(brickMap(i._1) - 1==0){
                  brickMap -= i._1
                }else {
                  brickMap += i._1 -> (brickMap(i._1) - 1).toShort
                }
                newspeedY = - newspeedY
                newX = (i._2._1.x + initBallRadius/sin(deg) * cos(deg)).toInt
                newY = i._2._1.y - initBallRadius
              case 2 =>
                //右
                if(brickMap(i._1) - 1==0){
                  brickMap -= i._1
                }else {
                  brickMap += i._1 -> (brickMap(i._1) - 1).toShort
                }
                newspeedX = - newspeedX
                newX = i._2._1.x + initBallRadius
                newY = (i._2._1.y + initBallRadius/cos(deg) * sin(deg)).toInt
              case 3 =>
                //下
                if(brickMap(i._1) - 1==0){
                  brickMap -= i._1
                }else {
                  brickMap += i._1 -> (brickMap(i._1) - 1).toShort
                }
                newspeedY = - newspeedY
                newX = (i._2._1.x + initBallRadius/sin(deg) * cos(deg)).toInt
                newY = i._2._1.y + initBallRadius
              case 4 =>
                //左
                if(brickMap(i._1) - 1==0){
                  brickMap -= i._1
                }else {
                  brickMap += i._1 -> (brickMap(i._1) - 1).toShort
                }
                newspeedX = - newspeedX
                newX = i._2._1.x - initBallRadius
                newY = (i._2._1.y + initBallRadius/cos(deg) * sin(deg)).toInt
              case x =>
            }
          }
          ball.copy(x = newX, y=newY, speedX = newspeedX, speedY = newspeedY)
        })
        player.copy(ball = newPlayerBall)
    }
    playerMap = newPlayerMap.map(s=>(s.id, s)).toMap
  }

  //检查小球和边界碰撞
  def checkBallBoundaryCrash() = {
    val newPlayerMap = playerMap.values.map{
      player =>
        val newPlayerBall = player.ball.map(ball =>{
          var newspeedX = ball.speedX
          var newspeedY = ball.speedY
          if(!ball.onBoard){
            checkTouchBoundary(Point(ball.x,ball.y),boundary) match{
              case 1=>
                newspeedX = - newspeedX
              case 2=>
                newspeedY = - newspeedY
              case _=>
            }
          }
          ball.copy(speedX = newspeedX, speedY = newspeedY)
        })
        player.copy(ball = newPlayerBall)
    }
    playerMap = newPlayerMap.map(s=>(s.id,s)).toMap
  }

  //检查小球和增加小球符号碰撞
  def checkBallAddBllCrash()

}
