package com.neo.sk.breakout.http


import akka.actor.{ActorRef, ActorSystem, Scheduler}
import akka.http.javadsl.server.Route
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout
import java.net.URLEncoder
import scala.concurrent.ExecutionContextExecutor

/**
  * create by zhaoyin
  * 2019/2/1  4:56 PM
  */
trait HttpService extends ResourceService{

  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler

  val routes: server.Route =
    ignoreTrailingSlash{
      pathPrefix("breakout"){
        pathEndOrSingleSlash{
          getFromResource("html/breakout.html")
        }~ resourceRoutes ~ userRoutes
      }
    }

}
