package com.neo.sk.breakout.front.pages

import scala.xml.Elem

import com.neo.sk.breakout.front.utils.Shortcut
import com.neo.sk.breakout.front.common.Page
import com.neo.sk.breakout.front.breakoutClient.GameHolder

/**
  * create by zhaoyin
  * 2019/1/31  4:59 PM
  */
class GamePage() extends Page {

  private val gameView = <canvas id ="GameView" tabindex="1"></canvas>

  def init()={
    val gameHolder = new GameHolder
    gameHolder.init()
    //直接建立websocket连接
    gameHolder.joinGame(playerId,playerName,roomId,userType)
  }

  override def render: Elem = {
    Shortcut.scheduleOnce(() =>init(),0)
    <div>
      {gameView}
    </div>
  }
}
