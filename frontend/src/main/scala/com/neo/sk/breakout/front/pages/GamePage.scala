package com.neo.sk.breakout.front.pages

import scala.xml.Elem
import com.neo.sk.breakout.front.common.Page
import com.neo.sk.breakout.front.core.GameHolder
import com.neo.sk.breakout.front.utils.Shortcut
/**
  * create by zhaoyin
  * 2019/1/31  4:59 PM
  */
class GamePage(playerId:String, playerName:String,playerType:Byte) extends Page {

  private val gameView = <canvas id ="GameView"></canvas>
  private val infoView = <canvas id ="InfoView"></canvas>
  private val offScreen = <canvas id="OffScreen"></canvas>

  def init()={
    GameHolder.init()
    //直接建立websocket连接
    GameHolder.start()
  }

  override def render: Elem = {
    Shortcut.scheduleOnce(() =>init(),0)
    <div style="width:500px; height:700px">
      {gameView}
      {infoView}
      {offScreen}
    </div>
  }
}
