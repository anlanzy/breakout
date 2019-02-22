package com.neo.sk.breakout.utils

import slick.codegen.SourceCodeGenerator
import slick.jdbc.{JdbcProfile,PostgresProfile,H2Profile}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * User: Taoz
 * Date: 7/15/2015
 * Time: 9:33 AM
 */
object MySlickCodeGenerator {


  import concurrent.ExecutionContext.Implicits.global

  /**postgresql**/
//  val slickDriver = "slick.jdbc.PostgresProfile"
//  val jdbcDriver = "org.postgresql.Driver"
//val url = "jdbc:postgresql://10.1.29.250:5432/gypsy"

  /**h2**/
  val slickDriver = "slick.jdbc.H2Profile"
  val jdbcDriver = "org.h2.Driver"
  val url = "jdbc:h2:file:/Users/zhaoyin/neotel/breakout/backend/data/breakout"
//  val url = "jdbc:postgresql://10.1.29.247:5432/gypsy"
 // val url = "jdbc:postgresql://localhost:5432/gypsy"
  val outputFolder = "target/gencode/genTablesPsql"
  val pkg = "com.neo.sk.breakout.models"
  val user = "zy"
  val password = "breakout"

  def genCustomTables(dbDriver: JdbcProfile) = {

    // fetch data model
    val driver: JdbcProfile =
      Class.forName(slickDriver + "$").getField("MODULE$").get(null).asInstanceOf[JdbcProfile]
    val dbFactory = driver.api.Database
    val db = dbFactory.forURL(url, driver = jdbcDriver,
      user = user, password = password, keepAliveConnection = true)


    // fetch data model
    val modelAction = dbDriver.createModel(Some(dbDriver.defaultTables)) // you can filter specific tables here
    val modelFuture = db.run(modelAction)

    // customize code generator
    val codeGenFuture = modelFuture.map(model => new SourceCodeGenerator(model) {
      // override mapped table and class name
      override def entityName =
        dbTableName => "r" + dbTableName.toCamelCase

      override def tableName =
        dbTableName => "t" + dbTableName.toCamelCase

      // add some custom import
      // override def code = "import foo.{MyCustomType,MyCustomTypeMapper}" + "\n" + super.code

      // override table generator
      /*    override def Table = new Table(_){
            // disable entity class generation and mapping
            override def EntityType = new EntityType{
              override def classEnabled = false
            }

            // override contained column generator
            override def Column = new Column(_){
              // use the data model member of this column to change the Scala type,
              // e.g. to a custom enum or anything else
              override def rawType =
                if(model.name == "SOME_SPECIAL_COLUMN_NAME") "MyCustomType" else super.rawType
            }
          }*/
    })

    val codeGenerator = Await.result(codeGenFuture, Duration.Inf)
    codeGenerator.writeToFile(
      slickDriver, outputFolder, pkg, "SlickTables", "SlickTables.scala"
    )

  }

  def genDefaultTables() = {
    slick.codegen.SourceCodeGenerator.main(
      Array(slickDriver, jdbcDriver, url, outputFolder, pkg, user, password)
    )
  }

  def main(args: Array[String]) {
    //genDefaultTables()
    val dbDriver:JdbcProfile = H2Profile

    genCustomTables(dbDriver)

    println(s"Tables.scala generated in $outputFolder")

  }

}


