package com.ww.spark.streaming

import java.sql.Timestamp
import java.text.SimpleDateFormat

import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010._

object SparkStreaming_Kafka {
  def main(args: Array[String]): Unit = {
    val kafkaParams = Map[String, String](
      "bootstrap.servers" -> "centos7-1:9092",
      "group.id" -> "spark_streaming_3",
      "auto.offset.reset" -> "earliest",
      "key.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "value.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer"
    )
    val spark = SparkSession
      .builder
      .master("local[5]")
      .appName("SparkStreaming_Kafka")
      .getOrCreate()
    val ssc = new StreamingContext(spark.sparkContext, Seconds(5))
    ssc.sparkContext.setLogLevel("INFO")
    ssc.checkpoint("../checkpoint/streaming_kafka_orders")

    val kafkaStream = KafkaUtils.createDirectStream[String, String](
      ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](Set("orders"), kafkaParams))

    case class Order(time: Timestamp, orderId: Long, clientId: Long,
                     symbol: String, amount: Int, price: Double, buy: Boolean)
    val orders = kafkaStream.flatMap(line => {
      val dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
      val s = line.value().split(",")
      System.err.println(Thread.currentThread().getName + "---" + line.value())
      try {
        assert(s(6) == "B" || s(6) == "S")
        List(Order(new Timestamp(dateFormat.parse(s(0)).getTime), s(1).toLong,
          s(2).toLong, s(3), s(4).toInt, s(5).toDouble, s(6) == "B"))
      } catch {
        case e: Throwable => System.err.println("Wrong line format (" + e + "): " + line.value())
          List()
      }
    })
    val numberType = orders.map(o => (o.buy, 1L)).reduceByKey(_ + _)
    val buySellList = numberType.map(t => {
      if (t._1) ("Buys", List(t._2.toString))
      else ("Sells", List(t._2.toString))
    })
    val amountPerClient = orders.map(o => (o.orderId, o.amount * o.price))
    val amountState = amountPerClient.updateStateByKey((vals, total: Option[Double]) => {
      total match {
        case Some(total) => Some(vals.sum + total)
        case None => Some(vals.sum)
      }
    })
    val top5Client = amountState.transform(_.sortBy(_._2, false).map(_._1)
      .zipWithIndex().filter(_._2 < 5))
    val top5List = top5Client.repartition(1)
      .map(x => x._1.toString)
      .glom()
      .map(arr => ("TOP5CLIENTS", arr.toList))
    val stocksPerWindow = orders.map(x => (x.symbol, x.amount))
      .reduceByKeyAndWindow((a1, a2) => a1 + a2, Minutes(60))
    val topStocks = stocksPerWindow.transform(_.sortBy(_._2, false).map(_._1)
      .zipWithIndex().filter(_._2 < 5))
      .repartition(1)
      .map(x => x._1.toString)
      .glom()
      .map(arr => ("TOP5STOCKS", arr.toList))
    val finalStream = buySellList.union(top5List).union(topStocks).print()

    ssc.start()
    ssc.awaitTermination()
  }

}
