package com.neo.sk.breakout.common

import java.util.concurrent.TimeUnit
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import com.neo.sk.breakout.utils.SessionSupport.SessionConfig

/**
  * create by zhaoyin
  * 2019/1/31  9:52 PM
  */
object AppSettings {
  private implicit class RichConfig(config: Config) {
    val noneValue = "none"

    def getOptionalString(path: String): Option[String] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getString(path))

    def getOptionalLong(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getLong(path))

    def getOptionalDurationSeconds(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getDuration(path, TimeUnit.SECONDS))
  }

  val log = LoggerFactory.getLogger(this.getClass)
  val config = ConfigFactory.parseResources("product.conf").withFallback(ConfigFactory.load())

  val appConfig = config.getConfig("app")

  val httpInterface = appConfig.getString("http.interface")
  val httpPort = appConfig.getInt("http.port")

  val projectVersion = appConfig.getString("projectVersion")

  val sConf = config.getConfig("session")
  val sessionConfig = {
    SessionConfig(
      cookieName = sConf.getString("cookie.name"),
      serverSecret = sConf.getString("serverSecret"),
      domain = sConf.getOptionalString("cookie.domain"),
      path = sConf.getOptionalString("cookie.path"),
      secure = sConf.getBoolean("cookie.secure"),
      httpOnly = sConf.getBoolean("cookie.httpOnly"),
      maxAge = sConf.getOptionalDurationSeconds("cookie.maxAge"),
      sessionEncryptData = sConf.getBoolean("encryptData")
    )
  }
  val sessionTime=sConf.getInt("sessionTime")

  val gameConfig=config.getConfig("game")
  val limitCount=gameConfig.getInt("limitCount")
  val SyncCount = gameConfig.getInt("SyncCount")



}
