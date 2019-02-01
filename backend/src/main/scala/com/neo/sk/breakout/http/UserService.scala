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
trait UserService {

}
