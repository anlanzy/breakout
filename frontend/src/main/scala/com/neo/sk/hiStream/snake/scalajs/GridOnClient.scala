package com.neo.sk.hiStream.snake.scalajs

import com.neo.sk.hiStream.snake.{Grid, Point, Protocol}

/**
  * User: Taoz
  * Date: 9/3/2016
  * Time: 10:13 PM
  */
class GridOnClient(override val boundary: Point) extends Grid {

  override def debug(msg: String): Unit = {
    //println(msg)
  }

  override def info(msg: String): Unit = {
    //println(msg)
  }

  override def feedApple(appleCount: Int): Unit = {} //do nothing.

  var updateTime = 0l

  override def update(): Unit = {
    val current = System.currentTimeMillis()
    if(current - updateTime < Protocol.frameRate * 0.8) {
      println(s"grid update in frame=$frameCount, last update before:${current - updateTime}")
    }
    updateTime = System.currentTimeMillis()
    super.update()
  }

}
