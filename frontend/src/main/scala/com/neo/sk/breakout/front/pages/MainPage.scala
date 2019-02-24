package com.neo.sk.breakout.front.pages

import mhtml._
import org.scalajs.dom
import scala.xml.Elem
import java.net.URLEncoder

import com.neo.sk.breakout.front.common.PageSwitcher
/**
  * create by zhaoyin
  * 2019/1/31  4:20 PM
  */
object MainPage extends PageSwitcher{

  private val currentPage: Rx[Elem] = currentHashVar.map{
    case "login" :: Nil => new LoginPage().render
    case "world" :: identity :: playerName :: playerType :: Nil => new WorldPage(identity, playerName, playerType.toByte).render
    case "playGame" :: playerId :: playerName :: playerType::roomId::roomName::roomType :: Nil => new GamePage(playerId, playerName, playerType.toByte,Some(roomId.toLong),Some(roomName),Some(roomType.toInt)).render
    case x =>
      println(s"unknown hash: $x")
      <div>Error Page</div>
  }

  def show():Cancelable= {
    switchPageByHash()
    val page =
      <div style="width:100%;height:100%;display:flex;justify-content:center;
  align-items:center;">
        {currentPage}
      </div>
    mount(dom.document.body,page)
  }

}
