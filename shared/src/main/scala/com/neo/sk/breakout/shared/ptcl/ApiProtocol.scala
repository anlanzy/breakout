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

  //登录
  case class LoginReq(
                       idenTity:String,
                       passWord:String
                     )
  case class LoginRsp(
                       errCode:Int = 0,
                       identity:String,
                       nickname:String = "ok"
                     )

  //注册
  case class RegisterReq(
                          idenTity:String,
                          nickName:String,
                          passWord:String
                        )
  case class RegisterRsp(
                          errCode:Int = 0,
                          identity:String,
                          nickname:String = "ok"
                        )

  case class TouristReq(
                       nickname:String
                       )

  case class RoomInUse(
                        errCode:Int = 0,
                        roomList: Map[Long, (String, Int, List[String])]
                      )


}
