package com.neo.sk.breakout.ptcl

/**
  * create by zhaoyin
  * 2019/2/2  5:33 PM
  */
object UserProtocol {
  case class BaseUserInfo(
                           userType: String, //普通玩家、管理员
                           userId: String,
                           userName: String
                         )
}
