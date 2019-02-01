package com.neo.sk.breakout.http

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.headers.CacheDirectives.{`max-age`, public}
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import scala.concurrent.ExecutionContextExecutor
import com.neo.sk.breakout.common.AppSettings



/**
  * create by zhaoyin
  * 2019/2/1  5:01 PM
  */
trait ResourceService {

  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  val log: LoggingAdapter

  private val resources = {
    pathPrefix("css") {
      extractUnmatchedPath { path =>
        getFromResourceDirectory("css")
      }
    } ~
      pathPrefix("js") {
        extractUnmatchedPath { path =>
          getFromResourceDirectory("js")
        }
      } ~
      pathPrefix("sjsout") {
        extractUnmatchedPath { path =>
          getFromResourceDirectory("sjsout")
        }
      } ~
      pathPrefix("font") {
        extractUnmatchedPath { path =>
          getFromResourceDirectory("font")
        }
      } ~
      pathPrefix("img") {
        getFromResourceDirectory("img")
      } ~
      pathPrefix("music") {
        getFromResourceDirectory("music")
      } ~ path("jsFile" / Segment / AppSettings.projectVersion) { name =>
      /*
       * fullOpt改三个地方:
       * 1、这里
       * 2、html中
       * 3、build.sbt 里面
       *
       */
      val jsFileName = name + ".js"
      //      if (jsFileName == "frontend-opt.js") {
      if (jsFileName == "frontend-fastopt.js") {
        getFromResource(s"sjsout/$jsFileName")
      } else {
        getFromResource(s"js/$jsFileName")
      }
    }

    //    ~ path("jsFile" / Segment / AppSettings.projectVersion) { name =>
    //      val jsFileName = name + ".js"
    //      if (jsFileName == "frontend-fastopt.js") {
    //        getFromResource(s"sjsout/$jsFileName")
    //      } else {
    //        getFromResource(s"js/$jsFileName")
    //      }
    //    }

  }

  //cache code copied from zhaorui.
  private val cacheSeconds = 24 * 60 * 60

  def resourceRoutes: Route = (pathPrefix("static") & get) {
    mapResponseHeaders { headers => `Cache-Control`(`public`, `max-age`(cacheSeconds)) +: headers } {
      encodeResponse(resources)
    } ~
      pathPrefix("html") {
        extractUnmatchedPath { path =>
          getFromResourceDirectory("html")
        }
      }
  }
}
