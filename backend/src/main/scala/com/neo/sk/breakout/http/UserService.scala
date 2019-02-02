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
import scala.concurrent.{ExecutionContextExecutor, Future}

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

  private def playGame = (path("playGame") & get & pathEndOrSingleSlash){
    parameter(
      'playerId.as[String],
      'playerName.as[String],
      'playType.as[Byte]
    ){case ( playerId, playerName , playType) =>
      val session = GypsySession(BaseUserInfo(UserRolesType.player, playerId, playerName, ""), System.currentTimeMillis()).toSessionMap
      val flowFuture:Future[Flow[Message,Message,Any]]=userManager ? (UserManager.GetWebSocketFlow(Some(PlayerInfo(playerId,playerName)),_))
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
