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
import com.neo.sk.breakout.models.{UserInfoRepo,UserInfo}
import scala.util.{Failure, Success}
import com.neo.sk.breakout.Boot.{executor, scheduler, timeout, userManager}


/**
  * create by zhaoyin
  * 2019/2/22  4:13 PM
  */
trait AccountService extends ServiceUtils with SessionBase {

  import io.circe._
  import io.circe.generic.auto._

  private val log = LoggerFactory.getLogger(this.getClass)

  private def registerErrorRsp(msg:String) = ErrorRsp(100001,msg)

  private def register = (path("register") & post){
    entity(as[Either[Error,ApiProtocol.RegisterReq]]){
      case Right(req) =>
        dealFutureResult{
          UserInfoRepo.checkIdentity(req.idenTity).map{r=>
            if(r.isDefined){
              //该邮箱/手机号已经被注册，提示用户重新输入
              complete(registerErrorRsp("该邮箱/手机已被注册，请重新输入！"))
            }else{
              dealFutureResult{
                UserInfoRepo.insertUserInfo(UserInfo(-1,req.idenTity,req.nickName,req.passWord,0,false)).map{r=>
                  complete(SuccessRsp())
                }
              }
            }
          }.recover{
            case e:Exception =>
              complete(registerErrorRsp("查找用户信息失败"))
          }
        }
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
