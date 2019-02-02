package com.neo.sk.breakout.breakoutServer

import com.neo.sk.breakout.shared.Grid
import com.neo.sk.breakout.shared.ptcl.Game._
import org.slf4j.LoggerFactory

/**
  * create by zhaoyin
  * 2019/2/1  5:34 PM
  */
class GameServer(override val boundary: Point) extends Grid{

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  private var roomId = 0l

  def setRoomId(id:Long)={
    roomId = id
  }



}
