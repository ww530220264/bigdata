package org.ww.spark.streaming

import java.io.File

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.reflect.io.Directory

object HdfsWordCount {
  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      System.err.println("Usage: HdfsWordCount <directory>")
      System.exit(1)
    }
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("HdfsWordCount")
    val ssc = new StreamingContext(sparkConf, Seconds(1))
    //本地路径
    val lines = ssc.textFileStream(new Directory(new File(args(0))).path)
    val words = lines.flatMap(_.split(" "))
    //val wordCounts = words.map(x => (x, 1L)).reduceByKey(_ + _)
    //优化
    val wordCounts2 = words.map(x => (x, 1)).reduceByKeyAndWindow(_ + _, _ - _, Seconds(5), Seconds(1))

    wordCounts2.print()

    ssc.start()
    ssc.awaitTermination()
  }
}
