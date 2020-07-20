package com.ww.spark

import org.apache.spark.sql.SparkSession

object Test_DataSet {

  case class Person(name:String,age:Long)

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
//      .appName("Test_DataSet")
      .master("spark://wangwei:7077")
      .master("local[3]")
      .getOrCreate()
    spark.conf.set("spark.local.dir","E:\\spark_local_dir")
    spark.sparkContext.addJar("E:\\workspace\\bigdata\\target\\sumEndPoint.jar")
    import spark.implicits._

    val personDF = spark.read
      .option("timestampFormat","yyyy-MM-dd HH:mm:ss.SSS")
      .json("file:///E:\\workspace\\bigdata\\src\\main\\resources\\data\\people.json")
    val personDS = personDF.as[Person]
    personDS.printSchema()
    personDS.show()

    spark.stop()
  }
}
