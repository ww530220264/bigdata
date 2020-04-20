package com.ww.flink.STREAMING_3_EVENT_TIME$

import org.apache.flink.api.common.functions.{MapFunction, RichMapFunction}
import org.apache.flink.api.common.operators.Order
import org.apache.flink.api.java.functions.FunctionAnnotation.{ForwardedFields, NonForwardedFields}
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.api.scala._
import org.apache.flink.configuration.Configuration
import org.apache.flink.core.fs.FileSystem.WriteMode

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Random

object BATCH_1_Overview {

  case class Point(x: Double, y: Double)

  val env = ExecutionEnvironment.getExecutionEnvironment

  def main(args: Array[String]): Unit = {
    _1 //扩展,支持匿名模式匹配
    //  _2  //Example 输出Sink分区排序
    //  _3  //MapPartition
    //  _4  //Reduce/ReduceGroup
    //  _5  //minBy/maxBy
    //  _6  //使用迭代,计算PI bulk iteration 大量的迭代,需要制定最大迭代次数
    //      //还有增量迭代
    //  _7  //FieldsForwardAnnotation
    //  _8  //广播变量
  }

  def _8 = {
    val toBroadcast = env.fromElements(1, 2, 3)
    val data = env.fromElements("a", "b")

    data.map(new RichMapFunction[String, String]() {
      var broadcast: Traversable[String] = null

      override def open(config: Configuration): Unit = {
        broadcast = getRuntimeContext
          .getBroadcastVariable[String]("broadcastSetName")
          .asScala
      }

      override def map(in: String): String = {
        in + "----"
      }
    }).withBroadcastSet(toBroadcast, "broadcastSetName")
      .printToErr()
  }

  def _7 = {

    @ForwardedFields(Array("_1->_3"))
    class MyMap extends MapFunction[(Int, Int), (String, Int, Int)] {
      override def map(value: (Int, Int)): (String, Int, Int) = {
        ("foo", value._2, value._1)
      }
    }
    @NonForwardedFields(Array("_1"))
    class MyMap2 extends MapFunction[(Int, Int), (Int, Int)] {
      override def map(value: (Int, Int)): (Int, Int) = {
        (value._1, value._2 / 2)
      }
    }

    val ds = env.fromElements[(Int, Int)]((1, 2), (3, 4), (5, 6))
    ds.map(new MyMap()).printToErr()
    //或者不使用注解
    //ds.map(new MyMap()).withForwardedFields("_1->_3").printToErr()
    ds.map(new MyMap2).printToErr()
  }

  def _6 = {

    val initial = env.fromElements(0)
    val count = initial.iterate(10000) { iterationInput: DataSet[Int] => {
      val result = iterationInput.map { i =>
        val x = Math.random()
        val y = Math.random()

        i + (if (x * x + y * y < 1) 1 else 0)
      }
      result
    }
    }
    val result = count.map { c => c / 10000.0 * 4 }
    result.printToErr()

  }

  def _5 = {
    val ds = env.fromElements(
      (1, 0.3, "a"),
      (2, 0.1, "b"),
      (3, 0.4, "c"),
      (1, 0.2, "b"),
      (2, 0.5, "a"))
    ds.minBy(1, 2).printToErr()
    println("---")
    ds.groupBy(2).maxBy(1).printToErr()
  }

  def _4 = {

    val ds = env.fromElements((1, 1), (1, 2), (2, 3), (2, 4))
    ds.groupBy(0).reduce { (x1, x2) => (x1._1, x1._2 + x2._2) }.printToErr()
    println("-----")
    ds.groupBy(0).reduceGroup {
      x => x.reduce((x1, x2) => (x1._1, x1._2 + x2._2))
    }.printToErr()
  }


  def _3 = {
    val data = new mutable.MutableList[(Int, Long, String)]
    data.+=((1, 1L, "Hi"))
    data.+=((2, 2L, "Hello"))
    data.+=((3, 2L, "Hello world"))
    data.+=((4, 3L, "Hello world, how are you?"))
    val collection = env.fromCollection(Random.shuffle(data))
    val unique = collection.partitionByHash(1).mapPartition {
      line =>
        line.map(x => (x._1, x._2, x._3))
    }
    unique.writeAsText("hashPartition", WriteMode.NO_OVERWRITE)
    env.execute()
  }

  def _2 = {
    val text = env.fromElements(
      "who's there",
      "I Think I hear them. Stand, ho! Who's there?"
    )
    val counts = text.flatMap {
      _.toLowerCase.split("\\W+") filter {
        _.nonEmpty
      }
    }.map {
      (_, 1)
    }.groupBy(0).sum(1)

    /**
     * sortPartition("_2", Order.ASCENDING): 按第二个字段排序
     * setParallelism(1): 设置并行度为1
     */
    counts.setParallelism(1).sortPartition("_2", Order.ASCENDING).print()
  }

  /**
   * 正常情况下不支持匿名模式匹配 tuples, case classes or collections
   * 扩展执行匿名模式匹配
   * flatMap -->flatMapWith
   * filter  --> filterWith
   * 等等..
   */
  def _1 = {
    // 支持匿名模式匹配需要添加如下代理方法
    // DataSet
    import org.apache.flink.api.scala.extensions.acceptPartialFunctions
    // DataStream
    // import org.apache.flink.streaming.api.scala.extensions.acceptPartialFunctions

    env.setParallelism(3)
    val ds = env.fromElements(Point(1, 2), Point(3, 2),
      Point(3, 4), Point(5, 6))
    ds.filterWith {
      case Point(x, _) => x > 1
    }.reduceWith {
      case (Point(x1, y1), Point(x2, y2)) => Point(x1 + y1, x2 + y2)
    }.mapWith {
      case Point(x, y) => (x, y)
    }.flatMapWith {
      case (x, y) => Seq("x" -> x, "y" -> y)
    }.groupingBy {
      case (id, _) => id
    }.first(10).printToErr()
  }
}
