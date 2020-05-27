package WebLogPackage


import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.serialization.IntegerSerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.spark.streaming.dstream.DStream
//import org.apache.logging.log4j.LogManager
//import org.apache.logging.log4j.Logger
import org.apache.spark._
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.types._
import org.apache.spark.streaming.StreamingContext;
import org.apache.spark.streaming._;
import java.io._
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.StructType
import java.lang._
import org.apache.spark.SparkDriverExecutionException
import org.json4s.{DefaultFormats,jackson}
import org.elasticsearch.spark.streaming._
import org.elasticsearch.spark.sql._
import org.elasticsearch.spark._
import org.apache.spark.sql.SparkSession

import java.util.Properties
import org.apache.spark.sql.functions._


import org.apache.spark.sql.SparkSession

object Weblog extends App {
  //System.setProperty("hadoop.home.dir", "D:\\winutils\\")
  val spark = SparkSession.builder().appName("newMaven").
    master("local[*]")
    .config("es.index.auto.create","true")
    .config("es.mapping.id","custid")
    .config("hive.metastore.uris", "thrift://localhost:9083")
    .config("spark.sql.warehouse.dir", "hdfs://localhost:54310/user/hive/warehouse")
    .enableHiveSupport()
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")


  val sc = spark.sparkContext

  val kafkaParams = Map[String, Object](
    "bootstrap.servers" -> "localhost:9092",
    "key.deserializer" -> classOf[StringDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> "kafkatest1",
    "auto.offset.reset" -> "earliest",
    "enable.auto.commit" -> (false: java.lang.Boolean)
  )

  val ssc1 = new StreamingContext(sc, Seconds(5))

  val topics = Array("usjson")
  val stream = KafkaUtils.createDirectStream[String, String](
    ssc1,
    PreferConsistent,
    Subscribe[String, String](topics, kafkaParams)
  )
  //ssc1.checkpoint("hdfs://localhost:54310/user/hduser/usjsonckpt")

  println("Reading data from kafka")
  val streamdata: DStream[String] =		stream.map(record => (record.value))


  streamdata.foreachRDD(foreachFunc = rdd =>

    if (!rdd.isEmpty()) {
      //val offsetranges=rdd.asInstanceOf[HasOffsetRanges].offsetRanges

      val jsondf = spark.read.option("multiline", "true")
        .option("mode", "DROPMALFORMED").json(rdd)
      try {
        jsondf.printSchema()
        jsondf.show(false)
        jsondf.createOrReplaceTempView("usdataview")
        val esDF = spark.sql(
        """select concat(usd.username,current_timestamp) as custid
          ,usd.uscity,usd.country,usd.state,usd.username from
(select
  explode(results) as res,
  info.page as page,res.cell as cell,
  res.name.first as first,
  res.dob.age as age,
  res.email as email,res.location.city as uscity,res.location.coordinates.latitude as latitude,
  res.location.coordinates.longitude as longitude,res.location.country as country,
  res.location.state as state,
  res.location.timezone as timezone,res.login.username as username
 from usdataview where info.page is not null) as usd """)

        esDF.saveToEs("usdataidx1/usdatatyp")



        val weblogschema = StructType(Array(
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
        //01784,361.321.73.6,17/Nov/2011:13:08:30 -0500
        //,17,Nov,11,2011,13,08,30,-0500,GET,/demo,0,/demo,Jakarta Commons-HttpClient/3.0-rc4,100
        val weblogrdd= sc.textFile("file:///home/hduser/weblog/WebLog")

        val weblogrow = weblogrdd.map(x=>x.split(",")).map(x=>Row(x(0),x(1),x(2),x(3),x(4),x(5),x(6)
          ,x(7),x(8),x(9),x(10),x(11),x(12),x(13),x(14),x(15),x(15),x(16)))

        val weblogdf=spark.createDataFrame(weblogrow,weblogschema)
        val testHive = spark.read.csv("file:///home/hduser/weblog/Test_hive")
        testHive.write.mode("overwrite").saveAsTable("default.test_hive3")
        println("data written to hive")

       spark.sql("select * from default.test_hive3").show

        println("data written to ES")
      }

     catch {
        case ex1: java.lang.IllegalArgumentException => {
          println(s"Illegal arg exception")
        }

        case ex2: java.lang.ArrayIndexOutOfBoundsException => {
          println("Array index out of bound")
        }

        case ex3: org.apache.spark.SparkException => {
          println("Spark common exception")


        }


        case ex6: java.lang.NullPointerException => {
          println("Values Ignored")


        }
      }

    })


  ssc1.start()
  ssc1.awaitTermination()

}




