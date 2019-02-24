package com.neo.sk.breakout.front.common

import org.scalajs.dom
import org.scalajs.dom.html.Document
/**
  * create by zhaoyin
  * 2019/1/31  9:33 PM
  */
object Routes {


  val base = "/breakout"

  object ApiRoute{

    private val baseUrl = base + "/user"

    private def playGame(playerId:String,
                         playerName:String,
                         playerType:Byte,
                         roomId:Option[Long],
                         roomName:Option[String],
                         roomType:Option[Int]
                        ) =
      if(roomId==None){
        baseUrl + s"/playGame?playerId=$playerId&playerName=$playerName&playerType=$playerType&roomName=${roomName.get}&roomType=${roomType.get}"
      }else{
        baseUrl + s"/playGame?playerId=$playerId&playerName=$playerName&playerType=$playerType&roomId=$roomId&roomName=${roomName.get}&roomType=${roomType.get}"

      }


    def getpgWebSocketUri(document: Document,
                          playerId:String,
                          playerName:String,
                          playerType:Byte,
                          roomId:Option[Long],
                          roomName:Option[String],
                          roomType:Option[Int]
                         ):String = {
      val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
      val wsUrl = playGame(playerId,playerName,playerType,roomId,roomName,roomType)
      s"$wsProtocol://${dom.document.location.host}$wsUrl"
    }

    def getwpWebSocketUri(document: Document
                         ):String = {
      val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
      s"$wsProtocol://${dom.document.location.host}/$baseUrl/world"
    }
    val worldList = baseUrl + "/world"

  }

  object AccountRoute{
    val adminLoginRoute = base + "/account/adminLogin"
    val registerRoute = base + "/account/register"
    val loginRoute = base + "/account/login"
    val touristRoute = base + "/account/tourist"
  }


}
