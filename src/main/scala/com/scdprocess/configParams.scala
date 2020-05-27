package com.scdprocess

import com.typesafe.config.ConfigFactory

trait configParams {

  val config = ConfigFactory.load("application.conf")
    .getConfig("usjson.weblog.analysis")

  /* Spark Configuration parameters*/
  val sparkConfig = config.getConfig("spark")
  val appName = sparkConfig.getString("app-name")
  val master = sparkConfig.getString("master")
  val logLevel = sparkConfig.getString("log-level")
  val chkpt = sparkConfig.getString("chkpt")

  /* Hive configuration parameters*/
  val hiveConfig = config.getConfig("hive")
  val hiveMetaStore =  hiveConfig.getString("hive.metastore.uris")
  val hiveWarehouse =  hiveConfig.getString("spark.sql.warehouse.dir")

  val hiveScd1TbName =  hiveConfig.getString("hive.scd1.TbName")
  val hiveScd1DbName  = hiveConfig.getString("hive.scd1.DbName")

  val hiveScd2TbName =  hiveConfig.getString("hive.scd2.TbName")
  val hiveScd2DbName  = hiveConfig.getString("hive.scd2.DbName")

  /*mySQL configuration parameters*/

  val mysqlConfig   = config.getConfig("mysql")
  val mysql_url =  mysqlConfig.getString("url")
  val mysql_driver =  mysqlConfig.getString("driver")
  val mysql_DBname =  mysqlConfig.getString("DBname")
  val mysql_scd1_TbName =  mysqlConfig.getString("sql.scd1.TbName")
  val mysql_scd2_TbName =  mysqlConfig.getString("sql.scd2.TbName")
  val mysql_username =  mysqlConfig.getString("username")
  val mysql_password =  mysqlConfig.getString("password")
  val primary_key =  mysqlConfig.getString("primary_key")
  val change_key =  mysqlConfig.getString("change_key")


  /* files configuration parameters*/

  val filesConfig      = config.getConfig("files")
  val scd1_Query       = filesConfig.getString("scd1_Query")
  val scd2_Query       = filesConfig.getString("scd2_Query")


}
