package com.neo.sk.breakout.front.pages

import com.neo.sk.breakout.front.common.Page
import com.neo.sk.breakout.front.utils.Shortcut
import mhtml._
import scala.xml.Elem
import org.scalajs.dom
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.{KeyboardEvent, html}
import org.scalajs.dom.html.Div
import com.neo.sk.breakout.shared.ptcl.ApiProtocol._
import scala.concurrent.ExecutionContext.Implicits.global
import com.neo.sk.breakout.front.utils.{Http, JsFunc}
import io.circe.generic.auto._
import io.circe._
import io.circe.syntax._
import com.neo.sk.breakout.front.common.Routes._
import java.net.URLEncoder

/**
  * create by zhaoyin
  * 2019/2/22  10:10 AM
  */
class LoginPage extends Page{

  private val bodyName = Var("登录")
  private val nickName = Var(emptyHTML)
  /**1:登录 2：注册 3: 游客登录 4：管理员登录**/
  private var types = 1
  private var anotherLogin = Var(emptyHTML) //游客登录
  private var passwordBody = Var(emptyHTML) //输入密码
  private var loginType = Var("游客登录")
  private var identityBody = Var(emptyHTML) //输入邮箱/密码

  //init
  init()

  private def init() = {
    anotherLogin := <div class="anotherLogin" onclick={()=>changeLogin() }>{loginType}</div>
    passwordBody :=
      <div style="margin-bottom:20px">
        <div>密码</div>
        <input class="inputStyle" id="password"></input>
      </div>
    identityBody :=
      <div style="margin-bottom:20px">
        <div>手机号/邮箱</div>
        <input class="inputStyle" placeholder="例如zhaoyin@ebupt.com" id="identity"></input>
      </div>
  }

  private def loginBody() = {
    identityBody :=
      <div style="margin-bottom:20px">
        <div>手机号/邮箱</div>
        <input class="inputStyle" placeholder="例如zhaoyin@ebupt.com" id="identity"></input>
      </div>
    bodyName := "登录"
    nickName := emptyHTML
    types = 1
    loginType := "游客登录"
    anotherLogin := <div class="anotherLogin" onclick={() =>changeLogin()}>{loginType}</div>
    passwordBody :=
      <div style="margin-bottom:20px">
        <div>密码</div>
        <input class="inputStyle" id="password"></input>
      </div>
  }

  private def registerBody() = {
    identityBody :=
      <div style="margin-bottom:20px">
        <div>手机号/邮箱</div>
        <input class="inputStyle" placeholder="例如zhaoyin@ebupt.com" id="identity"></input>
      </div>
    anotherLogin := emptyHTML
    bodyName := "注册"
    types = 2
    nickName :=
      <div style="margin-bottom:20px">
        <div>昵称</div>
        <input class="inputStyle" id="nickname"></input>
      </div>
  }

  private def enter():Unit = {
    val identity = dom.window.document.getElementById("identity").asInstanceOf[html.Input].value
    val password = dom.window.document.getElementById("password").asInstanceOf[html.Input].value
    types match {
      case 1 =>
        //登录
        val data = LoginReq(identity,password).asJson.noSpaces
        Http.postJsonAndParse[LoginRsp](AccountRoute.loginRoute,data).map{rsp =>
          if(rsp.errCode==0){
            //跳转进入创建房间or选择房间页
            dom.window.location.hash = s"#/world/${rsp.identity}/${rsp.nickname}/$types"
          }
        }
      case 2 =>
        //注册
        val nickname = dom.window.document.getElementById("nickname").asInstanceOf[html.Input].value
        val data = RegisterReq(identity,nickname,password).asJson.noSpaces
        Http.postJsonAndParse[RegisterRsp](AccountRoute.registerRoute, data).map{rsp =>
          if(rsp.errCode==0){
            //跳转进入创建房间or选择房间页
            dom.window.location.hash = s"#/world/${rsp.identity}/${rsp.nickname}/$types"
          }
        }
      case 3 =>
        //游客
        val nickname = dom.window.document.getElementById("nickname").asInstanceOf[html.Input].value
        val data = TouristReq(nickname).asJson.noSpaces
        Http.postJsonAndParse[LoginRsp](AccountRoute.touristRoute,data).map{rsp =>
          if(rsp.errCode==0){
            dom.window.location.hash = s"#/world/${rsp.identity}/${rsp.nickname}/$types"
          }
        }
      case 4 =>
        val nickname = dom.window.document.getElementById("nickname").asInstanceOf[html.Input].value
        val data = RegisterReq(identity,nickname,password).asJson.noSpaces
        Http.postJsonAndParse[RegisterRsp](AccountRoute.registerRoute,data).map{rsp =>
          if(rsp.errCode==0){

          }
        }
    }
  }

  private def changeLogin() = {
    if(types == 1){
      types = 3
      loginType := "玩家登录"
      passwordBody := emptyHTML
      nickName :=
        <div style="margin-bottom:20px">
          <div>昵称</div>
          <input class="inputStyle" id="nickname"></input>
        </div>
      identityBody := emptyHTML
    } else {
      types = 1
      loginType := "游客登录"
      passwordBody :=
        <div style="margin-bottom:20px">
          <div>密码</div>
          <input class="inputStyle" id="password"></input>
        </div>
      identityBody :=
        <div style="margin-bottom:20px">
          <div>手机号/邮箱</div>
          <input class="inputStyle" placeholder="例如zhaoyin@ebupt.com" id="identity"></input>
        </div>
      nickName := emptyHTML
    }
  }


  private val header =
    <div class="header">
      <div style="display:flex">
        <img src="breakout/static/img/logo.svg" style="width:30px;height:30px"></img>
        <div style="line-height:30px;margin-left:5px">Yin's Breakout</div>
      </div>
      <div style="display:flex;width:100px;justify-content:space-around">
        <div onclick={()=>registerBody()}>注册</div>
        <div onclick={()=>loginBody()}>登录</div>
      </div>
    </div>
  private val body =
    <div class="bodyAll">
      <div class="bodyStyle">
        <div style="font-size:30px;margin-bottom:20px;margin-top:10px">{bodyName}</div>
        {identityBody}
        {nickName}
        {passwordBody}
        <div class="loginOrRegister" onclick={() => enter()}>{bodyName}</div>
        {anotherLogin}
      </div>
    </div>

  override def render: Elem = {
    <div style="width:100%;height:100%;background:#fafafa">
      {header}
      {body}
    </div>
  }
}
