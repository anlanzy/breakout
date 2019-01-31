package com.neo.sk.breakout.front

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

import com.neo.sk.breakout.front.pages.MainPage
/**
  * create by zhaoyin
  * 2019/1/31  3:51 PM
  */
object Main {

  def main(args: Array[String]): Unit ={
    run()
  }

  @JSExport
  def run(): Unit = {
    MainPage.show()
  }

}
