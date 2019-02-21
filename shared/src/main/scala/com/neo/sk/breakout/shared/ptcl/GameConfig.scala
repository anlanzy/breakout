package com.neo.sk.breakout.shared.ptcl

/**
  * create by zhaoyin
  * 2019/1/31  9:39 PM
  */
object GameConfig {

  val frameRate = 150  //ms

  //玩家小球信息
  val initBallRadius:Short  = 8
  val initBallSpeed = 40

  //砖块宽高
  val brickW = 50
  val brickH = 25

  //小球数量增加器
  val addBallRadius: Short = 20

  //每次上升的高度
  val riseHeight = 60

  val maxDelayFrame = 3


}
