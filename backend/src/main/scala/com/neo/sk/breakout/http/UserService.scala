package com.neo.sk.breakout.http


import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshalling
import akka.stream.scaladsl.Flow
import akka.stream.{ActorAttributes, ActorMaterializer, Materializer, Supervision}
import akka.util.{ByteString, Timeout}
import akka.actor.typed.scaladsl.AskPattern._
import org.slf4j.LoggerFactory
import io.circe.generic.auto._

import scala.concurrent.{ExecutionContextExecutor, Future}
import com.neo.sk.breakout.http.SessionBase.BreakoutSession
import com.neo.sk.breakout.ptcl.UserProtocol._
import com.neo.sk.breakout.shared.ptcl.Protocol
import com.neo.sk.breakout.common.Constant._
import com.neo.sk.breakout.core.{RoomManager, UserManager}
import com.neo.sk.breakout.Boot.{executor, roomManager, timeout, userManager}
import com.neo.sk.breakout.shared.ptcl.ApiProtocol._

/**
  * create by zhaoyin
  * 2019/2/1  5:11 PM
  */
trait UserService extends ServiceUtils with SessionBase {

  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val scheduler: Scheduler

  implicit val timeout: Timeout

  private[this] val log = LoggerFactory.getLogger(getClass)

  private def playGame = (path("playGame") & get){
    parameter(
      'playerId.as[String],
      'playerName.as[String],
      'playerType.as[Byte],
      'roomId.as[Long].?
    ){case ( playerId, playerName, playType, roomIdOpt) =>
      //TODO 1、不应该在这里才建立session的 2、根据playerType来选择玩家类型（会员or游客）
      val session = BreakoutSession(BaseUserInfo(UserRolesType.tourist, playerId, playerName), System.currentTimeMillis()).toSessionMap
      val flowFuture:Future[Flow[Message,Message,Any]]= userManager ? (UserManager.GetWebSocketFlow(Some(BaseUserInfo(UserRolesType.tourist,playerId,playerName)),roomIdOpt,_))
      dealFutureResult(
        flowFuture.map(r=>
          addSession(session) {
            handleWebSocketMessages(r)
          }
        )
      )
    }
  }


  val userRoutes: Route =
    pathPrefix("user") {
      playGame
    }
}
