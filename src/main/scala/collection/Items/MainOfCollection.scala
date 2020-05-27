package collection.Items

import com.scdprocess.MainOfSCD.{appName, chkpt, hiveMetaStore, hiveWarehouse, logLevel, master}
import org.apache.spark.sql.SparkSession

object MainOfCollection {
  case class SubRecord(x: Int)
  case class ArrayElement(foo: String, bar: Int, vals: Array[Double])
  case class Record(
                     an_array: Array[Int], a_map: Map[String, String],
                     a_struct: SubRecord, an_array_of_structs: Array[ArrayElement])

  def main(args: Array[String]): Unit = {

    implicit val spark = SparkSession.builder()
      .appName(appName)
      .master(master)
      //   .config("hive.metastore.uris", hiveMetaStore)
      //   .config("spark.sql.warehouse.dir", hiveWarehouse)
      //   .enableHiveSupport()
      .getOrCreate()

    /*Set logger level to ERROR and set checkpoint directory*/
    val sc = spark.sparkContext
    sc.setLogLevel(logLevel)
    sc.setCheckpointDir(chkpt)

    import org.apache.spark.sql.functions.{udf, lit}
    import scala.util.Try



    import spark.implicits._
    case class SubRecord(x: Int)
    case class  ArrayElement(foo: String, bar: Int, vals: Array[Double])
    case class Record(
                       an_array: Array[Int], a_map: Map[String, String],
                       a_struct: SubRecord, an_array_of_structs: Array[ArrayElement])

    val df = sc.parallelize(Seq(
      Record(Array(1, 2, 3), Map("foo" -> "bar"), SubRecord(1),
        Array(
          ArrayElement("foo", 1, Array(1.0, 2.0, 2.0)),
          ArrayElement("bar", 2, Array(3.0, 4.0, 5.0)))),
      Record(Array(4, 5, 6), Map("foz" -> "baz"), SubRecord(2),
        Array(ArrayElement("foz", 3, Array(5.0, 6.0)),
          ArrayElement("baz", 4, Array(7.0, 8.0))))
    )).toDF()
    df.createOrReplaceTempView("df")
    df.printSchema

    df.select("an_array").show
/*
    root
    |-- an_array: array (nullable = true)
    |    |-- element: integer (containsNull = false)
    |-- a_map: map (nullable = true)
    |    |-- key: string
    |    |-- value: string (valueContainsNull = true)
    |-- a_struct: struct (nullable = true)
    |    |-- x: integer (nullable = false)
    |-- an_array_of_structs: array (nullable = true)
    |    |-- element: struct (containsNull = true)
    |    |    |-- foo: string (nullable = true)
    |    |    |-- bar: integer (nullable = false)
    |    |    |-- vals: array (nullable = true)
    |    |    |    |-- element: double (containsNull = false)
*/
    //select Array(1,2,3)



  }
}
