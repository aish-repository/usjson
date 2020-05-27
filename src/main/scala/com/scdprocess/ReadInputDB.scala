package com.scdprocess

import org.apache.spark.sql.{DataFrame, SparkSession}

class ReadInputDB (implicit spark: SparkSession) extends configParams {
/* Load all the records from given table into DF and return the DF*/
  def loadMySQLDB(DBname: String, TableName: String): DataFrame = {

    val mySQLdf = spark.read.format("jdbc")
      .option("url", s"$mysql_url$DBname")
      .option("driver", mysql_driver)
      .option("dbtable", TableName)
      .option("user", mysql_username)
      .option("password", mysql_password)
      .load()

    mySQLdf
  }

  def queryMySQLDB(DBname: String, Query: String): DataFrame = {

/* Load the records from mysql based on input query passed*/
    val qSQLdf = spark.read.format("jdbc")
      .option("url", s"$mysql_url$DBname")
      .option("driver", mysql_driver)
      .option("query", Query)
      .option("user", mysql_username)
      .option("password", mysql_password)
      .load()

    qSQLdf
  }
}