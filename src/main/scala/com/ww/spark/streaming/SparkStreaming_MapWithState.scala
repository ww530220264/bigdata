package com.ww.spark.streaming

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, State, StateSpec, StreamingContext}

object SparkStreaming_MapWithState {
  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf().setAppName("mapWithState").setMaster("local[3]")
    val ssc = new StreamingContext(sparkConf, Seconds(5))

    ssc.checkpoint("./opt/checkpoint/SparkStreaming_MapWithState")
//    ssc.sparkContext.setLogLevel("ERROR")
    val socketStream = ssc.socketTextStream("centos7-1", 9999)
    val mapWithStateFunc = (word: String, one: Option[Int], state: State[Int]) => {
      val sum = one.getOrElse(1) + state.getOption.getOrElse(0)
      state.update(sum)
      (word,sum)
    }
    val stateStream = socketStream
      .map(x => (x, 1))
      .reduceByKey(_+_)
//      .mapWithState(StateSpec.function(mapWithStateFunc))

    stateStream.print()

    ssc.start()
    ssc.awaitTermination()
  }
}
