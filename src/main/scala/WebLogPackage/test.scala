package WebLogPackage
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming._
object test extends App {
  val spark = SparkSession.builder().appName("newMaven").
    master("local[*]")
    .config("hive.metastore.uris", "thrift://localhost:9083")
    .config("spark.sql.warehouse.dir", "hdfs://localhost:54310/user/hive/warehouse")
    .enableHiveSupport().getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")


  val sc = spark.sparkContext

//  val testRDD = sc.textFile("file:///home/hduser/weblog/Test_hive")
//  testRDD.saveAsTextFile("hdfs://localhost:54310/user/hduser/testRDD2.txt")
  val testHive = spark.read.csv("file:///home/hduser/weblog/Test_hive")
  testHive.write.mode("overwrite").saveAsTable("default.test_hive2")
  println("data written to hive")

}
