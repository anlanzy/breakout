package com.neo.sk.breakout.front.core

import org.scalajs.dom.CanvasRenderingContext2D
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html.Canvas
import com.neo.sk.breakout.shared.ptcl.Game._
import com.neo.sk.breakout.shared.ptcl.Protocol._
import com.neo.sk.breakout.shared.ptcl.GameConfig._


/**
  * create by zhaoyin
  * 2019/1/31  5:21 PM
  */
case class DrawGame(
                     ctx:CanvasRenderingContext2D,
                     canvas:Canvas,
                     bounds:Point,
                     window:Point
                   ) {

  this.canvas.width = bounds.x
  this.canvas.height = bounds.y

  //欢迎文字
  def drawGameWelcome: Unit = {
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.font = "36px Helvetica"
    val welcome = "Welcome."
    val welcomeWidth = ctx.measureText(welcome).width
    ctx.fillText(welcome, window.x/2-welcomeWidth/2 ,window.y/2-bounds.y/2 - 50)
  }
  //等待文字
  def drawGameWait(myId:String) ={
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.font = "36px Helvetica"
    val wait = "Please wait."
    val waitWidth = ctx.measureText(wait).width
    ctx.fillText(wait, window.x/2-waitWidth/2, window.y/2-bounds.y/2 - 50)
  }
  //离线提示文字
  def drawGameLost: Unit = {
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.font = "36px Helvetica"
    val lost = "Ops, connection lost...."
    val lostWidth = ctx.measureText(lost).width
    ctx.fillText(lost, window.x/2-lostWidth/2, window.y/2-bounds.y/2 - 50)
  }

  def drawBackground: Unit = {
    /**背景色**/
    ctx.fillStyle = "#e6e7e6"
    ctx.fillRect(0, 0, bounds.x, bounds.y)
    /**顶部的砖块**/

  }

  def drawGrid(uid:String, data:GridDataSync, offsetTime:Long, bounds:Point, window:Point) = {
    drawBackground
    val players = data.playerDetails
    val bricks = data.brickDetails
    //绘制木板和小球
    players.foreach{ case Player(id, name, x, speedX, width, ball)=>
      //小球
      ctx.fillStyle = "#323232"
      val ballX = ball.x + ball.speedX*offsetTime.toFloat/frameRate
      val ballY = ball.y + ball.speedY*offsetTime.toFloat/frameRate
      val xfix = if(ballX > bounds.x - initBallRadius) bounds.x-initBallRadius else
      if(ballX < initBallRadius) initBallRadius else ballX
      val yfix = if(ballY < initBallRadius)  initBallRadius else ballY
      ctx.beginPath()
      ctx.arc(xfix,yfix, initBallRadius,0,2 * Math.PI)
      ctx.fill()
      //木板
      ctx.fillStyle = "#122772"
      val playerX = x + speedX * offsetTime.toFloat/frameRate
      val newplayerX = if(playerX > bounds.x-initWidth/2) bounds.x-initWidth/2 else
      if(playerX<  initWidth/2) initWidth/2 else playerX
      ctx.fillRect(newplayerX - initWidth/2, bounds.y-initHeight,initWidth,initHeight)
    }
    //绘制砖块
    bricks.groupBy(_.color).foreach{ a=>
      ctx.fillStyle = a._1 match {
        case 0 => "#f3456d"
        case 1 => "#f49930"
        case 2  => "#f4d95b"
        case 3  => "#2a9514"
        case 4  => "#4390d0"
        case 5  => "#bead92"
        case 6  => "#cfe6ff"
        case _  => "#de9dd6"
      }
      a._2.foreach{case Brick(x, y, color)=>
          ctx.fillRect(x - brickW/2, y - brickH/2, brickW, brickH)
      }
    }
  }

}
