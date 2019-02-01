package com.neo.sk.breakout.front.common

import org.scalajs.dom
import org.scalajs.dom.html.Document
/**
  * create by zhaoyin
  * 2019/1/31  9:33 PM
  */
object Routes {

  object ApiRoute{
    private val baseUrl = "/breakout/api"

    private def playGame(playerId:String,
                         playerName:String,
                         roomId:Long
                        ) = if(roomId == 0l)
      baseUrl + s"/playGame?playerId=$playerId&playerName=$playerName"
    else
      baseUrl + s"/playGame?playerId=$playerId&playerName=$playerName&roomId=$roomId"


    def getpgWebSocketUri(document: Document,
                          playerId:String,
                          playerName:String,
                          roomId:Long,
                          ):String = {
      val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
      val wsUrl = playGame(playerId,playerName,roomId)
      s"$wsProtocol://${dom.document.location.host}$wsUrl"
    }

  }

}
