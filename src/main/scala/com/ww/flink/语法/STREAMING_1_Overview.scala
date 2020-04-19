package com.ww.flink.STREAMING_3_EVENT_TIME$

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

object STREAMING_1_Overview {

  val env = StreamExecutionEnvironment.getExecutionEnvironment

  def main(args: Array[String]): Unit = {
    //  _1  //  Example
    //  _2  //  Streaming Iteration
    //  _3  //  Execution Parameters
    //  _4  //  Fault Tolerance
    //  _5
    /**
     * Controlling Latency 延迟控制
     * 默认情况下,元素没有在网络中逐个传输(会造成不必要的网络拥堵),但是他们会被缓冲;
     * buffer的大小可以再配置文件中配置,且这个方法有利于提高吞吐量,但是在输入流不足够快的时候,会造成延迟问题;
     * 可以通过env.setBufferTimeout(timeoutMillis)方法设置缓冲区等待被填满的最大时间,如果到了该时间之后,缓冲区仍然没有被填满,缓冲区中的数据也会被自动发送;
     * 为了使吞吐量最大化,setBufferTimeout(-1)方法将移除超时,只有缓冲区呗填满才会被刷出;
     * 为了最小化延迟,设置timeout为一个接近0的(5或10ms),避免设置为0,因为它会造成性能下降
     */
//      _6  //  Iterator Data Sink
    /**
     * 使用DataStreamUtils收集结果数据,返回一个迭代器,从迭代器获取结果数据
     */
    _test
  }

  def _test={
    val stream = env.fromElements((1, 2), (1, 3), (2, 3), (2, 4), (3, 5), (3, 6))
    stream.keyBy(0).sum(1).printToErr().setParallelism(1)
    env.execute()
  }
  def _6={

    import org.apache.flink.streaming.api.datastream.DataStreamUtils
    import scala.collection.JavaConverters._

    val result = env.fromCollection(Array(("a",1),("b",2),("c",3),("d",4)))
    val output = DataStreamUtils.collect(result.javaStream).asScala
    while (output.hasNext){
      println(output.next())
    }
  }

  def _5 = {
    env.setBufferTimeout(timeoutMillis = 100)
    env.generateSequence(1, 10).setBufferTimeout(100)
  }

  def _4 = {

  }

  def _3 = {
    val config = env.getConfig
    //  ...一系列set方法
  }

  def _2 = {
    val ds = env.socketTextStream("centos7-1", 9999)
    val someDataStream = ds.map(_.toInt)
    val iteration = someDataStream.iterate {
      iteartion => {
        val minusOne = iteartion.map {
          _ - 1
        }
        val stillGreaterThanZero = minusOne.filter {
          _ > 0
        }
        val lessThanZero = minusOne.filter {
          _ <= 0
        }
        (stillGreaterThanZero, lessThanZero)
      }
    }
    iteration.printToErr()
    env.execute()
  }

  def _1 = {
    val text = env.socketTextStream("centos7-1", 9999)
    val count = text.flatMap(_.split("\\W+"))
      .map((_, 1))
      .keyBy(0)
      .timeWindow(Time.seconds(5))
      .sum(1)
    count.print()
    env.execute()
  }
}
