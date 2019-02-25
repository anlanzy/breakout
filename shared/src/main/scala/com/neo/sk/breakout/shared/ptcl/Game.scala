package com.neo.sk.breakout.shared.ptcl


import com.neo.sk.breakout.shared.ptcl.GameConfig._
/**
  * create by zhaoyin
  * 2019/1/31  5:20 PM
  */
object Game {

  //网格上的一个点
  case class Point(x: Int, y: Int) {
    def +(other: Point) = Point(x + other.x, y + other.y)

    def -(other: Point) = Point(x - other.x, y - other.y)

    def *(n: Int) = Point(x * n, y * n)

    def %(other: Point) = Point(x % other.x, y % other.y)
  }
  case class Score(id:String,n:String,k: Short, score: Short)
  case class RankInfo(
                     index:Int, //排名
                     score: Score //分数
                     )

  case class Player(
                     id:String,
                     name:String,
                     x:Int,
                     color: Byte,
                     ball: List[Ball],
                     score: Int
                   )

  case class Ball(
                   x:Int,
                   y:Int,
                   beforeX:Int,
                   beforeY:Int,
                   radius:Short = initBallRadius,
//                   speed:Float = initBallSpeed,//TODO 存疑？
                   speedX:Float = 0,
                   speedY:Float = 0,
                   onBoard : Boolean = true,
                 )

  case class Brick(
                  x:Int,
                  y:Int,
                  nums:Short //打多少次才会消失 【1-10】【10 - 20】【20 - 30】【30 - 50】
                  )

  object GameState{
    val waiting:Int = -1
    val play:Int = 0
    val dead:Int = 1
    val allopatry:Int = 2
    val victory:Int = 3
  }

  object Boundary{
    val w = 500
    val h = 650
  }


}
