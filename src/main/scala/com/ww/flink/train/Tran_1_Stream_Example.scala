package com.ww.flink.train

import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.scala._
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.util.Collector

object Tran_1_Stream_Example {
  val env = StreamExecutionEnvironment.getExecutionEnvironment

  case class Person(name: String, age: Int)

  def main(args: Array[String]): Unit = {
    //    val persons = env.fromElements(Person("Fred", 35), Person("Wilma", 35), Person("Pebbles", 2))
    //    task_1(env)
    //    keyBy(env)
    //    connect(env)
    //    minAndMinBy(env)
    //    window(env)
    reduce(env)
  }

  def reduce(env: StreamExecutionEnvironment): Unit = {
    val eles = env.fromElements(1, 2, 3, 4, 5, 1, 3, 1, 1, 1, 2, 4, 5)
    System.err.println(env.getParallelism + "-------------------")
    val keyedStream = eles.keyBy(x => x)
    System.err.println(keyedStream.parallelism + "-------------------")
//    keyedStream.printToErr()
    println("--------------------")
    keyedStream.reduce(_+_).printToErr()
    env.execute()
  }

  def window(env: StreamExecutionEnvironment): Unit = {
    env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)
    env.socketTextStream("centos7-1", 9012)
      .map(x => {
        val strings = x.split(" ")
        (strings(0), strings(1).toInt)
      })
      .keyBy(0).window(TumblingEventTimeWindows.of(Time.seconds(5)))
      .sum(1)
      .printToErr()

    env.execute()
  }

  def minAndMinBy(env: StreamExecutionEnvironment): Unit = {
    val eles = env.fromElements(("a", 1, 3), ("a", 0, 1), ("b", 3, 2), ("b", 2, 1), ("a", 3, 0), ("b", 4, 3))
    //    eles.keyBy(0).min(1).printToErr()
    //    eles.keyBy(0).minBy(1).printToErr()
    env.fromElements(("a", 3), ("a", 1), ("b", 2), ("a", 3), ("b", 4), ("c", 4), ("d", 5))
      .keyBy(x => x._1).reduce((x, y) => (x._1, x._2 + y._2))
      .printToErr()

    //    env.fromElements(("a", 1), ("b", 2), ("a", 3), ("b", 4), ("c", 4), ("d", 5))
    //      .keyBy(x => x._1).sum(1)
    //      .printToErr()
    env.execute()
  }

  def connect(env: StreamExecutionEnvironment): Unit = {
    val eles1 = env.fromElements("DROP", "IGNORE", "HELLO", "DROP").keyBy(x => x)
    val eles2 = env.fromElements("Apache", "DROP", "Flink", "IGNORE").keyBy(x => x)
    eles1.connect(eles2).flatMap(new RichCoFlatMapFunction[String, String, String] {
      var valueState: ValueState[Boolean] = _

      override def open(parameters: Configuration): Unit = {
        valueState = getRuntimeContext.getState(new ValueStateDescriptor[Boolean]("valueState", classOf[Boolean]))
      }

      override def flatMap1(value: String, out: Collector[String]): Unit = {
        valueState.update(true)
      }

      override def flatMap2(value: String, out: Collector[String]): Unit = {
        if (valueState.value() == null) {
          out.collect(value)
        }
      }
    }).printToErr()

    env.execute("connect")
  }

  def keyBy(env: StreamExecutionEnvironment): Unit = {
    val eles = env.fromElements(("a", 1), ("b", 2), ("a", 3), ("b", 4), ("c", 4), ("d", 5))
    eles.keyBy(eles => eles._1).flatMap(new RichFlatMapFunction[(String, Int), (String, Int)] {
      // 消除重复的key
      var valueState: ValueState[Boolean] = _

      override def open(parameters: Configuration): Unit = {
        val desc = new ValueStateDescriptor[Boolean]("value_state", classOf[Boolean])
        valueState = getRuntimeContext.getState(desc)
        System.err.println("open---------")
      }

      override def flatMap(value: (String, Int), out: Collector[(String, Int)]): Unit = {
        System.err.println("flatmap---------")
        if (valueState.value() == null) {
          out.collect(value)
          valueState.update(true)
        }
      }
    }).printToErr()
    //8> (a,1)
    //3> (b,2)
    //6> (c,4)
    //7> (d,5)
    env.execute("keyBy")
  }

  def task_1(env: StreamExecutionEnvironment): Unit = {
    val personList = List(Person("Fred", 35), Person("Wilma", 35), Person("Pebbles", 2))
    val persons = env.fromCollection(personList)
    val adults = persons.filter(_.age >= 18)
    adults.printToErr()
    env.execute("Tran_1")
  }
}
