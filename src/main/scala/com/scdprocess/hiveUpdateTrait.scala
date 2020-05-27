package com.scdprocess

import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.row_number

trait hiveUpdateTrait extends configParams {

  /* This trait has common logic for SCD type 1 and SCD type 2*/
  /* Based on user's input, SCD type is determined and processed*/

  def updateHiveScd(scd_type: Int,hive_exists: Boolean)(implicit spark: SparkSession): DataFrame = {

    val hive_table_name   = if (scd_type == 1) s"$hiveScd1DbName.$hiveScd1TbName" else s"$hiveScd2DbName.$hiveScd2TbName"
    val mysql_table_name  = if (scd_type == 1) mysql_scd1_TbName else mysql_scd2_TbName
    val scd_query_file    = if (scd_type == 1) scd1_Query else scd2_Query

    /*When hive table exists */
      hive_exists match  {
      case true =>
      {
        /*Load the hive data into DF and checkpoint to remove the lineage thereby allowing table overwrites*/
        val hiveDf = spark.sql(s"select * from $hive_table_name").checkpoint()

        hiveDf.createOrReplaceTempView("tv_orders_old")

        /*Determine the last modified timestamp from hive*/

        val max = spark.sql(s"select max($change_key) from  $hive_table_name").first().get(0)
        val max_ts = if (max == null) "0001-01-01 00:00:00" else max.toString
        println(max_ts)

        /*Determine the incremental new records in mysql based on last modified timestamp from hive*/

        val readInputDB = new ReadInputDB()
        val ordersSQLdf = readInputDB.queryMySQLDB(mysql_DBname,
          s"select * from $mysql_table_name where $change_key > '$max_ts' ")

        println(s"Number of new records from $mysql_DBname.$mysql_table_name:" + ordersSQLdf.count)

        /*When there are new updates found in mysql*/

        if (ordersSQLdf.count > 0) {
          ordersSQLdf.createOrReplaceTempView("tv_orders_new")


          val scdQuery = spark.sparkContext.textFile(scd_query_file).take(1)(0)
          spark.sql(scdQuery)
        }
          /* When no incremental changes in mysql*/
        else{ordersSQLdf}

      }

      /* When the hive table does not exists; first time run only*/
      case false =>
      {

        println("hive table does not exists!! Creating and loading a new hive table")
        println(s"All data from table $mysql_DBname.$mysql_table_name collected for update ")
        val readInputDB = new ReadInputDB()
        if (scd_type == 2) {
        readInputDB.loadMySQLDB(mysql_DBname, mysql_table_name)
          .withColumn("version",row_number().over(Window.partitionBy(primary_key)
            .orderBy(change_key)))}
        else {
          readInputDB.loadMySQLDB(mysql_DBname, mysql_table_name)
        }
        //println("Number of new records to be updated:" + finalDF.count)

      }

    }

  }


  def updateHiveScd2(hive_exists: Boolean)(implicit spark: SparkSession): DataFrame = {
    hive_exists match  {
      case true =>
      {
        val hiveDf = spark.sql(s"select * from $hiveScd2DbName.$hiveScd2TbName").checkpoint()

        hiveDf.createOrReplaceTempView("tv_orders_old")

        val max = spark.sql(s"select max($change_key) from $hiveScd2DbName.$hiveScd2TbName").first().get(0)

        val max_ts = if (max == null) "0001-01-01 00:00:00" else max.toString

        println(max_ts)


        val readInputDB = new ReadInputDB()
        val ordersSQLdf = readInputDB.queryMySQLDB(mysql_DBname,
          s"select * from $mysql_DBname.$mysql_scd2_TbName where $change_key > '$max_ts' ")

        println("Number of new records to be updated:" + ordersSQLdf.count)


        if (ordersSQLdf.count > 0) {
          ordersSQLdf.createOrReplaceTempView("tv_orders_new")

          val scd2Query = spark.sparkContext.textFile(scd2_Query).take(1)(0)


          spark.sql(scd2Query)

        /*  spark.sql("""select order_ts,nw.orderNumber,orderDate,requiredDate,shippedDate,status,comments,customerNumber
        ,coalesce ((old_max_version + row_number() over (partition by nw.orderNumber order by order_ts) ),
        row_number() over (partition by nw.orderNumber order by order_ts)) as version
        from
        tv_orders_new nw
        left join
          (select max(version) as old_max_version,orderNumber from tv_orders_old group by orderNumber) old
        on old.orderNumber = nw.orderNumber

        union

        select * from tv_orders_old """)
*/
        }
        else {
          ordersSQLdf
        }


      }
      case false =>
      {

        println("hive table does not exists!! Creating and loading a new hive table")
        val readInputDB = new ReadInputDB()
        readInputDB.loadMySQLDB(mysql_DBname, mysql_scd2_TbName)
          .withColumn("version",row_number().over(Window.partitionBy(primary_key)
            .orderBy(change_key)))
        //println("Number of new records to be updated:" + finalDF.count)

      }

    }

  }
}
