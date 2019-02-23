package com.neo.sk.breakout.front.pages

import scala.xml.Elem
import com.neo.sk.breakout.front.common.Page
import com.neo.sk.breakout.front.common.Routes.ApiRoute
import com.neo.sk.breakout.front.utils.Shortcut
import com.neo.sk.breakout.front.core.WebSocketClient
import com.neo.sk.breakout.shared.ptcl.Protocol.GameMessage
import org.scalajs.dom.raw.{ErrorEvent, Event}
import com.neo.sk.breakout.shared.ptcl.Protocol
import org.scalajs.dom
/**
  * create by zhaoyin
  * 2019/2/23  12:01 PM
  */
class WorldPage(playerName:String,playerType:Byte) extends Page{

  val webSocketClient = WebSocketClient(wsConnectSuccess,wsConnectError,wsMessageHandler,wsConnectClose)


  def init()={
    //建立websocket连接
    val url = ApiRoute.getpgWebSocketUri(dom.document,playerId,playerName,playerType)

  }

  def joinWorld = {

  }

  def createWold = {

  }

  override def render: Elem = {
    Shortcut.scheduleOnce(() =>init(),0)
    <div style="width:100%;height:100%;background:#fafafa">
      <div class="chooseWorld">
        <div>选择房间</div>
        <div></div>
        <div onclick={() => joinWorld}>加入</div>
      </div>
      <div class="createWorld">
        <div>创建房间</div>
        <div></div>
        <div onclick={() => createWold}>创建</div>
      </div>
    </div>
  }


  private def wsMessageHandler(data:GameMessage):Unit = {
    data match {
      case Protocol.RoomInUse(roomList) =>

      case x =>

    }
  }

  private def wsConnectSuccess(e:Event) = {
    println(s"连接服务器成功")
    e
  }

  private def wsConnectError(e:ErrorEvent) = {
    println("----wsConnectError")
    e
  }

  private def wsConnectClose(e:Event) = {
    println("last Ws close")
    e
  }

}
