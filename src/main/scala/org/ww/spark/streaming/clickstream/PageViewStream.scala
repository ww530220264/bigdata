package org.ww.spark.streaming.clickstream

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

object PageViewStream {
  def main(args: Array[String]): Unit = {
    if (args.length != 3) {
      System.err.println("Usage: PageViewStream <metric> <host> <port>")
      System.err.println("<metric> must be one of pageCounts, slidingPageCounts," +
        " errorRatePerZipCode, activeUserCount, popularUsersSeen")
      System.exit(1)
    }

    val metric = args(0)
    val host = args(1)
    val port = args(2).toInt

    val sparkConf = new SparkConf().setAppName("PageViewStream").setMaster("local[2]")
    //    val ssc = new StreamingContext("local[2]",
    //      "PageViewStream", Seconds(1),
    //      System.getenv("SPARK_HOME"),
    //      StreamingContext.jarOfClass(this.getClass).toSeq)
    //一个batch的连续时间为2s
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    //本地运行时可以设置,集群需修改log4j
    ssc.sparkContext.setLogLevel("ERROR")
    ssc.checkpoint("../checkpoint")
    //url status zipCode userID
    val pageViews = ssc.socketTextStream(host, port).flatMap(_.split("\n")).map(PageView.fromString(_))
    //每个batch的每个url出现的次数
    val pageCounts = pageViews.map(view => view.url).countByValue()
    //每隔4秒统计最近10秒的出现的url的次数
    val slidingPageCounts = pageViews.map(view => view.url).countByValueAndWindow(Seconds(10), Seconds(4))
    val statusesPerZipCode = pageViews.window(Seconds(30), Seconds(2)).map(view => ((view.zipCode, view.status))).groupByKey()
    val errorRatePerZipCode = statusesPerZipCode.map {
      case (zip, statuses) =>
        val normalCount = statuses.count(_ == 200)
        val errorCount = statuses.size - normalCount
        val errorRatio = errorCount.toFloat / statuses.size
        if (errorRatio > 0.05) {
          "%s: **%s**".format(zip, errorRatio)
        } else {
          "%s: %s".format(zip, errorRatio)
        }
    }
    val activeUserCount = pageViews.window(Seconds(14), Seconds(2))
      .map(view => (view.userID, 1))
      .groupByKey()
      .count()
      .map("Unique active users" + _)
    val userList = ssc.sparkContext.parallelize(Seq(
      1 -> "Patrick Wendell",
      2 -> "Reynold Xin",
      3 -> "Matei Zahariz"
    ))
    metric match {
      case "pageCounts" => pageCounts.print()
      case "slidingPageCounts" => slidingPageCounts.print()
      case "errorRatePerZipCode" => errorRatePerZipCode.print()
      case "activeUserCount" => activeUserCount.print()
      case "popularUsersSeen" =>
        pageViews.map(view => (view.userID, 1))
          .foreachRDD((rdd, time) => rdd.join(userList).map(_._2._2).take(10)
            .foreach(u => println(s"Saw user $u at time $time")))
      case _ => println(s"Invalid metric entered: $metric")
    }

    ssc.start()
    ssc.awaitTermination()
  }

}
