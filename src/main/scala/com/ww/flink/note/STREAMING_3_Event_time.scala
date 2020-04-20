package com.ww.flink.STREAMING_3_EVENT_TIME$

import java.util.Properties

import org.apache.flink.api.common.serialization.DeserializationSchema
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.datastream.DataStreamUtils
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.assigners.WindowAssigner
import org.apache.flink.streaming.api.windowing.time.Time

object STREAMING_3_Event_time {

  val env = StreamExecutionEnvironment.getExecutionEnvironment

  def main(args: Array[String]): Unit = {
    //_watermarker_bounded_outofOrdere
    _watermarker_bounded_outofOrdere
    env.execute()
  }

  def _watermarker_assigned_periodic = {
    /**
     * 设置并行度
     * 设置时间特性/语义
     */
    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    /**
     * watermark > window.end-time时触发窗口计算
     */
    val ds = env.socketTextStream("centos7-1", 9999)
      .map(_.split(","))
      .map(x => (x(0), x(1).toLong, x(2).toInt))
      .assignTimestampsAndWatermarks(
        new AssignerWithPeriodicWatermarks[(String, Long, Int)] {
          val maxOutofOrderness = 1000L
          var currenetMaxTimestamp: Long = 0L

          //  生成watermark
          override def getCurrentWatermark: Watermark = {
            new Watermark(currenetMaxTimestamp - maxOutofOrderness)
          }

          //  抽取event-time时间戳
          override def extractTimestamp(element: (String, Long, Int), previousElementTimestamp: Long): Long = {
            val currentTimestamp = element._2
            currenetMaxTimestamp = Math.max(currentTimestamp, currenetMaxTimestamp)
            currentTimestamp
          }
        }
      )
    val ss = ds.keyBy(0).timeWindow(Time.seconds(5)).sum(2)
    ss.printToErr()
  }

  def _watermarker_bounded_outofOrdere = {
    /**
     * 设置并行度
     * 设置时间特性/语义
     */
    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    /**
     * 设置延迟时间 watermark = event.event-time - 延迟时间
     * watermark > window.end-time时触发窗口计算
     */
    val outofOrderTime = 3
    val ds = env.socketTextStream("centos7-1", 9999)
      .map(_.split(","))
      .map(x => (x(0), x(1).toLong, x(2).toInt))
      .assignTimestampsAndWatermarks(
        new BoundedOutOfOrdernessTimestampExtractor[(String, Long, Int)]
        (Time.seconds(outofOrderTime)) {
          //  抽取event-time时间戳
          override def extractTimestamp(element: (String, Long, Int)): Long = {
            System.err.println(element._2)
            element._2
          }
        })
    val ss = ds.keyBy(0).timeWindow(Time.seconds(5)).sum(2)
    ss.printToErr()
  }

  /**
   * 从SourceFunction中抽取Event-time时间戳 ctx.collectWithTimestamp
   * 从SourceFunction中生成watermark ctx.emitWatermark
   */
  def _watermarker_sourceFunc = {
    /**
     * 设置并行度
     * 设置时间特性/语义
     */
    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    val late = 2
    val input = List(("a", 20L, 1), ("a", 19L, 1), ("a", 30L, 1),
      ("a", 20L, 1), ("a", 30L, 1),
      ("a", 40L, 1), ("a", 50L, 1))
    env.addSource(new SourceFunction[(String, Long, Int)] {
      override def run(ctx: SourceFunction.SourceContext[(String, Long, Int)]): Unit = {
        input.foreach { x =>
          ctx.collectWithTimestamp(x, x._2)
          System.err.println("收集时间戳--" + x + x._2)
          ctx.emitWatermark(new Watermark(x._2 - late))
          System.err.println("发射watermarker--" + (x._2 - late))
        }
        ctx.emitWatermark(new Watermark(Long.MaxValue))
      }

      override def cancel(): Unit = ???
    }).keyBy(0)
      .timeWindow(Time.milliseconds(10))
      .sum(2)
      .printToErr()
  }

  def _1 = {
    //  设置DataStream的时间特性
    env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)
    //  env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    //  env.setStreamTimeCharacteristic(TimeCharacteristic.IngestionTime)
    case class MyEvent(user: String, money: Int) {}
    val topic = ""
    val prop = new Properties()
    val stream = env.addSource(new FlinkKafkaConsumer010[MyEvent](topic, new DeserializationSchema[MyEvent] {
      override def deserialize(bytes: Array[Byte]): MyEvent = {
        ???
      }

      override def isEndOfStream(t: MyEvent): Boolean = ???

      override def getProducedType: TypeInformation[MyEvent] = ???
    }, prop))

    val reslut = stream
      .keyBy(_.user)
      .timeWindow(Time.seconds(10))
      .reduce((a, b) => MyEvent(a.user, a.money + b.money))

    import scala.collection.JavaConverters._
    val iter = DataStreamUtils.collect(reslut.javaStream).asScala
    while (iter.hasNext) {
      println(iter.next())
    }
  }
}
