package com.neo.sk.breakout.front.breakoutClient

import com.neo.sk.breakout.ptcl.Protocol
import org.scalajs.dom.raw._
import org.seekloud.byteobject.ByteObject.bytesDecode
import org.seekloud.byteobject.MiddleBufferInJs

import scala.scalajs.js.typedarray.ArrayBuffer

/**
  * create by zhaoyin
  * 2019/1/31  5:26 PM
  */
case class WebSocketClient(
                            connectSuccessCallback: Event => Unit,
                            connectErrorCallback:ErrorEvent => Unit,
                            messageHandler:Protocol.GameMessage => Unit,
                            closeCallback:Event => Unit
                          ) {

  private var wsSetup=false

  private var webSocketOpt:Option[WebSocket]=None

  def getWsState=wsSetup

  private val sendBuffer: MiddleBufferInJs = new MiddleBufferInJs(8192)

  def sendMsg(msg:Protocol.UserAction) = {
    import org.seekloud.byteobject.ByteObject._
    webSocketOpt.get.send(msg.fillMiddleBuffer(sendBuffer).result())
  }

  def setUp(url:String)=
    if(wsSetup){
      println("-----error")
    }else{
      val gameStream = new WebSocket(url)
      webSocketOpt = Some(gameStream)
      webSocketOpt.get.onopen = { event: Event =>
        wsSetup = true
        connectSuccessCallback(event)
      }
      webSocketOpt.get.onerror = { event: ErrorEvent =>
        wsSetup = false
        webSocketOpt = None
        connectErrorCallback(event)
      }

      webSocketOpt.get.onmessage = { event: MessageEvent =>
        event.data match {
          case blobMsg:Blob =>
            val fr = new FileReader()
            fr.readAsArrayBuffer(blobMsg)
            fr.onloadend = { _: Event =>
              val buf = fr.result.asInstanceOf[ArrayBuffer]
              val middleDataInJs = new MiddleBufferInJs(buf)
              val data = bytesDecode[Protocol.GameMessage](middleDataInJs) match {
                case Right(msg) => msg
                case Left(e) => println(s"Error####:${e.message}")
              }
              messageHandler(data)
            }
          //          case jsonStringMsg:String =>
          //            import io.circe.generic.auto._
          //            import io.circe.parser._
          //            val data = decode[Protocol.GameMessage](jsonStringMsg).right.get
          //            messageHandler(data)

          case unknow =>  println(s"recv unknow msg:${unknow}")
        }

      }
      webSocketOpt.get.onclose = { event: Event =>
        wsSetup = false
        webSocketOpt=None
        closeCallback(event)
      }
    }

  def closeWs={
    wsSetup = false
    println("---close Ws active")
    webSocketOpt.foreach(_.close())
    webSocketOpt = None
  }

}
