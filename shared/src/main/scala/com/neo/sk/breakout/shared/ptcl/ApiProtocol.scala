package com.neo.sk.breakout.shared.ptcl

/**
  * create by zhaoyin
  * 2019/2/2  5:32 PM
  */
object ApiProtocol {

  trait CommonRsp {
    val errCode: Int
    val msg: String
  }


  final case class ErrorRsp(
                             errCode: Int,
                             msg: String
                           ) extends CommonRsp

  final case class SuccessRsp(
                               errCode: Int = 0,
                               msg: String = "ok"
                             ) extends CommonRsp


}
