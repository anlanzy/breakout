package com.neo.sk.breakout.breakoutServer

import akka.actor.typed.ActorRef
import org.slf4j.LoggerFactory
import scala.math._
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

  //玩家有几个小球（通过撞到增加小球符号可以增加小球）
  private var playerBallNums = Map.empty[String,Int]

  /**产生初始砖块**/
  generateGT()

  def setRoomId(id:Long)={
    roomId = id
  }

  def addPlayer(id: String, name: String) = waitingJoin += (id -> name)

  def checkCross(brickMap: Map[Point,Short],pointX:Int,pointY:Int,width:Int,height:Int):Boolean = {
    var cross = false
    if(!brickMap.isEmpty){
      brickMap.foreach(i =>{
        if(math.abs(pointX-i._1.x) >= width || math.abs(pointY-i._1.y) >= height){
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
      var player = Player("","",0,0, ball = List(Ball(0,0,0,0,0,0f,0f)))
      if(playerMap.isEmpty){
        player = Player(id,name, Boundary.w * 1/3, 1, ball = List(Ball(Boundary.w * 1/3, initBallRadius, Boundary.w * 1/3, initBallRadius)))
      }else {
        player = Player(id,name, Boundary.w * 2/3, 2, ball = List(Ball(Boundary.w * 2/3, initBallRadius, Boundary.w * 2/3, initBallRadius)))
      }
      playerMap += id -> player
      playerBallNums += id -> 1
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
      brickDetails,
      addBallList
    )
  }

  def getSubscribersMap(subscribersMap:mutable.HashMap[String,ActorRef[UserActor.Command]]) ={
    subscriber=subscribersMap
  }

  def checkAllBall() = {
    var updateTurns = true
    playerMap.foreach(player =>{
      player._2.ball.foreach(ball =>
        if(ball.y < boundary.y - initBallRadius){
          updateTurns = false
        })
    })
    if(updateTurns){
      /**一轮结束，开始下一轮**/
      generateGT
      //让玩家的小球又产生
      generatePlayerBall
      //清除鼠标点击事件
      ballMouseActionMap = Map.empty[String, MC]
    }
  }

  override def update(): Unit = {
    super.update()
    genWaitingStar()  //新增
    if(!playerMap.isEmpty){
      checkAllBall() //检查是否开始下一轮
    }
//    updateRanks()  //排名
  }

  def checkBallAddBllCrash() = {
    playerMap = playerMap.map(player => {
      player._2.ball.foreach(ball => {
        addBallList.foreach(addBall => {
          if(sqrt(pow(ball.x - addBall.x, 2) + pow(ball.y - addBall.y, 2)) <= initBallRadius + addBallRadius){
            //小球撞到了增加小球符号
            addBallList = addBallList.filterNot(i=> i.x == addBall.x && i.y == addBall.y)
            playerBallNums += player._1 -> (playerBallNums(player._1) + 1)
            dispatch(subscriber)(Protocol.AddBall(addBallList))
          }
        })
      })
      player
    })
  }

  /**产生轮次： 1.每一轮的砖块和增加小球符号 2.该轮有几个球可以打**/
  def generateGT() = {
    if(brickMap.map(i => i.copy(_1 = i._1.copy(y = i._1.y-riseHeight))).filter(_._1.y<= brickH/2).isEmpty){
      brickMap = brickMap.map(i => i.copy(_1 = i._1.copy(y = i._1.y-riseHeight))).filter(_._1.y>= brickH/2)

      addBallList = addBallList.map(i => i.copy(y = i.y - riseHeight)).filter(_.y >= addBallRadius)
      //每行1-4个
      //1-5 只产生1-10之间的  5-10 只产生 1-30之间的 10+ 都产生
      gameTurns match {
        case x if(x>=1 && x <= 4) =>
          val brickNums = random.nextInt(3).toShort + 1// 012 + 1
          for(i <-0 until brickNums){
            var pointx = random.nextInt(boundary.x - brickW) + brickW/2
            var pointy = random.nextInt(riseHeight - brickH) + brickH/2 + boundary.y - riseHeight
            //满足条件时进入循环
            while(checkCross(brickMap,pointx,pointy,brickW,brickH)){
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
            while(checkCross(brickMap,pointx,pointy,brickW,brickH)){
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
            while(checkCross(brickMap,pointx,pointy,brickW,brickH)){
              pointx = random.nextInt(boundary.x - brickW) + brickW/2
              pointy = random.nextInt(riseHeight - brickH) + brickH/2 + boundary.y - riseHeight
            }
            val num  = (random.nextInt(50) + 1).toShort
            brickMap += Point(pointx,pointy) -> num
          }
      }
      val nums = random.nextInt(100).toShort
      if(nums > 80){
        var pointx = random.nextInt(boundary.x - brickW) + brickW/2
        var pointy = random.nextInt(riseHeight - brickH) + brickH/2 + boundary.y - riseHeight
        while(checkCross(brickMap,pointx,pointy,brickW/2+addBallRadius,brickH/2+addBallRadius)){
          pointx = random.nextInt(boundary.x - brickW) + brickW/2
          pointy = random.nextInt(riseHeight - brickH) + brickH/2 + boundary.y - riseHeight
        }
        addBallList =  addBallList :+ Point(pointx,pointy)
      }
      if(!subscriber.isEmpty){
        dispatch(subscriber)(Protocol.Bricks(brickMap))
        dispatch(subscriber)(Protocol.AddBall(addBallList))
      }
      gameTurns += 1
    }else{
      dispatch(subscriber)(Protocol.GameOver())
      clearAllData
    }
  }

  def generatePlayerBall = {
    playerMap = playerMap.map(player => {
      var list = List[Ball]()
      for(i <-0 until playerBallNums(player._1)){
        list = list :+ Ball(player._2.x, initBallRadius - i * addBallHeight,player._2.x, initBallRadius - i * addBallHeight)
      }
      player.copy(_2 = player._2.copy(ball = list))
    })
    dispatch(subscriber)(Protocol.PlayerMap(playerMap))
  }

  override def clearAllData: Unit ={
    super.clearAllData
    gameTurns = 1
    playerBallNums = Map.empty[String,Int]
  }

}
