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
  //轮次
  private var gameTurns = 1
  //等待加入的玩家
  private[this] var waitingJoin = Map.empty[String, String]
  private [this] var subscriber=mutable.HashMap[String,ActorRef[UserActor.Command]]()
  implicit val sendBuffer = new MiddleBufferInJvm(81920)




  /**产生初始砖块**/
  generateGT()

  def setRoomId(id:Long)={
    roomId = id
  }

  def addPlayer(id: String, name: String) = waitingJoin += (id -> name)

  //产生轮次： 1.每一轮的砖块 2.该轮有几个球可以打
  def generateGT() = {
    brickMap = brickMap.map(i => i.copy(_1 = i._1.copy(y = i._1.y-riseHeight))).filter(_._1.y>= brickH/2)
    //每行1-4个
    //1-5 只产生1-10之间的  5-10 只产生 1-30之间的 10+ 都产生
    //TODO 产生有点问题 砖块之间会重叠

    gameTurns match {
      case x if(x>=1 && x <= 4) =>
        val brickNums = random.nextInt(3).toShort + 1// 012 + 1
        for(i <-0 until brickNums){
          var pointx = random.nextInt(boundary.x - brickW) + brickW/2
          var pointy = random.nextInt(riseHeight - brickH) + brickH/2 + boundary.y - riseHeight
          //满足条件时进入循环
          while(checkCross(brickMap,pointx,pointy)){
            pointx = random.nextInt(boundary.x - brickW) + brickW/2
            pointy = random.nextInt(riseHeight - brickH) + brickH/2 + boundary.y - riseHeight
          }
          val num  = (random.nextInt(10) + 1).toShort
          brickMap += Point(pointx,pointy) -> num
        }
      case x if(x>=5 && x <= 8) =>
        val brickNums = random.nextInt(3).toShort + 1// 012 + 1
        for(i <-0 until brickNums){
          var pointx = random.nextInt(boundary.x - brickW) + brickW/2
          var pointy = random.nextInt(riseHeight - brickH) + brickH/2 + boundary.y - riseHeight
          while(checkCross(brickMap,pointx,pointy)){
            pointx = random.nextInt(boundary.x - brickW) + brickW/2
            pointy = random.nextInt(riseHeight - brickH) + brickH/2 + boundary.y - riseHeight
          }
          val num  = (random.nextInt(30) + 1).toShort
          brickMap += Point(pointx,pointy) -> num
        }
      case x if(x>=9) =>
        val brickNums = random.nextInt(3).toShort + 1// 012 + 1
        for(i <-0 until brickNums){
          var pointx = random.nextInt(boundary.x - brickW) + brickW/2
          var pointy = random.nextInt(riseHeight - brickH) + brickH/2 + boundary.y - riseHeight
          while(checkCross(brickMap,pointx,pointy)){
            pointx = random.nextInt(boundary.x - brickW) + brickW/2
            pointy = random.nextInt(riseHeight - brickH) + brickH/2 + boundary.y - riseHeight
          }
          val num  = (random.nextInt(50) + 1).toShort
          brickMap += Point(pointx,pointy) -> num
        }
    }
    if(!subscriber.isEmpty){
      dispatch(subscriber)(Protocol.Bricks(brickMap))
    }
    gameTurns += 1
  }

  def checkCross(brickMap: Map[Point,Short],pointX:Int,pointY:Int):Boolean = {
    var cross = false
    if(!brickMap.isEmpty){
      brickMap.foreach(i =>{
        if(math.abs(pointX-i._1.x) >= brickW || math.abs(pointY-i._1.y) >= brickH){
        }else{
          cross = true
        }
      })
    }
    cross
  }

  //生成玩家
  private[this] def genWaitingStar() = {
    waitingJoin.filterNot(kv => playerMap.contains(kv._1)).foreach{ case (id, name) =>
      var player = Player("","",0,0, ball = Ball(0,0,0,0,0,0f,0f,true))
      if(playerMap.isEmpty){
        player = Player(id,name, Boundary.w * 1/3, 1, ball = Ball(Boundary.w * 1/3, initBallRadius, Boundary.w * 1/3, initBallRadius))
      }else {
        player = Player(id,name, Boundary.w * 2/3, 2, ball = Ball(Boundary.w * 2/3, initBallRadius, Boundary.w * 2/3, initBallRadius))
      }
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

  def checkAllBall() = {
    var updateTurns = true
    playerMap.foreach(player =>
      if(player._2.ball.y < boundary.y - initBallRadius){
        updateTurns = false
      }
    )
    if(updateTurns){
      generateGT
      //让玩家的小球又产生
      playerMap = playerMap.map(i => i.copy(_2 = i._2.copy(ball = Ball(i._2.x, initBallRadius, i._2.x, initBallRadius))))
    }
  }


  override def update(): Unit = {
    super.update()
    genWaitingStar()  //新增
    checkAllBall() //检查是否开始下一轮
//    updateRanks()  //排名
  }





}
