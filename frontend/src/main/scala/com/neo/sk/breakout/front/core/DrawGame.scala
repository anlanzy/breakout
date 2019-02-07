package com.neo.sk.breakout.front.core

import org.scalajs.dom.CanvasRenderingContext2D
import org.scalajs.dom.ext.Color
import org.scalajs.dom.html.Canvas

import com.neo.sk.breakout.shared.ptcl.Game._

/**
  * create by zhaoyin
  * 2019/1/31  5:21 PM
  */
case class DrawGame(
                     ctx:CanvasRenderingContext2D,
                     canvas:Canvas,
                     size:Point
                   ) {

  //欢迎文字
  def drawGameWelcome: Unit = {
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, this.canvas.width , this.canvas.height )
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.font = "36px Helvetica"
    ctx.fillText("Welcome.", 150, 180)
  }
  //离线提示文字
  def drawGameLost: Unit = {
    ctx.fillStyle = Color.White.toString()
    ctx.fillRect(0, 0, this.canvas.width , this.canvas.height )
    ctx.fillStyle = "rgba(99, 99, 99, 1)"
    ctx.font = "36px Helvetica"
    ctx.fillText("Ops, connection lost....", 350, 250)
  }

  def drawBackground: Unit = {
    /**背景色**/
    ctx.fillStyle = "rgba(34, 34, 34, 1)"
    ctx.fillRect(0, 0, this.canvas.width , this.canvas.height )
    /**顶部的砖块**/

  }

  def drawGrid() = {

  }

}