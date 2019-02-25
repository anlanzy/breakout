package com.neo.sk.breakout.front.core

import org.scalajs.dom.CanvasRenderingContext2D
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html.Canvas
import com.neo.sk.breakout.shared.ptcl.Game._
import com.neo.sk.breakout.shared.ptcl.Protocol._
import com.neo.sk.breakout.shared.ptcl.GameConfig._
import scalatags.JsDom.short._

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

  private[this] val addBallImg = img(*.src := "/breakout/static/img/addBall.png").render
  private[this] val brick = img(*.src := "/breakout/static/img/brick.png").render
  private[this] val talkL = img(*.src := "/breakout/static/img/talkL.png").render
  private[this] val talkR = img(*.src := "/breakout/static/img/talkR.png").render


  this.canvas.width = bounds.x
  this.canvas.height = bounds.y

  def test() = {
    ctx.fillStyle ="#f00925"
    ctx.fillRect(0, 0, bounds.x, bounds.y)
  }

  //欢迎文字
  def drawGameWelcome: Unit = {
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.font = "32px Helvetica"
    val welcome = "Welcome"
    val welcomeWidth = ctx.measureText(welcome).width
    ctx.fillText(welcome, bounds.x/2 - welcomeWidth/2 ,bounds.y/2 - 16)
  }
  //等待文字
  def drawGameWait(myId:String) ={
    ctx.fillStyle = "#ffffff"
    ctx.font = "32px Helvetica"
    val wait = "Please wait"
    val waitWidth = ctx.measureText(wait).width
    ctx.fillText(wait, bounds.x/2 - waitWidth/2 ,bounds.y/2 - 16)
  }
  //离线提示文字
  def drawGameLost: Unit = {
    ctx.fillStyle = "#ffffff"
    ctx.font = "32px Helvetica"
    val lost = "Ops, connection lost"
    val lostWidth = ctx.measureText(lost).width
    ctx.fillText(lost, bounds.x/2 - lostWidth/2 ,bounds.y/2 - 16)
  }

  def drawImg = {
    //发送表情提示
    ctx.font = "20px Helvetica"
    ctx.fillStyle = "#000"
    val text = "按ASDF键发送表情 ：）"
    ctx.fillText(text, 20, 20)
    //绘制文字泡
    ctx.drawImage(talkL, 0, bounds.y/2-50, 300, 300)
    ctx.drawImage(talkR, bounds.x- 320, bounds.y/2-50, 300, 300)
  }

  def drawBackground: Unit = {
    /**背景色**/
    ctx.fillStyle = "#25313e"
    ctx.fillRect(brickLength, brickLength, bounds.x-brickLength*2, bounds.y-brickLength)
    /**三边的砖块**/
    for(i <-0 until 11){
      //顶部
      ctx.drawImage(brick,i*brickLength,0,brickLength,brickLength)
    }
    for(i <- 0 until 15){
      ctx.drawImage(brick,0,i*brickLength,brickLength,brickLength)
    }
    for(i <- 0 until 15){
      ctx.drawImage(brick,500 + brickLength,i*brickLength,brickLength,brickLength)
    }

  }

  def drawWhenDead() = {
    cleanCtx()
    ctx.fillStyle = "rgba(0, 0, 0, 0.3)"
    ctx.fillRect(0,0,bounds.x,bounds.y)
    ctx.font = "20px Helvetica"
    ctx.fillStyle = "#ffffff"
    val text = "Game Over! Press Space to start new game"
    val textLength = ctx.measureText(text).width
    ctx.fillText(text, bounds.x /2 - textLength/2, bounds.y/2 - 10)
  }

  def drawGrid(data:GridDataSync, offsetTime:Long, bounds:Point, window:Point) = {
    cleanCtx()
    val players = data.playerDetails
    val bricks = data.brickDetails
    val addBall = data.addBallDetails
    players.foreach{ case Player(id, name, x, color, ballList)=>
      //小球
      ctx.fillStyle = color match {
        case 1 => "#fa8b28"
        case 2 => "#608ff7"
        case 3 => "#199e28"
        case 4 => "#f32c5f"
        case 5 => "#4ff6f8"
        case 6 => "#fabf30"
        case _ => "#fa8b28"
      }
      //玩家姓名
      if(x == bounds.x * 1/3){
        //自己
        ctx.font = "20px Helvetica"
        val namefix = if(name.length > 7) name.substring(0, 6) + "*" else name
        val nameWidth = ctx.measureText(namefix).width
        ctx.fillText(namefix, x - initBallRadius - nameWidth - 30, 20)
      }else {
        //对方
        ctx.font = "20px Helvetica"
        val namefix = if(name.length > 8) name.substring(0, 7) + "*" else name
        ctx.fillText(namefix, x + initBallRadius + 30, 20)
      }
      //小球
      ballList.foreach(ball =>{
        val ballX = ball.x + ball.speedX * offsetTime.toFloat/frameRate
        val ballY = ball.y + ball.speedY * offsetTime.toFloat/frameRate
        val xfix = if(ballX > bounds.x - initBallRadius) bounds.x-initBallRadius else
        if(ballX < initBallRadius) initBallRadius else ballX
        val yfix = if(ballY < initBallRadius && !ball.onBoard)  initBallRadius else ballY
        ctx.beginPath()
        ctx.arc(xfix,yfix, initBallRadius,0,2 * Math.PI)
        ctx.fill()
      })
    }
    //绘制砖块
    bricks.groupBy(_.nums).foreach{ a=>
      ctx.fillStyle = a._1 match {
        case x if( x>=1 && x<=5) => "#01e2f6"
        case x if( x>=6 && x<=10) => "#ebc445"
        case x if( x>=11 && x<=20)=> "#f0855b"
        case x if( x>=21 && x<=30)=> "#69d17d"
        case x if( x>=31 && x<=50)=> "#ee7483"
        case _  => "#830a19"
      }
      a._2.foreach{case Brick(x, y, nums)=>
        {
          ctx.font = "15px Helvetica"
          ctx.fillRect(x - brickW/2, y - brickH/2, brickW, brickH)
          ctx.save()
          ctx.fillStyle = "#ffffff"
          ctx.fillText(nums.toString, x-5,y+5)
          ctx.restore()
        }
      }
    }
    //绘制增加小球符号
    addBall.foreach(item =>{
      ctx.drawImage(addBallImg, item.x, item.y, addBallRadius*2, addBallRadius*2)
    })
  }

  def cleanCtx() = {
    ctx.clearRect(0,0,bounds.x,bounds.y)
  }

}
