package com.ww.kafka

import java.util.Properties
import java.util.concurrent.CountDownLatch

import org.apache.kafka.clients.producer.{Callback, KafkaProducer, ProducerRecord, RecordMetadata}


object MyProducer {

  val config = new Properties()
  config.put("bootstrap.servers", "centos7-1:9092")
  config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  config.put("acks", "all")
  val producer: KafkaProducer[String, String] = new KafkaProducer(config)

  def main(args: Array[String]): Unit = {
    println(producer)
    //    send_sync()
    //    send_async()
    putOrders
  }

  def putOrders: Unit = {
    import scala.io.Source.fromFile
    val lines = fromFile("E:\\workspace\\bigdata\\src\\main\\resources\\data\\orders.txt").getLines()
    var count = 0
    for (line <- lines) {
      Thread.sleep(scala.util.Random.nextInt(5000))
      producer.send(new ProducerRecord[String, String]("orders", line.trim))
      count = count + 1
    }
    Thread.sleep(10000)
    System.err.println(count)
  }

  def fireAndForgot(): Unit = {

  }

  def send_sync(): Unit = {
    try {
      val start = System.currentTimeMillis()
      for (i <- 0 to 2000) {
        producer
          .send(new ProducerRecord[String, String]("test", "v1"))
          .get() // Future.get 同步阻塞获取服务器响应
      }
      System.err.println("消息发送成功！ 耗时：" + (System.currentTimeMillis() - start) / 2000)
    } catch {
      case ex: Exception => {
        ex.printStackTrace()
      }
    }

  }

  // 使用回调处理异步发送时可能出现的异常
  def send_async(): Unit = {
    val num = 1
    val countDownLatch = new CountDownLatch(num)
    try {
      val start = System.currentTimeMillis()
      var count = 0;
      for (i <- 0 until num) {
        //        Thread.sleep(scala.util.Random.nextInt(30))
        producer
          .send(new ProducerRecord[String, String]("test", "v111"), new ProducerCallback)
      }
      System.err.println("消息发送成功！ 耗时：" + (System.currentTimeMillis() - start))

      //      countDownLatch.await()
      Thread.sleep(3000)
      System.err.println(s"发送消息完毕：${count}")
    }
  }
}

class ProducerCallback() extends Callback {
  override def onCompletion(recordMetadata: RecordMetadata, e: Exception): Unit = {
    println(Thread.currentThread() + "----")
    if (e != null) {
      e.printStackTrace()
      System.err.println("发送失败")
    } else {
      System.err.println(recordMetadata)
    }
  }
}

