package com.neo.sk.breakout.models

import scala.concurrent.Future

/**
  * create by zhaoyin
  * 2019/2/22  10:24 PM
  */
case class UserInfo(id:Long, identity:String, nickname:String, password:String, best_score:Long, is_forbidden:Boolean)

trait UserInfoTable {

  import com.neo.sk.breakout.utils.DBUtil.driver.api._

  class UserInfoTable(tag: Tag) extends Table[UserInfo](tag, "USER_INFO") {
    val id = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    val identity = column[String]("IDENTITY")
    val nickname = column[String]("NICKNAME")
    val password = column[String]("PASSWORD")
    val best_score = column[Long]("BEST_SCORE")
    val is_forbidden = column[Boolean]("IS_FORBIDDEN")

    def * = (id, identity, nickname, password, best_score, is_forbidden) <> (UserInfo.tupled, UserInfo.unapply)
  }

  protected val UserInfoTableQuery = TableQuery[UserInfoTable]
}

object UserInfoRepo extends UserInfoTable {

  import com.neo.sk.breakout.utils.DBUtil.driver.api._
  import com.neo.sk.breakout.utils.DBUtil.db

  def insertUserInfo(userInfo:UserInfo) = {
    db.run(UserInfoTableQuery.insertOrUpdate(userInfo))
  }

  def checkIdentity(identity: String) = {
    db.run(UserInfoTableQuery.filter(u => u.identity === identity).result.headOption)
  }

  def userLogin(identity: String,password:String) = {
    db.run(UserInfoTableQuery.filter(u => u.identity===identity && u.password===password).result.headOption)
  }

}