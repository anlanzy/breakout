package com.neo.sk.breakout.ptcl

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

  case class Player(
                     id:String,
                     name:String
                   )

  object GameState{
    val waiting:Int = -1
    val play:Int = 0
    val dead:Int = 1
    val allopatry:Int = 2
    val victory:Int = 3
  }

  object Boundary{
    val w = 1200
    val h = 600
  }



}
