package com.ww.spark.streaming

import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Seconds, StreamingContext}

@SerialVersionUID(11111111111111111L)
object NetworkWordCount {
  def main(args: Array[String]): Unit = {
//    if (args.length < 2) {
//      System.err.println("Usage: NetworkWordCount <hostname> <port>")
//      System.exit(1)
//    }

    val sparkConf = new SparkConf().setAppName("NetworkWordCount")
//      .setMaster("spark://wangwei:7077")
      .setMaster("local[2]")
    val ssc = new StreamingContext(sparkConf, Seconds(10))
//    ssc.sparkContext.addJar("E:\\workspace\\bigdata\\target\\sumEndPoint.jar")
    val lines = ssc.socketTextStream("centos7-1", 9999.toInt, StorageLevel.MEMORY_ONLY_SER)
    lines.flatMap(_.split(" ")).print()
//    val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _)
//    wordCounts.print()
    ssc.start()
    ssc.awaitTermination()
  }
}
