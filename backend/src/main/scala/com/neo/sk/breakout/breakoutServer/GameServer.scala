package com.neo.sk.breakout.breakoutServer

import akka.actor.typed.ActorRef
import org.slf4j.LoggerFactory
import scala.collection.mutable
import org.seekloud.byteobject.MiddleBufferInJvm

import com.neo.sk.breakout.shared.ptcl.GameConfig._
import com.neo.sk.breakout.core.RoomActor.{dispatch, dispatchTo}
import com.neo.sk.breakout.shared.Grid
import com.neo.sk.breakout.shared.ptcl.Game._
import com.neo.sk.breakout.core.UserActor
import com.neo.sk.breakout.shared.ptcl.Protocol._
import com.neo.sk.breakout.shared.ptcl.Protocol
/**
  * create by zhaoyin
  * 2019/2/1  5:34 PM
  */
class GameServer(override val boundary: Point,override val window: Point) extends Grid{

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  override def debug(msg: String): Unit = log.debug(msg)

  override def info(msg: String): Unit = log.info(msg)

  private var roomId = 0l
  //游戏关卡等级，目前有三关
  private var gameCheckPoint = 1
  //等待加入的玩家
  private[this] var waitingJoin = Map.empty[String, String]
  private [this] var subscriber=mutable.HashMap[String,ActorRef[UserActor.Command]]()
  implicit val sendBuffer = new MiddleBufferInJvm(81920)


  /**产生关卡**/
  generateGC()

  def setRoomId(id:Long)={
    roomId = id
  }

  def addPlayer(id: String, name: String) = waitingJoin += (id -> name)

  //生成游戏关卡
  def generateGC():Unit = {
    gameCheckPoint match {
      case 1 =>
        /**第一关：砖块数20，四列，每列五个**/
        for(i <- 0 until 4){
          val tmpW = (i + 1) * brickW * 2
          for(j <- 0 until 5){
            val tmpH = brickH * 2 + j * brickH
            val p = Point(tmpW, tmpH)
            brickMap += (p -> j.toShort)
          }
        }
      case 2 =>
        /**第二关：砖块数25个，四边形**/
        val numList = List(1, 3, 5, 7, 5, 3, 1)
        for(i <- numList; j <- 0 until 7){
          val tmpW = Boundary.w /2 - Math.floor(i/2) * brickW
          val tmpH = 40 + j * brickH
          val p = Point(tmpW.toInt, tmpH)
          val color = random.nextInt(2).toShort
          brickMap += (p -> color)
        }
      case x =>
        //TODO 其他关卡，待开发
    }
    gameCheckPoint += 1
  }

  //生成玩家
  private[this] def genWaitingStar() = {
    waitingJoin.filterNot(kv => playerMap.contains(kv._1)).foreach{ case (id, name) =>
      val player = Player(id,name, Boundary.w/2, ball = Ball(Boundary.w/2, Boundary.h - initHeight - initBallRadius,Boundary.w/2, Boundary.h - initHeight - initBallRadius))
      playerMap += id -> player
      dispatch(subscriber)(PlayerJoin(id,player))
      dispatchTo(subscriber)(id, getAllGridData)
    }
    waitingJoin = Map.empty[String, String]
  }


  def getAllGridData: Protocol.GridDataSync = {
    var brickDetails: List[Brick] = Nil
    brickMap.foreach(item => brickDetails ::= Brick(item._1.x, item._1.y, item._2))
    Protocol.GridDataSync(
      frameCount,
      playerMap.values.toList,
      brickDetails
    )
  }

  def getSubscribersMap(subscribersMap:mutable.HashMap[String,ActorRef[UserActor.Command]]) ={
    subscriber=subscribersMap
  }


  override def update(): Unit = {
    super.update()
    genWaitingStar()  //新增
//    updateRanks()  //排名
  }





}
