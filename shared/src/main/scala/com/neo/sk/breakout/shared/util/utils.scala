package com.neo.sk.breakout.shared.util

import com.neo.sk.breakout.shared.ptcl.Game._
import com.neo.sk.breakout.shared.ptcl.GameConfig._

/**
  * create by zhaoyin
  * 2019/2/10  9:50 AM
  */
object utils {


  //小球砖块碰撞检测
  def checkCollision(ball:Point, brick:Point) = {
    if(ball.x + initBallRadius > brick.x - brickW/2 ||
      ball.x - initBallRadius < brick.x + brickW/2
      ){
      //x方向转向
      1
    }else if(ball.y + initBallRadius > brick.y - brickH/2 ||
      ball.y - initBallRadius < brick.y + brickH/2){
      //y方向转向
      2
    }else 0
  }

  def checkTouchBoundary(ball:Point, boundary: Point,window: Point)={
    if(ball.x >= window.x/2 + boundary.x/2 - initBallRadius ||
      ball.x <= window.x/2 - boundary.x/2 + initBallRadius)
    //x方向转向
      1
    else if(ball.y <= window.y/2 - boundary.y/2 + initBallRadius)
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
