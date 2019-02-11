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
    ctx.fillStyle = "rgba(34, 34, 34, 1)"
    ctx.fillRect(window.x/2-bounds.x/2, window.y/2-bounds.y/2, bounds.x, bounds.x )
    /**顶部的砖块**/

  }

  def drawGrid(uid:String, data:GridDataSync, offsetTime:Long, bounds:Point, window:Point) = {
    val players = data.playerDetails
    val bricks = data.brickDetails
    //绘制木板和小球
    players.foreach{ case Player(id, name, x, targetX, targetY, speedX, width, ball)=>
      ctx.fillStyle = "#f3456d"
      val ballX = ball.x + ball.speedX*offsetTime.toFloat/frameRate
      val ballY = ball.y + ball.speedY*offsetTime.toFloat/frameRate
      val xfix = if(ballX > window.x/2+bounds.x/2-initBallRadius) window.x/2+bounds.x/2-initBallRadius else
      if(ballX < window.x/2-bounds.x/2+initBallRadius) window.x/2-bounds.x/2+initBallRadius else ballX
      val yfix = if(ballY< window.y/2-bounds.y/2+initBallRadius)  window.y/2-bounds.y/2+initBallRadius else ballY
      ctx.beginPath()
      ctx.arc(xfix,yfix,initBallRadius,0,2*Math.PI)
      ctx.fill()
      ctx.fillStyle = "#cfe6ff"
        val playerX = x + speedX * offsetTime.toFloat/frameRate
        val newplayerX = if(playerX > window.x/2 + bounds.x/2-initWidth/2) window.x/2 + bounds.x/2-initWidth/2 else
          if(playerX< window.x/2 - bounds.x/2 + initWidth/2) window.x/2 - bounds.x/2 + initWidth/2 else playerX
        ctx.fillRect(playerX - initWidth/2, window.y/2+bounds.y/2-initHeight,initWidth,initHeight)
    }
    //绘制砖块
    bricks.groupBy(_.color).foreach{ a=>
      ctx.fillStyle = a._1 match {
        case 0 => "#f3456d"
        case 1 => "#f49930"
        case 2  => "#f4d95b"
        case 3  => "#4cd964"
        case 4  => "#9fe0f6"
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
