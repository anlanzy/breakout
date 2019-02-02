package com.neo.sk.breakout

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.actor.typed.scaladsl.adapter._

import com.neo.sk.breakout.http.HttpService
import com.neo.sk.breakout.core.{RoomManager, UserManager}
/**
  * create by zhaoyin
  * 2019/1/31  9:48 PM
  */
object Boot extends HttpService{

  import concurrent.duration._
  import com.neo.sk.breakout.common.AppSettings._

  override implicit val system = ActorSystem("breakout", config)
  // the executor should not be the default dispatcher.
  override implicit val executor = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  override implicit val materializer = ActorMaterializer()

  override implicit val timeout = Timeout(20 seconds) // for actor asks

  override implicit val scheduler = system.scheduler

  val log: LoggingAdapter = Logging(system, getClass)

  val roomManager: ActorRef[RoomManager.Command] =system.spawn(RoomManager.create(),"roomManager")

  val userManager:ActorRef[UserManager.Command] = system.spawn(UserManager.create(),"userManager")

  def main(args: Array[String]) {
    log.info("Starting.")
    Http().bindAndHandle(routes, httpInterface, httpPort)
    log.info(s"Listen to the $httpInterface:$httpPort")
    log.info("Done.")
    println(s"Server is listening on http://localhost:${httpPort}/breakout")
  }

}
