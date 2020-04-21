package com.ww.flink.note

import org.apache.flink.api.scala.{ExecutionEnvironment, _}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.types.Row

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
      "W:\\workspace\\bigdata\\src\\main\\scala\\com\\ww\\data\\bank.csv"
    ).filter(!_.startsWith("\"age\""))
      .map(_.replace("\"", ""))
      .map(_.split(";"))
      .map(x => Person(x(0).toInt, x(1), x(2)))

    //创建tableEnv
    val fbEnv = ExecutionEnvironment.getExecutionEnvironment
    val tableEnv = BatchTableEnvironment.create(fbEnv)
    //    tableEnv.fromDataSet(dataset,ArrayElement)
    //    tableEnv.createTemporaryView("aaa",dataset)
    ////    tableEnv.createTemporaryView[String,String]("aa",dataset,"age","job")
    val table = tableEnv.fromDataSet(dataset)
    tableEnv
      .toDataSet[Row](table.select("age,job,marital").where("age > 30").orderBy("age").fetch(20))
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
