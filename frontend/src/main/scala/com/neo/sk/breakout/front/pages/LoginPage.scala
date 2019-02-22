package com.neo.sk.breakout.front.pages

import com.neo.sk.breakout.front.common.Page
import com.neo.sk.breakout.front.utils.Shortcut
import mhtml._
import scala.xml.Elem

/**
  * create by zhaoyin
  * 2019/2/22  10:10 AM
  */
class LoginPage extends Page{

  private val bodyName = Var("登录")
  private val nickName = Var(emptyHTML)


  private def loginBody() = {
    bodyName := "登录"
    nickName := emptyHTML
  }

  private def registerBody() = {
    bodyName := "注册"

    nickName :=
      <div style="margin-bottom:20px">
        <div>昵称</div>
        <input class="inputStyle"></input>
      </div>
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
        <div style="margin-bottom:20px">
          <div>手机号/邮箱</div>
          <input class="inputStyle" placeholder="例如zhaoyin@ebupt.com"></input>
        </div>
        {nickName}
        <div style="margin-bottom:20px">
          <div>密码</div>
          <input class="inputStyle"></input>
        </div>
        <div class="loginOrRegister">{bodyName}</div>
      </div>
    </div>

  override def render: Elem = {
    <div style="width:100%;height:100%;background:#fafafa">
      {header}
      {body}
    </div>
  }
}
