package com.neo.sk.breakout.front.pages

import com.neo.sk.breakout.front.common.PageSwitcher
import mhtml._
import org.scalajs.dom
import scala.xml.Elem
/**
  * create by zhaoyin
  * 2019/1/31  4:20 PM
  */
object MainPage extends PageSwitcher{

  private val currentPage: Rx[Elem] = currentHashVar.map{
    case "playGame" :: playerId :: playerName :: roomId :: accessCode :: Nil => new GamePage(playerId, playerName, roomId.toLong, accessCode,0).render
    case x =>
      println(s"unknown hash: $x")
      <div>Error Page</div>
  }

  def show():Cancelable= {
    switchPageByHash()
    val page =
      <div>
        {currentPage}
      </div>
    mount(dom.document.body,page)
  }
}
