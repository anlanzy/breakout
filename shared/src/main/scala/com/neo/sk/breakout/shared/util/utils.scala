package com.neo.sk.breakout.shared.util

import com.neo.sk.breakout.shared.ptcl.Game._
import com.neo.sk.breakout.shared.ptcl.GameConfig._
import scala.math._

/**
  * create by zhaoyin
  * 2019/2/10  9:50 AM
  */
object utils {


  //小球砖块碰撞检测
  def checkCollision(beforeBall:Point, ball:Point, brick:Point) = {
    var direction = Point(0,0) -> 0
    if(isOverLap(beforeBall,ball,brick)){
      val distanceX = ball.x - beforeBall.x
      val distanceY = ball.y - beforeBall.y
      val deg = atan2(distanceY, distanceX)
      val distance = sqrt(pow(distanceX, 2) + pow(distanceY, 2))
      var pointMap = Map.empty[Point,Int]
      //上边交点
      val pointTX = (brick.y - brickH/2 - beforeBall.y)/sin(deg) * cos(deg) + beforeBall.x
      val pointT= Point(pointTX.toInt, brick.y - brickH/2) -> 1
      //右边交点
      val pointRY = (brick.x + brickW/2 - beforeBall.x) / cos(deg) * sin(deg) + beforeBall.y
      val pointR = Point(brick.x + brickW/2, pointRY.toInt) -> 2
      //下边交点
      val pointDX = (brick.y + brickH/2 - beforeBall.y)/sin(deg) * cos(deg) + beforeBall.x
      val pointD = Point(pointDX.toInt, brick.y + brickH/2) -> 3
      //左边交点
      val pointLY = (brick.x - brickW/2 - beforeBall.x) / cos(deg) * sin(deg) + beforeBall.y
      val pointL = Point(brick.x - brickW/2, pointLY.toInt) -> 4
      for( i <- List(pointT,pointR,pointD,pointL)){
        if(isCross(i._1,beforeBall,ball,brick)){
          pointMap += i
        }
      }
      pointMap.toList.length match {
        case 1 =>
          val i = pointMap.toList.head
          i._2 match {
            case 1 =>
              direction =  Point((i._1.x + initBallRadius/sin(deg) * cos(deg)).toInt,i._1.y-initBallRadius) -> i._2
            case 2 =>
              direction = Point(i._1.x + initBallRadius ,(i._1.y+initBallRadius/cos(deg) * sin(deg)).toInt) -> i._2
            case 3 =>
              direction =  Point((i._1.x + initBallRadius/sin(deg) * cos(deg)).toInt,i._1.y+initBallRadius) -> i._2
            case 4 =>
              direction =  Point(i._1.x - initBallRadius ,(i._1.y+initBallRadius/cos(deg) * sin(deg)).toInt) -> i._2
          }
        case 2 =>
          val i = pointMap.toList.sortBy(i=>sqrt(pow(i._1.y - beforeBall.y,2)+pow(i._1.x - beforeBall.x,2))).reverse.head
          i._2 match {
            case 1 =>
              direction =  Point((i._1.x + initBallRadius/sin(deg) * cos(deg)).toInt,i._1.y-initBallRadius) -> i._2
            case 2 =>
              direction =  Point(i._1.x + initBallRadius ,(i._1.y+initBallRadius/cos(deg) * sin(deg)).toInt) -> i._2
            case 3 =>
              direction =  Point((i._1.x + initBallRadius/sin(deg) * cos(deg)).toInt,i._1.y+initBallRadius) -> i._2
            case 4 =>
              direction =  Point(i._1.x - initBallRadius ,(i._1.y+initBallRadius/cos(deg) * sin(deg)).toInt) -> i._2
          }
        case x =>
      }
    }
    brick -> direction
  }

  def checkTouchBoundary(ball:Point, boundary: Point)={
    if(ball.x >= boundary.x - initBallRadius ||
      ball.x <= initBallRadius)
    //x方向转向
      1
    else if(ball.y <= initBallRadius)
    //x方向转向
      2
    else
      0
  }

  def checkTouchPlayer(ball:Point,player:Point,playerSpeedY:Float) ={
    if(ball.y + initBallRadius >= player.y - initHeight/2 && playerSpeedY > 0){
      //注意此时小球可能与木板平行贴过
      if(ball.x >= player.x - initWidth/2 && ball.x <= player.x + initWidth/2 ){
        //撞到木板上
        1
      }else 2 //没撞到木板,游戏结束
    }else 0
  }


  def isOverLap(beforeBall:Point,ball:Point,brick:Point)={
    var overLap = false
    val minX = if(beforeBall.x <= ball.x) beforeBall.x else ball.x
    val maxX = if(beforeBall.x > ball.x) beforeBall.x else ball.x
    val minY = if(beforeBall.y <= ball.y) beforeBall.y else ball.y
    val maxY = if(beforeBall.y > ball.y) beforeBall.y else ball.y
    if(minX>brick.x+brickW/2 || maxX < brick.x - brickW/2){

    }else if(minY > brick.y+brickH/2 || maxY < brick.y -brickH/2){

    }else {
      overLap = true
    }
    overLap
  }

  def isCross(point:Point,beforeBall:Point,ball:Point,brick:Point) = {
    var cross = false
    var minX = if(beforeBall.x < ball.x) beforeBall.x else ball.x
    var maxX = if(beforeBall.x > ball.x) beforeBall.x else ball.x
    var minY = if(beforeBall.y < ball.y) beforeBall.y else ball.y
    var maxY = if(beforeBall.y > ball.y) beforeBall.y else ball.y
    if(point.x == brick.x -brickW/2 || point.x == brick.x + brickW/2 || point.y == brick.y - brickH/2 || point.y == brick.y + brickH/2){
      if(point.x >= minX && point.x <= maxX && point.y >= minY && point.y <= maxY){
        cross = true
      }
    }
    cross
  }

}