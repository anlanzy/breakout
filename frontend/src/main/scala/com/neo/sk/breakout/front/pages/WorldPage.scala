package com.neo.sk.breakout.front.pages

import java.net.URLEncoder

import scala.xml.Elem
import com.neo.sk.breakout.front.common.Page
import com.neo.sk.breakout.front.common.Routes.ApiRoute
import com.neo.sk.breakout.front.utils.Shortcut
import com.neo.sk.breakout.front.core.WebSocketClient
import com.neo.sk.breakout.shared.ptcl.Protocol.GameMessage
import org.scalajs.dom.raw.{ErrorEvent, Event}
import com.neo.sk.breakout.shared.ptcl.Protocol
import org.scalajs.dom
import mhtml._
/**
  * create by zhaoyin
  * 2019/2/23  12:01 PM
  */
class WorldPage(playerName:String,playerType:Byte) extends Page{

  val webSocketClient = WebSocketClient(wsConnectSuccess,wsConnectError,wsMessageHandler,wsConnectClose)


  def init()={
    //建立websocket连接
    val url = ApiRoute.getwpWebSocketUri(dom.document)

  }

  def joinWorld = {

  }

  def createWold = {

  }

  override def render: Elem = {
    Shortcut.scheduleOnce(() =>init(),0)
    <div class ="world">
      <div class="two">
        <div class="chooseWorld">
          <div style="font-size:25px">选择房间</div>
          <div></div>
          <div class="joinButton" onclick={() => joinWorld}>加入</div>
        </div>
        <div class="createWorld">
          <div style="font-size:25px">创建房间</div>
          <div style="margin-top:70px">
            <img src="/breakout/static/img/touxiang.png" style="width:80px;height:80px;margin:0 auto;display:block"></img>
            <div style="margin:10px auto;font-size:20px;text-align:center">{playerName}</div>
            <input placeholder="请输入您的房间名" class="inputStyle" style="margin:15px 0;height:40px;font-size:18px"></input>
            <div class="worldTypes">
              <img src="/breakout/static/img/cooperation.png" style="width:80px;height:80px;" onclick={()=>}></img>
              <img src="/breakout/static/img/compete.png" style="width:80px;height:80px;" onclick={()=>}></img>
            </div>
          </div>
          <div class="createButton"  onclick={() => createWold}>创建</div>
        </div>
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
