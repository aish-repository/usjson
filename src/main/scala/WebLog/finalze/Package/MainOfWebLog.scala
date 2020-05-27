package WebLog.finalze.Package

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Seconds, StreamingContext}

object MainOfWebLog {
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



      implicit val ssc1 = new StreamingContext(sc, Seconds(30))

      import org.apache.spark.streaming.dstream.ConstantInputDStream;
      val 	dynamiclkp=new ConstantInputDStream(ssc1,sc.parallelize(Seq())).window(Seconds(60),Seconds(60))

      // Refresh the table data every 60 seconds
          dynamiclkp.foreachRDD{
            x=>{
              readInputDB.refreshDB(custSQLdf,"weblog","custprof")
            }
          }


    //  val kafkaConsumer = new KafkaConsumer()
    //  kafkaConsumer.ConsumeKafkaMessages
      //ssc1.checkpoint("hdfs://localhost:54310/user/hduser/usjsonckpt")
      //streamdata.print()*/
 val kafkaParams = Map[String, Object](
   "bootstrap.servers" -> "localhost:9092",
   "key.deserializer" -> classOf[StringDeserializer],
   "value.deserializer" -> classOf[StringDeserializer],
   "group.id" -> "usdatagroup",
   "auto.offset.reset" -> "earliest",
   "enable.auto.commit" -> (false: java.lang.Boolean)
 )


      val topics = Array("usjson")
      val stream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream[String, String](
        ssc1,
        PreferConsistent,
        Subscribe[String, String](topics, kafkaParams)
      )
      //ssc1.checkpoint("hdfs://localhost:54310/user/hduser/usjsonckpt")
      println("Reading data from kafka")
      val streamdata: DStream[String] =		stream.map(record => (record.value))
      streamdata.foreachRDD(rdd=>

        if(!rdd.isEmpty()) {
          //val offsetranges=rdd.asInstanceOf[HasOffsetRanges].offsetRanges
          val jsondf = spark.read.option("multiline", "true")
            .option("mode", "DROPMALFORMED").json(rdd)
          try {
            //val userwithid= jsondf.withColumn("results",explode($"results")).
            //select("results[0].username")
            jsondf.printSchema();
            jsondf.createOrReplaceTempView("tv_usdataview")
            //
            val finaldf = spark.sql(
              """
                    select concat(usd.username,day,month,yr,hr,mt,sec) as custid
								    ,row_number() over(partition by usd.username order by yr
								    ,month,day,hr,mt,sec) as version
								    ,usd.page,usd.cell,usd.first,usd.age,usd.email,
								    concat(usd.latitude,usd.longitude) as coordinates
								    ,usd.uscity,usd.country,usd.state,usd.username
								    ,cp.age as age,cp.profession as profession,wl.ip
								    ,wl.dt,concat(wl.yr,'-',wl.time1,'-',wl.day) as fulldt,
								    wl.verb,wl.page,wl.statuscd,ws.category,ws.desc
								    ,case when wl.dt is null then 'new customer' else
								    'existing customer' end as custtype
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
 left outer join tv_https ws on (wl.statuscd=ws.cd)""")
            finaldf.show(false)

            //finaldf.saveToEs("usdataidx/usdatatype")
            println("data written to ES")
          }

          catch {
            case ex1: java.lang.IllegalArgumentException => {
              println("Illegal arg exception")
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
        }
      )

          ssc1.start()
          ssc1.awaitTermination()

    }


}

