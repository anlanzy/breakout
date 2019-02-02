package com.neo.sk.breakout.front.pages

import mhtml._
import org.scalajs.dom
import scala.xml.Elem

import com.neo.sk.breakout.front.common.PageSwitcher
/**
  * create by zhaoyin
  * 2019/1/31  4:20 PM
  */
object MainPage extends PageSwitcher{

  private val currentPage: Rx[Elem] = currentHashVar.map{
    case "playGame" :: playerId :: playerName  :: playerType :: Nil => new GamePage(playerId, playerName, playerType).render
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
