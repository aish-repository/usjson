package com.scdprocess

import org.apache.spark.sql.{DataFrame, SparkSession}

/* Implements SCD2 for order_scd2 table from mysql on hive
*  Extends trait hiveUpdateTrait that has the SCD1 and SCD2 core logic
*  Extends trait configParams having configuration parameters*/
object MainOfSCD extends hiveUpdateTrait with configParams {

  /* The program requires input argument of integer 1 or 2*/
  def main(args: Array[String]): Unit = {

    implicit val spark = SparkSession.builder()
      .appName(appName)
      .master(master)
      .config("hive.metastore.uris", hiveMetaStore)
      .config("spark.sql.warehouse.dir", hiveWarehouse)
      .enableHiveSupport()
      .getOrCreate()

/*Set logger level to ERROR and set checkpoint directory*/
    val sc = spark.sparkContext
    sc.setLogLevel(logLevel)
    sc.setCheckpointDir(chkpt)

    /* validate the SCD type as 1 or 2; Abort the program when otherwise*/
    val scd_type = args(0).toInt

    if (!(scd_type == 1 || scd_type == 2)) {
      println("Invalid SCD type code provided! Valid values are 1 or 2. Try Again!!!")
      System.exit(1)
    }
    println(s"Program initiated for SCD type $scd_type")
/*Based on the SCD type provided determine the hive and mySQL databases and table names to be accessed from config file*/
    val hive_table_name  = if (scd_type == 1) s"$hiveScd1DbName.$hiveScd1TbName" else s"$hiveScd2DbName.$hiveScd2TbName"

/* Ensure hive table exists to perform SCD overwrites accordingly*/
    val hive_tb_exists = spark.catalog.tableExists(hive_table_name)

/*Perform the core logic for SCD by invoking the method updateHiveSCD*/
    val finalDF: DataFrame = updateHiveScd(scd_type,hive_tb_exists)
    println(s"Total number of records to be written to Hive table $hive_table_name : " + finalDF.count)

    /*Update the hive table only when there are new records to update*/
    if (finalDF.count > 0) {
     finalDF.write.mode("overwrite").saveAsTable(s"$hive_table_name")
     println(s"$hive_table_name stored in hive successfully")
      // below show is for test only
      spark.sql(s"select * from $hive_table_name ").show(500, false)
    }
    else {
      /* No operation when no records to update*/
      println("No new updates available at this time!")
    }





  }


}





