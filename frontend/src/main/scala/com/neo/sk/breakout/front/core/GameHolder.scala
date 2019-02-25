package com.neo.sk.breakout.front.core

import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.html.{Canvas, Document => _}
import org.scalajs.dom.raw._
import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.breakout.front.breakoutClient.GameClient
import com.neo.sk.breakout.front.common.Routes.ApiRoute
import com.neo.sk.breakout.front.utils.NetDelay
import com.neo.sk.breakout.shared.ptcl.Game._
import com.neo.sk.breakout.shared.ptcl.GameConfig._
import com.neo.sk.breakout.shared.ptcl.Protocol
import com.neo.sk.breakout.shared.ptcl.Protocol._
import mhtml._


/**
  * create by zhaoyin
  * 2019/1/31  5:13 PM
  */
class GameHolder {

  val bounds = Point(Boundary.w, Boundary.h)
  val offscreenBounds = Point(Boundary.w+brickLength*2,Boundary.h+brickLength*2)
  var window = Point(dom.window.innerWidth.toInt, dom.window.innerHeight.toInt)
  private[this] val gameViewCanvas = dom.document.getElementById("GameView").asInstanceOf[Canvas]
  private[this] val gameCtx = gameViewCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val infoViewCanvas = dom.document.getElementById("InfoView").asInstanceOf[Canvas]
  private[this] val infoViewCtx = infoViewCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val offScreenCanvas = dom.document.getElementById("OffScreen").asInstanceOf[Canvas]
  private[this] val offCtx = offScreenCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val drawGameView=DrawGame(gameCtx,gameViewCanvas,bounds,window)
  private[this] val drawOffScreen=DrawGame(offCtx,offScreenCanvas,offscreenBounds,window)
  private[this] val drawInfoView=DrawGame(infoViewCtx,infoViewCanvas,window,window)




  /**状态值**/
  private[this] var justSynced = false
  //游戏状态
  private[this] var  gameState = GameState.waiting

  private[this] var ballDirection = true
  var showLook = false //判断该帧内有无玩家发送表情


  /**可变参数**/
  var nextInt = 0
  var nextFrame = 0
  var keyInFlame = false
  private[this] var logicFrameTime = System.currentTimeMillis()
  private[this] var syncGridData: scala.Option[GridDataSync] = None
  private[this] val actionSerialNumGenerator = new AtomicInteger(0)
  var mp = MP(None,0,0,0,0)
  var mc = MC(None,0,0,0,0)
  //  var fmp = MP(None,0,0,0,0)
  val webSocketClient = WebSocketClient(wsConnectSuccess,wsConnectError,wsMessageHandler,wsConnectClose)

  /**前端为每一个玩家有一个对应的grid**/
  val grid = new GameClient(bounds,window)

  /**用于显示玩家表情**/
  private[this] var lookList = List.empty[(Int,String,Int)] //time,identity,lookType

  def getActionSerialNum=actionSerialNumGenerator.getAndIncrement()

  def init(): Unit = {
//    drawGameView.drawGameWelcome
    drawInfoView.drawImg
    drawOffScreen.drawBackground
//    drawGameView.drawGameOn()
//    drawMiddleView.drawRankMap()
  }

  def joinGame(playerId: String,
               playerName:String,
               playerType:Byte,
               roomId:Option[Long],
               roomName:Option[String],
               roomType:Option[Int]
              ):Unit = {
    val url = ApiRoute.getpgWebSocketUri(dom.document,playerId,playerName,playerType,roomId,roomName,roomType)
    //开启websocket
    webSocketClient.setUp(url)
    start()
    addActionListenEvent
  }

  def start(): Unit = {
    println("start---")
    /**
      * gameLoop: 150ms
      * gameRender: 约为16ms
      */
    nextInt=dom.window.setInterval(() => gameLoop, frameRate)
    dom.window.requestAnimationFrame(gameRender())
  }

  //不同步就更新，同步就设置为不同步
  def gameLoop: Unit = {
//    checkScreenSize
//    NetDelay.ping(webSocketClient)
    logicFrameTime = System.currentTimeMillis()
    if (webSocketClient.getWsState) {
      //差不多每三秒同步一次
      //不同步
      if (!justSynced) {
        keyInFlame = false
        grid.update()
      } else {
        if (syncGridData.nonEmpty) {
          //同步
          grid.setSyncGridData(syncGridData.get)
          syncGridData = None
        }
        justSynced = false
      }
    }
  }

  def gameRender(): Double => Unit = { d =>
    val curTime = System.currentTimeMillis()
    val offsetTime = curTime - logicFrameTime
    gameState match {
      case GameState.waiting =>
        drawGameView.drawGameWait(grid.myId)
      case GameState.play if grid.myId!= ""=>
        draw(offsetTime)
      case GameState.dead =>
        drawInfoView.drawWhenDead()
        //可以看到别的玩家的操作
        draw(offsetTime)
      case _ =>
    }
    nextFrame = dom.window.requestAnimationFrame(gameRender())
  }

  def draw(offsetTime:Long) = {
    if (webSocketClient.getWsState){
      val data = grid.getGridData()
      drawGameView.drawGrid(data,offsetTime,bounds,window)
      val paraBack = drawInfoView.drawLook(grid.myId,showLook, lookList)
      lookList = paraBack._1
      showLook = paraBack._2
    }else{
      drawGameView.drawGameLost
    }
  }

  def addActionListenEvent = {
    infoViewCanvas.focus()

    infoViewCanvas.onkeydown = { e: dom.KeyboardEvent => {
        if(grid.playerMap.get(grid.myId).isDefined){
                    if(e.keyCode==KeyCode.A){
                      println("aaaaaa")
                      val keyCode = Protocol.KC(None, e.keyCode, grid.frameCount, getActionSerialNum)
                      webSocketClient.sendMsg(keyCode)
                    }
                    if(e.keyCode==KeyCode.S){
                      println("sssss")
                      val keyCode = Protocol.KC(None, e.keyCode, grid.frameCount, getActionSerialNum)
                      webSocketClient.sendMsg(keyCode)
                    }
                    if(e.keyCode==KeyCode.D){
                      println("ddddd")
                      val keyCode = Protocol.KC(None, e.keyCode, grid.frameCount, getActionSerialNum)
                      webSocketClient.sendMsg(keyCode)

                    }
                    if(e.keyCode==KeyCode.F){
                      println("fffff")
                      val keyCode = Protocol.KC(None, e.keyCode, grid.frameCount, getActionSerialNum)
                      webSocketClient.sendMsg(keyCode)

                    }
        }
      }
    }

    infoViewCanvas.onclick = { (e: dom.MouseEvent) =>
      //球在木板上时选择方向发射,且此时房间里有两个人
      if(grid.playerMap.get(grid.myId).isDefined && ballDirection && grid.playerMap.toList.length == 2){
        val pageX = e.pageX - (window.x/2 - bounds.x/2)
        val pageY = e.pageY - (window.y/2 - bounds.y/2)
        mc = MC(None, pageX.toShort, pageY.toShort, grid.frameCount, getActionSerialNum)
        grid.addBallMouseActionWithFrame(grid.myId, mc)
        webSocketClient.sendMsg(mc)
        ballDirection = false
      }
    }


  }

  private def wsMessageHandler(data:GameMessage):Unit = {
    data match {
      case Protocol.Id(id) =>
        grid.myId = id
        gameState = GameState.play

      case m:Protocol.KC =>
        //按键发表情
        if(m.id.isDefined){
          if(m.kC!=KeyCode.Space){
            lookList :+=(200,m.id.get,m.kC)
            showLook = true
          }
        }

        //目的：接受其他玩家的动作
      case m:Protocol.MC =>
        if(m.id.isDefined){
          val mid = m.id.get
          if(!grid.myId.equals(mid)){
            grid.addBallMouseActionWithFrame(mid,m)
          }
        }

      case data:Protocol.GridDataSync =>
        println("获取全量数据  get ALL GRID===================")
        syncGridData = Some(data)
        justSynced = true

      //网络延迟检测
      case Protocol.Pong(createTime) =>
        NetDelay.receivePong(createTime ,webSocketClient)

      case Protocol.PlayerJoin(id, player) =>
        println(s"${player.id}  加入游戏 ${grid.frameCount} MYID:${grid.myId} ")


      case Protocol.PlayerLeft(id) =>
        if(grid.playerMap.get(id).isDefined){
          grid.removePlayer(id)
          if(id == grid.myId)
            gameClose
        }

      /**此时新的一轮开始**/
      case Protocol.Bricks(brickMap) =>
        grid.brickMap = brickMap

      case Protocol.AddBall(addBallList) =>
        grid.addBallList = addBallList

      case Protocol.PlayerMap(playerMap) =>
        grid.playerMap = playerMap
        grid.ballMouseActionMap = Map.empty[String, MC]
        ballDirection = true

      /******/
      case Protocol.GameOver() =>
        gameState = GameState.dead
        grid.clearAllData


      case Protocol.PlayerCrash(player) =>
        grid.playerMap += (player.id -> player)

      case msg@_ =>
        println(s"unknown $msg")

    }

  }

  private def wsConnectSuccess(e:Event) = {
    println(s"连接服务器成功")
    e
  }

  private def wsConnectError(e:ErrorEvent) = {
    println("----wsConnectError")
    drawGameView.drawGameLost
    e
  }

  private def wsConnectClose(e:Event) = {
    println("last Ws close")
    e
  }

  def gameClose={
    webSocketClient.closeWs
    dom.window.cancelAnimationFrame(nextFrame)
    dom.window.clearInterval(nextInt)
    //    Shortcut.stopMusic("bg")
  }

}
