package com.neo.sk.breakout.front.core

import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.{ErrorEvent, Event}
import java.util.concurrent.atomic.AtomicInteger

import com.neo.sk.breakout.front.breakoutClient.GameClient
import com.neo.sk.breakout.front.common.Routes.ApiRoute
import com.neo.sk.breakout.front.utils.NetDelay
import com.neo.sk.breakout.shared.ptcl.Game._
import com.neo.sk.breakout.shared.ptcl.GameConfig._
import com.neo.sk.breakout.shared.ptcl.Protocol
import com.neo.sk.breakout.shared.ptcl.Protocol._
/**
  * create by zhaoyin
  * 2019/1/31  5:13 PM
  */
class GameHolder {

  val bounds = Point(Boundary.w, Boundary.h)
  private[this] val canvas1 = dom.document.getElementById("GameView").asInstanceOf[Canvas]
  private[this] val ctx = canvas1.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val canvas2 = dom.document.getElementById("TopView").asInstanceOf[Canvas]
  private[this] val ctx2 = canvas2.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val offScreenCanvas = dom.document.getElementById("OffScreen").asInstanceOf[Canvas]
  private[this] val offCtx = offScreenCanvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  private[this] val drawGameView=DrawGame(ctx,canvas1,bounds)
  private[this] val drawOffScreen=DrawGame(offCtx,offScreenCanvas,bounds)


  /**状态值**/
  private[this] var justSynced = false
  //游戏状态
  private[this] var  gameState = GameState.play


  /**可变参数**/
  var nextInt = 0
  var nextFrame = 0
  var keyInFlame = false
  private[this] var logicFrameTime = System.currentTimeMillis()
  private[this] var syncGridData: scala.Option[GridDataSync] = None
  private[this] val actionSerialNumGenerator = new AtomicInteger(0)
  var mp = MP(None,0,0,0,0)
//  var fmp = MP(None,0,0,0,0)
  val webSocketClient = WebSocketClient(wsConnectSuccess,wsConnectError,wsMessageHandler,wsConnectClose)

  /**前端为每一个玩家有一个对应的grid**/
  val grid = new GameClient

  def getActionSerialNum=actionSerialNumGenerator.getAndIncrement()

  def init(): Unit = {
    drawGameView.drawGameWelcome
    drawOffScreen.drawBackground
//    drawGameView.drawGameOn()
//    drawMiddleView.drawRankMap()
  }

  def joinGame(playerId: String,
               playerName:String,
               playerType:Byte
              ):Unit = {
    val url = ApiRoute.getpgWebSocketUri(dom.document,playerId,playerName,playerType)
    //开启websocket
    webSocketClient.setUp(url)
    //gameloop + gamerender
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
      case GameState.play if grid.myId!= ""=>
        draw(offsetTime)
      case _ =>
    }
    nextFrame = dom.window.requestAnimationFrame(gameRender())
  }

  def draw(offsetTime:Long) = {
    if (webSocketClient.getWsState){

    }else{
      drawGameView.drawGameLost
    }
  }

  def addActionListenEvent = {
    canvas2.focus()
    canvas2.onclick = { (e: dom.MouseEvent) =>
      //球在木板上时选择方向发射
      if(grid.ballOnBoard){
        val ballStart = Protocol.BallStart(None, e.pageX.toShort, e.pageY.toShort, grid.frameCount, getActionSerialNum)
        mp = MP(None, e.pageX.toShort, e.pageY.toShort, grid.frameCount, getActionSerialNum)
        grid.addBallMouseActionWithFrame(grid.myId, mp)
        webSocketClient.sendMsg(ballStart)
        grid.ballOnBoard = false
      }
    }
    canvas2.onmousemove = { (e: dom.MouseEvent) =>
      mp = MP(None, e.pageX.toShort, e.pageY.toShort, grid.frameCount, getActionSerialNum)
      grid.addMouseActionWithFrame(grid.myId, mp)
      webSocketClient.sendMsg(mp)
    }
  }

  private def wsMessageHandler(data:GameMessage):Unit = {
    data match {
      case Protocol.Id(id) =>
        grid.myId = id

      case m:Protocol.KC =>
        if(m.id.isDefined){
          val mid = m.id.get
          if(!grid.myId.equals(mid)){
            grid.addActionWithFrame(mid,m)
          }
        }

      case msg@_ =>
        println(s"unknown $msg")

    }

  }

  private def wsConnectSuccess(e:Event) = {
    println(s"连接服务器成功")
    e
  }
  private def wsConnectError(e:ErrorEvent) = {
    val playground = dom.document.getElementById("playground")
    println("----wsConnectError")
    drawGameView.drawGameLost
    playground.insertBefore(paraGraph(s"Failed: code: ${e.colno}"), playground.firstChild)
    e
  }
  def paraGraph(msg: String) = {
    val paragraph = dom.document.createElement("p")
    paragraph.innerHTML = msg
    paragraph
  }
  private def wsConnectClose(e:Event) = {
    println("last Ws close")
    e
  }

}
