package com.ww.spark.streaming

import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, ConsumerStrategies, HasOffsetRanges, KafkaUtils, LocationStrategies}
import org.apache.spark._

object SparkStreaming_Kafka {
  def main(args: Array[String]): Unit = {
    val kafkaParams = Map[String, String](
      "bootstrap.servers" -> "centos7-1:9092",
      "group.id" -> "spark_streaming_3",
      "auto.offset.reset" -> "latest",
      "key.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "value.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
      "auto.commit.enable" -> "false"
    )
    val ssc = new StreamingContext(new SparkConf()
//      .setMaster("local[3]")
            .setMaster("spark://wangwei:7077")
      .setAppName("test-kafka"), Seconds(10))
    ssc.sparkContext.setLogLevel("DEBUG")
    ssc.sparkContext.addJar("E:\\workspace\\bigdata\\target\\sumEndPoint.jar")

    def producerConfiguration: Properties = {
      val props = new Properties()
      props.put("bootstrap.servers", "centos7-1:9092")
      props.put("value.serializer", classOf[StringSerializer].getName)
      props.put("key.serializer", classOf[StringSerializer].getName)
      props.put("acks", "all")
      props
    }

    val kafkaStreams = KafkaUtils.createDirectStream(ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](Set("test"), kafkaParams))

    kafkaStreams.foreachRDD(rdd => {
      val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      rdd.map(x=>(x.value(),1)).foreachPartition(p => {
        val producer = new KafkaProducer[String, String](producerConfiguration)
        p.foreach({ case (k, v) =>
          producer.send(new ProducerRecord[String, String]("test1", null, k + "--->" + v))
        })
      })
      kafkaStreams.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)
    })
    ssc.start()
    ssc.awaitTermination()
    //    val spark = SparkSession
    //      .builder
    //      .master("local[2]")
    //      .appName("SparkStreaming_Kafka")
    //      .getOrCreate()
    //    val ssc = new StreamingContext(spark.sparkContext, Seconds(2))
    //    ssc.sparkContext.setLogLevel("DEBUG")
    //    ssc.sparkContext.addJar("E:\\workspace\\bigdata\\target\\sumEndPoint.jar")
    //    ssc.checkpoint("../checkpoint/streaming_kafka_orders")
    //    val lines = ssc.socketTextStream("centos7-1", 9999)
    //    lines.print()
    //    val kafkaStream = KafkaUtils.createDirectStream[String, String](
    //      ssc,
    //      LocationStrategies.PreferConsistent,
    //      ConsumerStrategies.Subscribe[String, String](Set("orders"), kafkaParams))
    //
    //    case class Order(time: Timestamp, orderId: Long, clientId: Long,
    //                     symbol: String, amount: Int, price: Double, buy: Boolean)
    //    val orders = kafkaStream.flatMap(line => {
    //      val dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    //      val s = line.value().split(",")
    //      System.err.println(Thread.currentThread().getName + "---" + line.value())
    //      try {
    //        assert(s(6) == "B" || s(6) == "S")
    //        List(Order(new Timestamp(dateFormat.parse(s(0)).getTime), s(1).toLong,
    //          s(2).toLong, s(3), s(4).toInt, s(5).toDouble, s(6) == "B"))
    //      } catch {
    //        case e: Throwable => System.err.println("Wrong line format (" + e + "): " + line.value())
    //          List()
    //      }
    //    })
    //    val numberType = orders.map(o => (o.buy, 1L)).reduceByKey(_ + _)
    //    val buySellList = numberType.map(t => {
    //      if (t._1) ("Buys", List(t._2.toString))
    //      else ("Sells", List(t._2.toString))
    //    })
    //    val amountPerClient = orders.map(o => (o.orderId, o.amount * o.price))
    //    val amountState = amountPerClient.updateStateByKey((vals, total: Option[Double]) => {
    //      total match {
    //        case Some(total) => Some(vals.sum + total)
    //        case None => Some(vals.sum)
    //      }
    //    })
    //    val top5Client = amountState.transform(_.sortBy(_._2, false).map(_._1)
    //      .zipWithIndex().filter(_._2 < 5))
    //    val top5List = top5Client.repartition(1)
    //      .map(x => x._1.toString)
    //      .glom()
    //      .map(arr => ("TOP5CLIENTS", arr.toList))
    //    val stocksPerWindow = orders.map(x => (x.symbol, x.amount))
    //      .reduceByKeyAndWindow((a1, a2) => a1 + a2, Minutes(60))
    //    val topStocks = stocksPerWindow.transform(_.sortBy(_._2, false).map(_._1)
    //      .zipWithIndex().filter(_._2 < 5))
    //      .repartition(1)
    //      .map(x => x._1.toString)
    //      .glom()
    //      .map(arr => ("TOP5STOCKS", arr.toList))
    //    val finalStream = buySellList.union(top5List).union(topStocks).print()

    //      ssc.start()
    //    ssc.awaitTermination()
  }

}
