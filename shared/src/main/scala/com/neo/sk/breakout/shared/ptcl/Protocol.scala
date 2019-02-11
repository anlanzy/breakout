package com.neo.sk.breakout.shared.ptcl

import Game._

/**
  * create by zhaoyin
  * 2019/1/31  5:30 PM
  */
object Protocol {
  /**
    * 后端发送的数据--------------------------------------
    * */
  trait WsMsgSource

  case object CompleteMsgServer extends WsMsgSource
  case class FailMsgServer(ex: Throwable) extends WsMsgSource

  sealed trait GameMessage extends WsMsgSource

  trait GameBeginning extends WsMsgSource

  case class ErrorWsMsgFront(msg:String) extends GameMessage
  final case object RebuildWebSocket extends GameMessage

  case class PlayerIdBytes(playerIdByteMap: Map[String, Byte]) extends GameMessage

  case class GridDataSync(
                           frameCount: Int,
                           playerDetails: List[Player],
                           brickDetails: List[Brick]
                         ) extends GameMessage



  case class Id(id: String) extends GameMessage

  case class Ranks(currentRank: List[RankInfo]) extends GameMessage

  case class MyRank(rank:RankInfo) extends GameMessage

  case class PlayerRestart(id:String) extends GameMessage

  /**玩家从playerMap中删除的两种可能：**/
  /**1、玩家死亡**/
  case class UserDeadMessage(killerName:String, deadId:String, killNum:Short, score:Short, lifeTime:Long) extends GameMessage
  /** 2、玩家离开房间**/
  case class PlayerLeft(id: Byte) extends GameMessage

  case class Wrap(ws:Array[Byte],isKillMsg:Boolean = false) extends WsMsgSource

  case class KillMessage(killerId:String, deadId:String) extends GameMessage

  case class GameOverMessage(id:String,killNum:Short,score:Short,lifeTime:Long) extends GameMessage

  case class MatchRoomError() extends GameMessage


  case class Pong(timestamp: Long)extends GameMessage

  case class VictoryMsg(id:String,name:String,score:Short,totalFrame:Int) extends GameMessage

  //  按F分裂的球发送的全量消息
  //  case class SplitPlayer(splitPlayers:Map[String,List[Cell]]) extends GameMessage

  case class PlayerJoin(id:String, player:Player) extends GameMessage //id: 邮箱

  case class JoinRoomSuccess(playerId:String, roomId:Long) extends GameMessage

  case class JoinRoomFailure(playerId:String,roomId:Long,errorCode:Int,msg:String) extends GameMessage

  /**
    * 前端发送的数据--------------------------------------
    * */
  //  sealed trait WsSendMsg{
  //        val serialNum:Int = -1 //类似每一帧的动作顺序
  //        val frame:Int = -1
  //      }
  //sN -> serialNum; f -> frame
  sealed trait WsSendMsg{
    val sN:Int = -1 //类似每一帧的动作顺序
    val f:Int = -1
  }

  case object WsSendComplete extends WsSendMsg

  case class WsSendFailed(ex:Throwable) extends WsSendMsg

  sealed trait UserAction extends WsSendMsg

  //MP -> MousePosition;  cX -> clientX;  cY -> clientY; sN -> serialNum; f -> frame
  //  case class MousePosition(id: Option[String],clientX:Short,clientY:Short, override val frame:Int, override val serialNum:Int) extends UserAction with GameMessage
  case class MP(id: Option[Byte],cX:Short,cY:Short, override val f:Int, override val sN:Int) extends UserAction with GameMessage

  //KC -> KeyCode; kC -> keyCode
  //  case class KeyCode(id: Option[String],keyCode: Int, override val frame:Int,override val serialNum:Int) extends UserAction with GameMessage
  case class KC(id: Option[String],kC: Int, override val f:Int,override val sN:Int) extends UserAction with GameMessage

  case object PressSpace extends UserAction

  case class BallStart(id:Option[Byte], cX:Short, cY:Short, override val f:Int, override val sN:Int) extends UserAction

  //复活
  case class ReLiveMsg(override val f:Int) extends UserAction with GameMessage

  //胜利后重开
  case class ReJoinMsg(override val f:Int) extends UserAction with GameMessage

  case class WatchChange(watchId: String) extends UserAction

  case class Ping(timestamp: Long) extends UserAction

  case object CreateRoom extends UserAction

  case class JoinRoom(roomId:Option[Long]) extends UserAction


}
