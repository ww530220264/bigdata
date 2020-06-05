package com.ww.flink.train

import java.sql.{Date, Timestamp}
import java.text.SimpleDateFormat

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.table.api.scala.BatchTableEnvironment
import org.apache.flink.api.scala._
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.table.api.{TableColumn, Tumble}
import org.apache.flink.table.api.scala._
import org.apache.flink.table.dataformat.SqlTimestamp
import org.apache.flink.table.descriptors.FileSystem
import org.apache.flink.table.functions.ScalarFunction

import scala.util.Random

object Tran_3_Table {
  def main(args: Array[String]): Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    val streamTable = StreamTableEnvironment.create(env)
    val timeToTimestamp = new TimeToTimestamp
    val table = streamTable.fromDataStream(
      env.fromElements(
        (4, "2020-05-31 09:17:02", 305), (1, "2020-05-23 16:15:51", 323), (4, "2020-05-30 06:04:41", 89),
        (1, "2020-05-29 19:24:10", 180), (2, "2020-06-01 13:45:32", 187), (1, "2020-05-24 08:49:54", 372),
        (3, "2020-06-02 20:57:58", 484), (3, "2020-05-24 21:30:21", 200), (1, "2020-05-21 08:49:54", 272),
        (3, "2020-05-25 09:57:26", 90), (1, "2020-05-26 04:46:12", 440), (2, "2020-05-27 22:06:08", 499),
        (3, "2020-05-29 11:40:04", 248), (2, "2020-05-31 19:28:47", 155), (2, "2020-05-27 22:10:02", 329),
        (2, "2020-06-01 07:20:30", 115), (2, "2020-05-23 09:13:12", 315), (4, "2020-05-25 15:51:55", 436),
        (4, "2020-05-24 02:40:01", 25), (1, "2020-05-30 05:38:32", 432), (1, "2020-05-22 19:52:39", 347)
      )
        .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[(Int, String, Int)](Time.seconds(3)) {

          override def extractTimestamp(element: (Int, String, Int)): Long = {
            val eventTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(element._2).getTime
            eventTime
          }
        }
        ).map((x=>(x._1,Timestamp.valueOf(x._2),x._3)))
      , 'accountId, 'timestamp, 'amount
    )

    val t1 = table.select('accountId,'timestamp as 'w,'amount)
//    t1.window(Tumble over( 1.minute on ))
    t1.getSchema.getTableColumns.toArray.foreach(println)
        val t2 = t1.window(Tumble over 1.hour on 'w as 'w1)
          .groupBy('accountId, 'w1)
          .select('accountId, 'w1.start as 'timestamp, 'amount.sum)
    streamTable.toAppendStream[(Int, Timestamp, Int)](t2).printToErr()

    env.execute()

    //    val env = ExecutionEnvironment.getExecutionEnvironment
    //    val batchTable = BatchTableEnvironment.create(env)
    //    val table = batchTable.fromDataSet(
    //      env.fromElements(
    //        (4, "2020-05-31 09:17:02", 305), (1, "2020-05-23 16:15:51", 323), (4, "2020-05-30 06:04:41", 89),
    //        (1, "2020-05-29 19:24:10", 180), (2, "2020-06-01 13:45:32", 187), (1, "2020-05-24 08:49:54", 372),
    //        (3, "2020-06-02 20:57:58", 484), (3, "2020-05-24 21:30:21", 200), (1, "2020-05-21 08:49:54", 272),
    //        (3, "2020-05-25 09:57:26", 90), (1, "2020-05-26 04:46:12", 440), (2, "2020-05-27 22:06:08", 499),
    //        (3, "2020-05-29 11:40:04", 248), (2, "2020-05-31 19:28:47", 155), (2, "2020-05-27 22:10:02", 329),
    //        (2, "2020-06-01 07:20:30", 115), (2, "2020-05-23 09:13:12", 315), (4, "2020-05-25 15:51:55", 436),
    //        (4, "2020-05-24 02:40:01", 25), (1, "2020-05-30 05:38:32", 432), (1, "2020-05-22 19:52:39", 347)
    //      ), 'accountId, 'timestamp, 'amount
    //    )
    //    val table2 = table.select("accountId, timestamp, amount")
    //      .orderBy('accountId.desc)
    //    //      .fetch(10)
    //    batchTable.toDataSet[(Int, String, Int)](table2).printToErr()
    //
    //    val timeToHour = new TimeToHour
    //    val table3 = table.select('accountId, timeToHour('timestamp) as 'timestamp, 'amount)
    //      .groupBy('accountId, 'timestamp)
    //      .select('accountId,'timestamp,'amount.sum).orderBy('accountId.asc,'timestamp.asc)
    //    batchTable.toDataSet[(Int, String, Int)](table3).printToErr()
    //
    //    env.execute()


    //    for (i <- 1 to 20) {
    //      val d = new Date().getTime - Random.nextInt(999999999)
    //      val smt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(d))
    //      System.err.println("(" + (Random.nextInt(4) + 1) + ",\"" + smt + "\"," + Random.nextInt(500) + ")")
    //    }
  }

  class TimeToHour extends ScalarFunction {
    def eval(time: String): String = {
      time.substring(0, 13) + ":00:00"
    }
  }

  class TimeToTimestamp extends ScalarFunction {
    def eval(time: String) {
      SqlTimestamp.fromEpochMillis(Timestamp.valueOf(time).getTime)

    }

    override def getResultType(signature: Array[Class[_]]): TypeInformation[_] = {
      Types.SQL_TIMESTAMP
    }
  }

}
