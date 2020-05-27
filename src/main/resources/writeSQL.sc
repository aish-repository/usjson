
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._



implicit val spark = SparkSession.builder()
  .appName("abc")
  .master("local[*]")
  .getOrCreate()

import spark.implicits._

val sc = spark.sparkContext

print(spark.version)

def jsonToDataFrame(json: String, schema: StructType = null): DataFrame = {
  // SparkSessions are available with Spark 2.0+
  val reader = spark.read
  Option(schema).foreach(reader.schema)
  reader.json(sc.parallelize(Array(json)))
}
val schema = new StructType().add("a", new StructType().add("b", IntegerType))

val events = jsonToDataFrame("""
{
  "a": {
     "b": 1
  }
}
""", schema)

display(events.select("a.b"))


/*
val kk = List(
  (Array(1, 2)),
  (Array(1, 2, 3, 1)),
  (null)
).toDF("nums")

val mm =
  kk.withColumn("arr_dist",array_distinct(col("nums")))


val jj = mm.
  withColumn("joined",
    array_join(col("arr_dist"),"@"))


jj.withColumn("union",
  array_union(col("nums"),col("arr_dist")))


jj.select(
    (0 until 3).map(i => $"nums".getItem(i).as(s"col$i")): _*
  )
  .show()

  */