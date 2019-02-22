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
import com.neo.sk.breakout.shared.ptcl.ApiProtocol
import com.neo.sk.breakout.shared.ptcl.ApiProtocol._

import com.neo.sk.breakout.models.SlickTables._
/**
  * create by zhaoyin
  * 2019/2/22  4:13 PM
  */
trait AccountService extends ServiceUtils with SessionBase {

  import io.circe._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(this.getClass)

  private def registerErrorRsp(msg:String) = ErrorRsp(100011,msg)

  private def register = (path("register") & post){
    entity(as[Either[Error,ApiProtocol.RegisterReq]]){
      case Right(req) =>
        complete(registerErrorRsp(""))
      case Left(error) =>
        log.debug(s"用户注册失败：${error}")
        complete(registerErrorRsp(s"用户注册失败：${error}"))
    }
  }

  val accountRoutes: Route =
    pathPrefix("account") {
      register
    }


}
