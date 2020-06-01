package com.ww.spark.dataframe

import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{IntegerType, LongType, StringType, StructField, StructType, TimestampType}

object DATAFRAME_1_Overview {
  def main(args: Array[String]): Unit = {
//    val spark = SparkSession.builder.master("spark://172.27.192.177:7077").appName("DataFrame_1").getOrCreate
    val spark = SparkSession.builder.master("local[3]").appName("DataFrame_1").getOrCreate
//    val sc = spark.sparkContext
//    sc.hadoopConfiguration.set("fs.defaultFS", "hdfs://mycluster")
//    sc.hadoopConfiguration.set("dfs.nameservices", "mycluster")
//    sc.hadoopConfiguration.set("dfs.ha.namenodes.mycluster", "centos7-1,centos7-2")
//    sc.hadoopConfiguration.set("dfs.namenode.rpc-address.mycluster.centos7-1", "centos7-1:9000")
//    sc.hadoopConfiguration.set("dfs.namenode.rpc-address.mycluster.centos7-2", "centos7-2:9000")
//    sc.hadoopConfiguration.set("dfs.client.failover.proxy.provider.mycluster", "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider")
    task_1(spark)
  }

  def task_1(spark: SparkSession): Unit = {
    import spark.implicits._
    val postRDD = spark.sparkContext.textFile("file:///E:\\sourcecode\\spark\\spark-2.4.4\\examples\\src\\main\\resources\\my\\github\\ch05\\italianPosts.csv")
//    val postRDD = spark.sparkContext.textFile("hdfs://mycluster/mydata/italianPosts.csv")
    val tupleRDD = postRDD.map(_.split("~"))
      .map(x => (x(0), x(1), x(2), x(3), x(4), x(5), x(6), x(7), x(8), x(9), x(10), x(11), x(12)))
    // 方式1：如果不加参数的话，字段名称默认为_1,_2...字段类型为String
    println("RDD转DataFrame：df1")
    val df1 = tupleRDD.toDF("commentCount", "lastActivityDate",
      "ownerUserId", "body", "score", "creationDate", "viewCount", "title",
      "tags", "answerCount", "acceptedAnswerId", "postTypeId", "id")
    df1.printSchema()
    df1.show(10)
    // 方式2：case class
    println("RDD转DataFrame：df2")
    val df2 = postRDD.map(stringToPost(_)).toDF()
    df2.printSchema()
    df2.show(15)
    // 方式3：Schema
    println("RDD转DataFrame：df3")
    val df3 = spark.createDataFrame(postRDD.map(post2Row(_)), postSchema)
    df3.printSchema()
    df3.show(20)

    df3.columns.foreach(println)
    df3.dtypes.foreach(println)
//    spark.sparkContext.addJar("E:\\sourcecode\\spark\\spark-2.4.4\\examples\\target\\original-spark-examples_2.11-2.4.4.jar")
//    val parRDD = spark.sparkContext.parallelize(Seq("a","b","c","d","e","a","d"))
//    val mapRDD = parRDD.map((_, 1))
//    val groRDD = mapRDD.groupByKey()
//    groRDD.foreach(x=>println(s"key: ${x._1},values: ${x._2}"))
  }

  val postSchema: StructType = StructType(Seq(
    StructField("commentCount", IntegerType, true),
    StructField("lastActivityDate", TimestampType, true),
    StructField("ownerUserId", LongType, true),
    StructField("body", StringType, true),
    StructField("score", IntegerType, true),
    StructField("creationDate", TimestampType, true),
    StructField("viewCount", IntegerType, true),
    StructField("title", StringType, true),
    StructField("tags", StringType, true),
    StructField("answerCount", IntegerType, true),
    StructField("acceptedAnswerId", LongType, true),
    StructField("postTypeId", LongType, true),
    StructField("id", LongType, true)
  ))

  import StringImplicits._

  def post2Row(s: String): Row = {
    val r = s.split("~")
    Row(
      r(0).toIntSafe.getOrElse(null),
      r(1).toTimestampSafe.getOrElse(null),
      r(2).toLongSafe.getOrElse(null),
      r(3),
      r(4).toIntSafe.getOrElse(null),
      r(5).toTimestampSafe.getOrElse(null),
      r(6).toIntSafe.getOrElse(null),
      r(7),
      r(8),
      r(9).toIntSafe.getOrElse(null),
      r(10).toLongSafe.getOrElse(null),
      r(11).toLongSafe.getOrElse(null),
      r(12).toLong
    )
  }

  def stringToPost(row: String): Post = {
    val r = row.split("~")
    Post(
      r(0).toIntSafe,
      r(1).toTimestampSafe,
      r(2).toLongSafe,
      r(3),
      r(4).toIntSafe,
      r(5).toTimestampSafe,
      r(6).toIntSafe,
      r(7),
      r(8),
      r(9).toIntSafe,
      r(10).toLongSafe,
      r(11).toLongSafe,
      r(12).toLong
    )
  }
}

import java.sql.Timestamp

case class Post(
                 commentCount: Option[Int],
                 lastActivityDate: Option[java.sql.Timestamp],
                 ownerUserId: Option[Long],
                 body: String,
                 score: Option[Int],
                 creationDate: Option[java.sql.Timestamp],
                 viewCount: Option[Int],
                 title: String,
                 tags: String,
                 answerCount: Option[Int],
                 acceptedAnswerId: Option[Long],
                 postTypeId: Option[Long],
                 id: Long
               )

object StringImplicits {

  implicit class StringImprovements(val s: String) {

    import scala.util.control.Exception.catching

    def toIntSafe = catching(classOf[NumberFormatException]).opt(s.toInt)

    def toLongSafe = catching(classOf[NumberFormatException]).opt(s.toLong)

    def toTimestampSafe = catching(classOf[IllegalArgumentException]).opt(Timestamp.valueOf(s))
  }

}
