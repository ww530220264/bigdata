package com.ww.flink.语法

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.{OutputTag, StreamExecutionEnvironment}
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

object STREAMING_5_Side_output {

  /**
   * 可以从如下Function将发射到Side output
   * ProcessFunction
   * KeyedProcessFunction
   * CoProcessFunction
   * KeyedCoProcessFunction
   * ProcessWindowFunction
   * ProcessAllWindowFunction
   */
  val configuration = new Configuration()
  configuration.setBoolean("queryable-state.enable", true)
  val env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI()

  def main(args: Array[String]): Unit = {
  }

  def _side_output = {
    val input = env.socketTextStream("centos7-1", 9999)
    val outputTag = OutputTag[String]("side-output")
    val mainStream = input.process(new ProcessFunction[String, Int] {
      override def processElement(
                                   value: String,
                                   ctx: ProcessFunction[String, Int]#Context,
                                   out: Collector[Int]): Unit = {
        out.collect(value.toInt)
        ctx.output(outputTag, "side-output-" + value)
      }
    })
    val sideOutputStream = mainStream.getSideOutput(outputTag)
    //  ...后续操作
  }
}
