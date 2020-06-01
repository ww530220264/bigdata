package com.ww.kafka

import java.util
import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, Partitioner, ProducerRecord}
import org.apache.kafka.common.Cluster
import org.apache.kafka.common.record.InvalidRecordException
import org.apache.kafka.common.utils.Utils

object Partitioner{
  val config = new Properties()
  config.put("bootstrap.servers", "centos7-1:9092")
  config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  config.put("partitioner.class", "com.ww.kafka.Partitioner_1")
  val producer: KafkaProducer[String, String] = new KafkaProducer(config)

  def main(args: Array[String]): Unit = {
    producer.send(
      new ProducerRecord[String,String]("test","wangwei","v1")
    )
    producer.send(
      new ProducerRecord[String,String]("test","1","v1")
    )
    producer.send(
      new ProducerRecord[String,String]("test","2","v1")
    )
    producer.send(
      new ProducerRecord[String,String]("test","3","v1")
    )
    producer.send(
      new ProducerRecord[String,String]("test","4","v1")
    )
  }
}

class Partitioner_1 extends Partitioner{

  override def partition(topic: String, key: Any, keyBytes: Array[Byte],
                         value: Any, valueBytes: Array[Byte], cluster: Cluster): Int = {
    if (keyBytes == null || !(key.isInstanceOf[String])){
      throw new InvalidRecordException("我们需要字符串的值为key")
    }
    val partitions = cluster.partitionsForTopic(topic)
    val numPartitions = partitions.size()
    if (key.asInstanceOf[String].equals("wangwei")){
      System.err.println(numPartitions)
      numPartitions
    }else{
      val partition = (math.abs(Utils.murmur2(keyBytes))) % (numPartitions - 1)
      System.err.println(partition)
      partition
    }
  }

  override def close(): Unit = {
    System.err.println("close com.ww.kafka.Partitioner_1")
  }

  override def configure(configs: util.Map[String, _]): Unit = {
    System.err.println("configure com.ww.kafka.Partitioner_1")
  }
}
