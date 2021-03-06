package com.neo.sk.hiStream.http

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorAttributes, Materializer, OverflowStrategy, Supervision}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import upickle.default._

import scala.concurrent.ExecutionContextExecutor

/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:27 PM
  */
trait HttpService extends SnakeService{


  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout


  val resourceRoutes = {


    (path("frontend-launcher.js") & get) {
      getFromResource("histream_frontend-launcher.js")
    } ~
    (path("frontend-fastopt.js") & get) {
      getFromResource("histream_frontend-fastopt.js")
    }

  }

  val snakeRoute = {
    (path("snake") & get) {
      getFromResource("web/mySnake.html")
    }
  }


  val routes =
    pathPrefix("hiStream") {
      snakeRoute ~
      netSnakeRoute ~
      resourceRoutes
    }




  def tmp = {
    val out = Source.empty
    val in = Sink.ignore
    Flow.fromSinkAndSource(in, out)
  }


  def tmp2 = {

    val sink = Sink.ignore
    def chatFlow(sender: String): Flow[String, String, Any] = {
      val in =
        Flow[String]
          .to(sink)

      // The counter-part which is a source that will create a target ActorRef per
      // materialization where the chatActor will send its messages to.
      // This source will only buffer one element and will fail if the client doesn't read
      // messages fast enough.
      val chatActor: ActorRef = null
      val out =
        Source.actorRef[String](1, OverflowStrategy.fail)
          .mapMaterializedValue(actor => chatActor ! "NewParticipant(sender, _)")

      Flow.fromSinkAndSource(in, out)
    }
  }


}
