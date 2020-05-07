package com.ww.flink.note

import org.apache.flink.api.scala.{ExecutionEnvironment}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}
import org.apache.flink.table.api.scala.{BatchTableEnvironment, StreamTableEnvironment}
import org.apache.flink.types.Row

/**
 * Table API 使用(')符号来引用table的属性,Table使用scala的隐式转换,需要import如下两个地方的类库
 * import org.apache.flink.table.api.scala._
 * import org.apache.flink.api.scala._
 */
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala._
case class Person(age: Int, job: String, marital: String)

object TABLE_BATCH_1 {
  /**
   *  1.如果只有一个planner在/lib目录下,可以使用useAnyPlanner来创建一个指定的环境设置
   *  2.tables可以是虚表(VIEWS)或平常的表,VIEWS可以从一个已存在的table创建
   *  3.table描述了外部的数据,可以是一个文件/数据库或消息队列
   *  4.table分为临时表和永久表
   */

  def main(args: Array[String]): Unit = {
    _Batch_TO_table
  }

  def _Batch_TO_table = {
    //  数据加载/清洗
    val dataSetEnv = ExecutionEnvironment.getExecutionEnvironment
    val dataset = dataSetEnv.readTextFile(
      "E:\\workspace\\bigdata\\src\\main\\resources\\data\\bank.csv"
    ).filter(!_.startsWith("\"age\""))
      .map(_.replace("\"", ""))
      .map(_.split(";"))
      .map(x => Person(x(0).toInt, x(1), x(2)))

    //  创建tableEnv
    val fbEnv = ExecutionEnvironment.getExecutionEnvironment
    val tableEnv = BatchTableEnvironment.create(fbEnv)
    //  DataSet--> table
    val table = tableEnv.fromDataSet(dataset)
    val newTable = table.renameColumns('age as 'age1)
    tableEnv
      .toDataSet[Row](newTable.select("age1,job,marital").where("age1 > 30").orderBy("age1").fetch(20))
      .printToErr()
  }

  def _Stream_query = {
    val fsSettings = EnvironmentSettings
      .newInstance()
      .useOldPlanner()
      .inStreamingMode()
      .build()
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    val fsTableEnv = StreamTableEnvironment.create(fsEnv, fsSettings)
  }

  def Blink_stream_query = {
    val bsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    val bsSettings = EnvironmentSettings
      .newInstance()
      .useBlinkPlanner()
      .inStreamingMode()
      .build()
    val bsTableEnv = StreamTableEnvironment.create(bsEnv, bsSettings)
  }

  def _Batch_query = {
    val fbEnv = ExecutionEnvironment.getExecutionEnvironment
    val fbTableEnv = BatchTableEnvironment.create(fbEnv)
  }

  def _Blink_batch_query = {
    val bbSettings = EnvironmentSettings
      .newInstance()
      .useBlinkPlanner()
      .inBatchMode()
      .build()
    val bbTableEnv = TableEnvironment.create(bbSettings)
  }
}
