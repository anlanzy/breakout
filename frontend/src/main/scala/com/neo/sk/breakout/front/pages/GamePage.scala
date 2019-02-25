package com.neo.sk.breakout.front.pages

import scala.xml.Elem
import com.neo.sk.breakout.front.common.Page
import com.neo.sk.breakout.front.core.GameHolder
import com.neo.sk.breakout.front.utils.Shortcut
/**
  * create by zhaoyin
  * 2019/1/31  4:59 PM
  */
class GamePage(playerId:String, playerName:String,playerType:Byte,roomId:Option[Long], roomName:Option[String],roomType:Option[Int]) extends Page {

  private val gameView = <canvas id ="GameView" tabindex="1"></canvas>
  private val infoView = <canvas id ="InfoView" tabindex="3"></canvas>
  private val offScreen = <canvas id="OffScreen" tabindex="4"></canvas>
  private val topView = <canvas id="TopView" tabindex="5"></canvas>


  def init()={
    val gameHolder = new GameHolder
    gameHolder.init()
    //直接建立websocket连接
    if(roomId.get == -1l){
      //创建房间
      gameHolder.joinGame(playerId,playerName,playerType,None,roomName,roomType)
    }else{
      //进入房间
      gameHolder.joinGame(playerId,playerName,playerType,roomId,roomName,roomType)
    }
  }

  override def render: Elem = {
    Shortcut.scheduleOnce(() =>init(),0)
    <div style="width:600px; height:750px; display:flex; justify-content:center;align-items:center">
      {gameView}
      {infoView}
      {offScreen}
      {topView}
    </div>
  }
}
