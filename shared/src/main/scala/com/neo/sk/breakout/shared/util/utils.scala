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

    if(ball.x + initBallRadius >= brick.x - brickW/2 &&
      ball.x - initBallRadius <= brick.x + brickW/2 &&
      ball.y + initBallRadius >= brick.y - brickH/2 &&
      ball.y - initBallRadius <= brick.y + brickH/2
      ){
      val deg1 = atan2(brick.y-brickH/2-ball.y, brick.x+brickW/2-ball.x)
      val deg2 = atan2(brick.y-brickH/2-ball.y, brick.x-brickW/2-ball.x)
      val deg3 = atan2(brick.y+brickH/2-ball.y, brick.x-brickW/2-ball.x)
      val deg4 = atan2(brick.y+brickH/2-ball.y, brick.x+brickW/2-ball.x)
      val deg = atan2(beforeBall.y - ball.y, beforeBall.x - ball.x)
      deg match {
        case x if(deg > deg1 && deg < deg2) => 3
        case x if(deg > deg2 && deg < deg3) => 1
        case x if(deg >deg3 && deg < deg4) => 4
        case x if( deg>deg4 && deg < 2*math.Pi) => 2
        case x if(deg>0 && deg < deg1) => 2
      }

    }else 0
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

  def checkTouchPlayer(ball:Point,player:Point) ={
    if(ball.y + initBallRadius >= player.y - initHeight){
      if(ball.x > player.x - initWidth/2 && ball.x < player.x + initWidth/2){
        //撞到木板上
        1
      }else 2 //没撞到木板
    }else 0  // 距离木板有一段距离
  }

}
