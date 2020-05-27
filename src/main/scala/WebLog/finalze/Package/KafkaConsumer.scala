package WebLog.finalze.Package

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent

class KafkaConsumer (implicit ssc1 : StreamingContext,implicit val spark:SparkSession){

  val kafkaParams = Map[String, Object](
    "bootstrap.servers" -> "localhost:9092",
    "key.deserializer" -> classOf[StringDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> "usdatagroup",
    "auto.offset.reset" -> "latest",
    "enable.auto.commit" -> (false: java.lang.Boolean)
  )

  val topics = Array("usjson")

  def ConsumeKafkaMessages = {

    val stream = KafkaUtils.createDirectStream[String, String](
      ssc1,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams))

    println("Reading data from kafka")
    val streamdata =  stream.map(record => (record.value))
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

  }

}
