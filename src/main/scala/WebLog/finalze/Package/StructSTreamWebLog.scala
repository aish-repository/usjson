package WebLog.finalze.Package

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.sql.types._
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.types.StringType
import com.wizzardo.tools.json


object StructSTreamWebLog {
  def main(args:Array[String]):Unit = {

    implicit val spark =  SparkSession.builder()
      .appName("Web Log Customer info")
      .master("local[*]")
      .config("hive.metastore.uris","thrift://localhost:9083")
      .config("spark.sql.warehouse.dir","hdfs://localhost:54310/user/hive/warehouse")
      .enableHiveSupport()
      .getOrCreate()

    val sc = spark.sparkContext
    sc.setCheckpointDir("/home/hduser/weblog/checkDir")
    sc.setLogLevel("ERROR")
    import spark.implicits._
    val readInputFile = new ReadInputFile()
    val httpDF = readInputFile.ReadXMLFile("file:///home/hduser/weblog/http_status.xml","httpstatus")
    httpDF.cache()
    httpDF.rdd.checkpoint()
    httpDF.createOrReplaceTempView("tv_https")


    val webLogdf = readInputFile.ReadTextFile("file:///home/hduser/weblog/WebLog")
    webLogdf.createOrReplaceTempView("tv_webLog")
    // webLogdf.show()

    val readInputDB = new ReadInputDB()
    val custSQLdf = readInputDB.loadMySQLDB("weblog", "custprof")
    custSQLdf.createOrReplaceTempView("tv_custprof")
    //webLogSQLdf.show

    val smallBatch = spark.read.format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "testjson")
      .option("startingOffsets", "earliest")
      .option("endingOffsets", """{"testjson":{"0":2}}""")
    //.option("endingOffsets",""" {"test":{"0":-1}} """)
      .load()
      .selectExpr("CAST(value AS STRING) as STRING").as[String].toDF()

     smallBatch.write.mode("overwrite").format("text").save("file:///home/hduser/batch")

    val smallBatchSchema: StructType = spark.read.json("file:///home/hduser/batch/part-*.txt").schema
    println(smallBatchSchema)

    val KafkaDF = spark.readStream.format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "testjson")
      .option("startingoffsets", "earliest")
      .load()

    val dataDf = KafkaDF.selectExpr("CAST(value AS STRING) as json")
      .select(from_json('json, smallBatchSchema).as("data"))
      .select("data.*")

    dataDf.createOrReplaceTempView("tv_usdataview")

    //,row_number() over(partition by usd.username order by yr
    //  ,month,day,hr,mt,sec) as version
    val finalDF = spark.sql("""
                    select concat(usd.username,day,month,yr,hr,mt,sec) as custid
								    ,usd.page,usd.cell,usd.first,usd.age,usd.email,
								    concat(usd.latitude,usd.longitude) as coordinates
								    ,usd.uscity,usd.country,usd.state,usd.username as wbuser
								    ,cp.age as age,cp.profession as profession,wl.ip
								    ,wl.dt,concat(wl.yr,'-',wl.time1,'-',wl.day) as fulldt,
								    wl.verb,wl.page,wl.statuscd,ws.category,ws.desc
								    ,case when wl.dt is null then 'new customer' else
								    'existing customer' end as custtype,count(1) as cnt
								      from
								    (select
  explode(results) as res,
  info.page as page,res.cell as cell,
  res.name.first as first,
  res.dob.age as age,
  res.email as email,res.location.city as uscity,res.location.coordinates.latitude as latitude,
  res.location.coordinates.longitude as longitude,res.location.country as country,
  res.location.state as state,
  res.location.timezone as timezone,res.login.username as username
 from tv_usdataview where info.page is not null) as usd left outer join tv_custprof cp on
 (substr(regexp_replace(cell,'[()-]',''),0,5)=cp.id)
 left outer join tv_weblog wl on (wl.username=substr(regexp_replace(cell,'[()-]',''),0,5))
 left outer join tv_https ws on (wl.statuscd=ws.cd)

 group by concat(usd.username,day,month,yr,hr,mt,sec)
								    ,usd.page,usd.cell,usd.first,usd.age,usd.email,
								    concat(usd.latitude,usd.longitude)
								    ,usd.uscity,usd.country,usd.state,usd.username
								    ,cp.age ,cp.profession ,wl.ip
								    ,wl.dt,concat(wl.yr,'-',wl.time1,'-',wl.day) ,
								    wl.verb,wl.page,wl.statuscd,ws.category,ws.desc
								    ,case when wl.dt is null then 'new customer' else
								    'existing customer' end
 order by concat(usd.username,day,month,yr,hr,mt,sec)
 """)




      finalDF.writeStream
      .format("console")
      .outputMode("complete")
      .start()
      .awaitTermination()
    /*
    val KafkaDF = spark.readStream.format("kafka")
      .option("kafka.bootstrap.servers","localhost:9092")
      .option("startingoffsets","earliest")
     // .option("endingoffsets","latest")
      .option("subscribe","testjson")
      .load()

    println("Reading data from kafka")
    val data = KafkaDF.selectExpr("CAST(value as String)").select("value").rdd.map(x=>x.toString())
  //val dataRDD: RDD[String] = data.select("value").rdd.map(x=>x.toString())

    val schema = new StructType()
      .add(StructField("time", IntegerType))
      .add(StructField("amountRange", IntegerType))
      .add(StructField("label", IntegerType))



    spark.read.option("multiline", "true")
      .option("mode", "DROPMALFORMED").json(data)
      .writeStream
      .format("console")
      .outputMode("append")
      .start()


    spark.streams.awaitAnyTermination()
*/

  }


}

