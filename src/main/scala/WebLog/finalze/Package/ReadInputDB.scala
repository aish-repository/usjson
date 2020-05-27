package WebLog.finalze.Package

import com.scdprocess.configParams
import org.apache.spark.sql.{DataFrame, SparkSession}

class ReadInputDB (implicit spark: SparkSession) extends configParams{

  def loadMySQLDB (DBname:String, TableName:String): DataFrame   = {

    val mySQLdf = spark.read.format("jdbc")
      .option("url", s"$mysql_url$DBname")
      .option("driver", mysql_driver)
      .option("dbtable", TableName)
      .option("user", mysql_username)
      .option("password", mysql_password)
      .load()

    mySQLdf
  }


  def queryMySQLDB (DBname:String, Query:String): DataFrame = {


    val qSQLdf = spark.read.format("jdbc")
      .option("url", s"$mysql_url$DBname")
      .option("driver", mysql_driver)
      .option("query", Query)
      .option("user", mysql_username)
      .option("password", mysql_password)
      .load()

    qSQLdf
  }

  def refreshDB(custSQLdf: DataFrame,DBname:String, TableName:String): DataFrame = {

    val x=custSQLdf;
    println("Customer Profile Table is going to be refreshed")
    println("Record count Before refresh: " + custSQLdf.count())
    x.unpersist;
    val mySQLcustdf =  loadMySQLDB(DBname,TableName);
    println("Record count After refresh: " + mySQLcustdf.count())
    mySQLcustdf.cache();
    mySQLcustdf.createOrReplaceTempView("tv_custprof")
    mySQLcustdf
  }


}
