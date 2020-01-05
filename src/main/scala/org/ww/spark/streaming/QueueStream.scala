package org.ww.spark.streaming

import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.collection.mutable


object QueueStream {
  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("QueueStream")
    val ssc = new StreamingContext(sparkConf, Seconds(2))

    val rddQueue = new mutable.Queue[RDD[Int]]()
    val inputStream = ssc.queueStream(rddQueue)
    val mappedStream = inputStream.map(x => (x, 1))
    val reducedStream = mappedStream.reduceByKey(_ + _)
    reducedStream.print(1000)

    ssc.start()
    for (i <- 1 to 20) {
      rddQueue.synchronized {
        rddQueue += ssc.sparkContext.makeRDD(1 to 100, 10)
      }
      Thread.sleep(1000)
    }
    Thread.sleep(3000)
    ssc.stop()


  }
}
