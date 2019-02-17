package com.neo.sk.breakout.shared.util

import com.neo.sk.breakout.shared.ptcl.Game._
import com.neo.sk.breakout.shared.ptcl.GameConfig._
import scala.math._

/**
  * create by zhaoyin
  * 2019/2/10  9:50 AM
  */
object utils {


  /**
    * 小球砖块碰撞检测算法1
    */
  def checkCollisionOld(beforeBall:Point, ball:Point, brick:Point) = {
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
  //碰撞检测算法1：投影重叠
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
    val minX = if(beforeBall.x < ball.x) beforeBall.x else ball.x
    val maxX = if(beforeBall.x > ball.x) beforeBall.x else ball.x
    val minY = if(beforeBall.y < ball.y) beforeBall.y else ball.y
    val maxY = if(beforeBall.y > ball.y) beforeBall.y else ball.y
    if(point.x == brick.x -brickW/2 || point.x == brick.x + brickW/2 || point.y == brick.y - brickH/2 || point.y == brick.y + brickH/2){
      if(point.x >= minX && point.x <= maxX && point.y >= minY && point.y <= maxY){
        cross = true
      }
    }
    cross
  }

  /**
    * 小球砖块碰撞检测算法2
    */
  def checkCollisionNew(beforeBall:Point, ball:Point, brick:Point) = {
    var pointMap = Point(0,0) -> 0
    if(linesPolygons(beforeBall,ball,brick) && isOverLap(beforeBall,ball,brick)){
      //1、小球两帧之间的运动轨迹穿过砖块
      //2、求出直线与线段的交点
      //！！！！！sortBy默认是从小到大的排序
      val point = getIntersectPoint(beforeBall,ball,brick).toList.sortBy(i=>sqrt(pow(i._1.y - beforeBall.y,2)+pow(i._1.x - beforeBall.x,2)))
      if(point.length>1){
        println(point,beforeBall,ball)
      }
      if(!point.isEmpty){
        pointMap = point.head
      }
    }
    brick -> pointMap
  }
  //碰撞检测算法2：凸多边形的顶点在一条直线的两边，则直线穿过该凸多边形
  def linesPolygons(beforeBall:Point,ball:Point,brick:Point) = {
    var cross = false
    if(ball.x == beforeBall.x && ball.y == beforeBall.y){

    }else {
      val a = if(ball.x == beforeBall.x) 1 else (ball.y - beforeBall.y) / (ball.x - beforeBall.x)
      val b = if(ball.x == beforeBall.x) - ball.x else beforeBall.y - a * beforeBall.x
      var tmpZero = 0
      var tmpOverZero = 0
      var tmpUnderZero = 0
      val pointList = List(Point(brick.x - brickW/2,brick.y - brickH/2),Point(brick.x + brickW/2,brick.y - brickH/2),
        Point(brick.x - brickW/2,brick.y + brickH/2),Point(brick.x + brickW/2,brick.y + brickH/2))
      pointList.foreach(point =>
        if(a * point.x + b - point.y > 0){
          tmpOverZero += 1
        }else if(a * point.x + b - point.y < 0){
          tmpUnderZero += 1
        }else {
          tmpZero += 1
        }
      )
      if(tmpOverZero * tmpUnderZero != 0){
        cross = true
      }
    }
    cross
  }

  //两条线段的交点
  def getIntersectPoint(beforeBall:Point, ball:Point, brick:Point) = {
    var a = 0
    var b = 0
    var pointMap = Map.empty[Point,Int]
    if(ball.y == beforeBall.y || ball.y - beforeBall.y ==0){
      b = - ball.x
      pointMap += Point(brick.x + brickW/2, b) -> 2 //右
      pointMap += Point(brick.x - brickW/2, b) -> 4 //左
    }else {
      if(ball.x == beforeBall.x || ball.x - beforeBall.x ==0) {
        a = 1
        pointMap += Point((brick.y - brickH/2 - b)/ a, brick.y - brickH/2) -> 1 //上
        pointMap += Point((brick.y + brickH/2 - b)/ a, brick.y + brickH/2) -> 3  //下
      }
      else {
        a = (ball.y - beforeBall.y) / (ball.x - beforeBall.x)
        b = beforeBall.y - a * beforeBall.x
//        println(beforeBall,ball)
        pointMap += Point((brick.y - brickH/2 - b)/ a, brick.y - brickH/2) -> 1 //上
        pointMap += Point(brick.x + brickW/2, (brick.x + brickW/2) * a + b) -> 2 //右
        pointMap += Point((brick.y + brickH/2 - b)/ a, brick.y + brickH/2) -> 3  //下
        pointMap += Point(brick.x - brickW/2, (brick.x - brickW/2) * a + b) -> 4 //左
      }
    }
    val x = pointMap.filter(i => {
      isCross2(i._1,beforeBall,ball,brick)==true
    })
    x
  }

  def isCross2(point:Point,beforeBall:Point,ball:Point,brick:Point) = {
    var cross = false
    val minX = if(beforeBall.x < ball.x) beforeBall.x else ball.x
    val maxX = if(beforeBall.x > ball.x) beforeBall.x else ball.x
    val minY = if(beforeBall.y < ball.y) beforeBall.y else ball.y
    val maxY = if(beforeBall.y > ball.y) beforeBall.y else ball.y
    if(point.x >= minX && point.x <= maxX && point.y >= minY && point.y <= maxY
      && point.x >= brick.x-brickW/2 && point.x <= brick.x + brickW/2 && point.y >= brick.y - brickH/2 && point.y <= brick.y + brickH/2){
      cross = true
    }
    cross
  }

  /**
    * 小球边界（三边）碰撞
    */
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

}