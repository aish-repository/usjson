package WebLog.finalze.Package

import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.types._
import org.apache.spark.sql.types.StructType


class ReadInputFile(implicit spark: SparkSession) {
  import spark.implicits._
  implicit val Schema = StructType(Array(
    StructField("username", StringType, true),
    StructField("ip", StringType, true),
    StructField("dt", StringType, true),
    StructField("day", StringType, true),
    StructField("month", StringType, true),
    StructField("time1", StringType, true),
    StructField("yr", StringType, true),
    StructField("hr", StringType, true),
    StructField("mt", StringType, true),
    StructField("sec", StringType, true),
    StructField("tz", StringType, true),
    StructField("verb", StringType, true),
    StructField("page", StringType, true),
    StructField("index", StringType, true),
    StructField("fullpage", StringType, true),
    StructField("referrer", StringType, true),
    StructField("referrer2", StringType, true),
    StructField("statuscd", StringType, true)));

  def ReadXMLFile( FileXML :String, RowTag: String ): DataFrame={
    val XMLdf = spark.read.format("com.databricks.spark.xml")
      // .option("rowTag","httpstatus")
      // .load("file:///home/hduser/weblog/http_status.xml")
      .option("rowTag",RowTag)
      .load(FileXML)
    XMLdf

  }
  def ReadTextFile( FileText :String)(implicit  TextSchema: StructType = Schema ): DataFrame={
    val TEXTrdd = spark.sparkContext.textFile(FileText)
    val TEXTrow = TEXTrdd.map(x=>x.split(",")).map(x=>Row(x(0),x(1),x(2),x(3),x(4),x(5),x(6)
      ,x(7),x(8),x(9),x(10),x(11),x(12),x(13),x(14),x(15),x(15),x(16)))
    val TEXTdf = spark.createDataFrame(TEXTrow,TextSchema)
    TEXTdf
  }




}