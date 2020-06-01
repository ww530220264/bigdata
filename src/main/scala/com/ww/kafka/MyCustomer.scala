package com.ww.kafka

import java.util.{Collections, Properties}

import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}

object MyCustomer {
  val config = new Properties()
  config.put("bootstrap.servers","centos7-1:9092")
  config.put("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer")
  config.put("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer")
  config.put("group.id","testGroup")
  config.put("auto.offset.reset","latest")
  config.put("client.id","1")
  val consumer = new KafkaConsumer[String,String](config)
  consumer.subscribe(Collections.singletonList("test"))

  def main(args: Array[String]): Unit = {
    var count = 0
    while (true){
      val records = consumer.poll(100)
//      System.err.println(consumer.assignment())
      import collection.JavaConversions._
      for (record <- records){
        System.err.println(s"topic:${record.topic()}," +
          s"partition:${record.partition()}," +
          s"offset:${record.offset()}," +
          s"key:${record.key()}," +
          s"value:${record.value()},")
        count = count + 1
      }
      System.err.println(s"读取到的总记录数：${count}")
    }
  }
}
