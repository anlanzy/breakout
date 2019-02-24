package com.neo.sk.breakout.front.pages

import java.net.URLEncoder
import org.scalajs.dom.{KeyboardEvent, html}
import scala.xml.Elem
import com.neo.sk.breakout.front.common.Page
import com.neo.sk.breakout.front.common.Routes.ApiRoute
import com.neo.sk.breakout.front.utils.Shortcut
import com.neo.sk.breakout.front.core.{GameHolder, WebSocketClient}
import com.neo.sk.breakout.shared.ptcl.Protocol.{GameMessage, Room}
import org.scalajs.dom.raw.{ErrorEvent, Event}
import com.neo.sk.breakout.shared.ptcl.Protocol
import org.scalajs.dom
import mhtml._
import scala.xml.Node
import com.neo.sk.breakout.front.core.GameHolder

/**
  * create by zhaoyin
  * 2019/2/23  12:01 PM
  */
class WorldPage(identity:String,playerName:String,playerType:Byte) extends Page{

  var roomLists:Rx[Node] = Rx(emptyHTML)
  var roomType = 0 //1:合作  2：竞争

  def init()={
    //建立websocket连接
    GameHolder.joinGame(identity,playerName,playerType)

    roomLists = GameHolder.roomInuse.map(i=>
      <div class="roomContain">
        {i.map(room=>
        <div>
        </div>
      )}
      </div>
    )
  }


  def joinWorld = {
    //判断该房间是否能开始
  }

  def createWold = {
    val roomName = dom.window.document.getElementById("roomName").asInstanceOf[html.Input].value
    if(!roomName.isEmpty&&roomType!=0){
      GameHolder.webSocketClient.sendMsg(Protocol.CreateRoom(roomName,roomType))
      //跳转到GamePage页
      dom.window.location.hash = s"#/playGame/${identity}/${playerName}/$playerType"
    }
  }

  def chooseRoomType(types:Int)={
    roomType = types
  }

  override def render: Elem = {
    Shortcut.scheduleOnce(() =>init(),0)
    <div class ="world">
      <div class="two">
        <div class="chooseWorld">
          <div style="font-size:25px">选择房间</div>
          {roomLists}
          <div class="joinButton" onclick={() => joinWorld}>加入</div>
        </div>
        <div class="createWorld">
          <div style="font-size:25px">创建房间</div>
          <div style="margin-top:70px">
            <img src="/breakout/static/img/touxiang.png" style="width:80px;height:80px;margin:0 auto;display:block"></img>
            <div style="margin:10px auto;font-size:20px;text-align:center">{playerName}</div>
            <input id="roomName" placeholder="请输入您的房间名" class="inputStyle" style="margin:15px 0;height:40px;font-size:18px"></input>
            <div class="worldTypes">
              <img src="/breakout/static/img/cooperation.png" style="width:80px;height:80px;" onclick={()=>chooseRoomType(1)}></img>
              <img src="/breakout/static/img/compete.png" style="width:80px;height:80px;" onclick={()=>chooseRoomType(2)}></img>
            </div>
          </div>
          <div class="createButton"  onclick={() => createWold}>创建</div>
        </div>
      </div>
    </div>
  }


}
