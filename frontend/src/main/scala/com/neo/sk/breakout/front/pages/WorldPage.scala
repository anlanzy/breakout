package com.neo.sk.breakout.front.pages

import java.net.URLEncoder
import org.scalajs.dom.{KeyboardEvent, html}
import scala.xml.Elem
import com.neo.sk.breakout.front.common.Page
import com.neo.sk.breakout.front.common.Routes.ApiRoute
import com.neo.sk.breakout.front.utils.Shortcut
import com.neo.sk.breakout.front.core.{GameHolder, WebSocketClient}
import org.scalajs.dom.raw.{ErrorEvent, Event}
import com.neo.sk.breakout.shared.ptcl.ApiProtocol
import com.neo.sk.breakout.shared.ptcl.Protocol
import org.scalajs.dom
import mhtml._
import scala.xml.Node
import com.neo.sk.breakout.front.utils.{Http, JsFunc}
import com.neo.sk.breakout.front.common.Routes._
import io.circe.generic.auto._
import io.circe._
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * create by zhaoyin
  * 2019/2/23  12:01 PM
  */
class WorldPage(identity:String,playerName:String,playerType:Byte) extends Page {

  var roomInuse =  Var(List.empty[(Long,(String,Int,List[String]))])

  var chooseRoomId = -1l
  var chooseRoomIdVar = Var(-1l)
  var roomPlayerNum = 1
  var roomType = 0 //1:合作  2：竞争
  var roomTypeVar = Var(0)
  var roomName = None

  var roomLists:Rx[Node] = roomInuse.map(roomList=>
    {
      def check(room:(Long,(String,Int,List[String]))) = {
        <div>{chooseRoomIdVar.map(id => if(id==room._1){
          <div class="roomStyleC" onclick={()=> clickRoom(room)}>
            <div class="roomName">{room._2._1}</div>
            {roomTypePic(room._2._2)}
            <div style="color:#858585;line-height: 30px;">{room._2._3.length}/2</div>
          </div>
        }else{
          <div class="roomStyle" onclick={()=> clickRoom(room)}>
            <div class="roomName">{room._2._1}</div>
            {roomTypePic(room._2._2)}
            <div style="color:#858585;line-height: 30px;">{room._2._3.length}/2</div>
          </div>
        })}</div>

      }

      <div class="roomContain">
        {roomList.map(check(_))}
      </div>
    }

  )

  def clickRoom(room:(Long,(String,Int,List[String]))) ={
    chooseRoomId = room._1
    roomPlayerNum = room._2._3.length
    chooseRoomIdVar := room._1
  }
  def roomTypePic(types:Int) = {
    if(types==1){
      <img src="/breakout/static/img/cooperation.png"></img>
    }else{
      <img src="/breakout/static/img/compete.png"></img>
    }
  }
  val cooperation = roomTypeVar.map(i=>if(i==1){
    <img src="/breakout/static/img/cooperationC.png" style="width:60px;height:60px;" onclick={()=>chooseRoomType(1)}></img>
  }else {
    <img src="/breakout/static/img/cooperation.png" style="width:60px;height:60px;" onclick={()=>chooseRoomType(1)}></img>
  }
  )
  val compete = roomTypeVar.map(i=> if(i==2){
    <img src="/breakout/static/img/competeC.png" style="width:60px;height:60px;" onclick={()=>chooseRoomType(2)}></img>
  }else{
    <img src="/breakout/static/img/compete.png" style="width:60px;height:60px;" onclick={()=>chooseRoomType(2)}></img>
  })

  def init():Unit={
    //获取当前房间列表
    Http.getAndParse[ApiProtocol.RoomInUse](ApiRoute.worldList).map{rsp=>
      if(rsp.errCode==0){
        roomInuse := rsp.roomList.toList
      }
    }
  }


  def joinWorld = {
    //判断该房间是否满员
    if(roomPlayerNum == 1 && chooseRoomId != -1l){
      dom.window.location.hash = s"#/playGame/${identity}/${playerName}/$playerType/$chooseRoomId/0/0"
    }
  }

  def createWold = {
    val roomName = dom.window.document.getElementById("roomName").asInstanceOf[html.Input].value
    if(roomName.length >0 && roomType!=0){
      dom.window.location.hash = s"#/playGame/${identity}/${playerName}/$playerType/-1/$roomName/$roomType"
    }
    }


  def chooseRoomType(types:Int)={
    roomType = types
    roomTypeVar := types
  }

  override def render: Elem = {
    Shortcut.scheduleOnce(() =>init(),0)
    <div class ="world">
      <div class="two">
        <div class="chooseWorld">
          <div style="display:flex;justify-content:space-between;padding-right:80px">
            <div style="font-size:25px">选择房间</div>
            <img onclick={()=>init()} src="/breakout/static/img/refresh.png" class="refresh"></img>
          </div>
          {roomLists}
          <div class="joinButton" onclick={() => joinWorld}>加入</div>
        </div>
        <div class="createWorld">
          <div style="font-size:25px">创建房间</div>
          <div style="margin-top:40px;height:70%">
            <img src="/breakout/static/img/touxiang.png" style="width:80px;height:80px;margin:0 auto;display:block"></img>
            <div style="margin:10px auto;font-size:20px;text-align:center">{playerName}</div>
            <input id="roomName" placeholder="请输入您的房间名" class="inputStyle" style="margin:15px 0;height:40px;font-size:18px"></input>
            <div>选择房间类型（合作/竞争）：</div>
            <div class="worldTypes">
              {cooperation}
              {compete}
            </div>
          </div>
          <div class="createButton"  onclick={() => createWold}>创建</div>
        </div>
      </div>
    </div>
  }


}
