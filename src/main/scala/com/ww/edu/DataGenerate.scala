package com.ww.edu

import java.text.SimpleDateFormat
import java.util.{Date, Properties, Random}

import com.ww.kafka.ProducerCallback
import org.apache.kafka.clients.producer.{Callback, KafkaProducer, Producer, ProducerRecord, RecordMetadata}

object DataGenerate {
  /**
   * user_id 【学生ID】【1-10000000】
   * class_id 【班级ID】 【1-10】
   * paper_id 【试卷ID】【1-1000】
   * paper_class 【试卷科目】【1-6】 【1：数学、2：语文、3：英语、4：物理、5：化学、6：生物】
   * subject_id 【题目ID】 【1-100】
   * subject_type 【题目类型】【1-2】【1：主观题、2：客观题】
   * subject_category_id 【题目分类】 【1-100】
   * subject_score 【题目分数】 【1-15】
   * subject_answer 【作答结果】【0：错误 1：正确】
   * datetime 【作答时间】
   *
   * @param args
   */
  def main(args: Array[String]): Unit = {
    val smt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val now: Long = new Date().getTime
    val oneYear = 365 * 24 * 60 * 60 * 1000

    val config = new Properties()
    config.put("bootstrap.servers", "centos7-1:9092")
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    config.put("acks", "all") //所有的同步副本收到消息
    config.put("retries", Integer.MAX_VALUE.toString)
    config.put("linger.ms", "50")
    config.put("batch.size", "61440")
    config.put("buffer.memory", "33554432")
//    config.put("producer.type","async")
//    config.put("queue.buffering.max.mx","5000")
//    config.put("queue.buffering.max.messages","10000")
//    config.put("queue.enqueue.timeout.ms","-1")
//    config.put("batch.num.messages","200")
    config.put("request.timeout.ms","500000")
    config.put("max.block.ms","120000")
    config.put("block.on.buffer.full","true")
//    config.put("max.in.flight.requests.per.connection","1")//避免消息乱序

    val pruducer: Producer[String, String] = new KafkaProducer(config)

    for (i <- 1 to 3) {
      val rand = new Random()
      val user_id: Long = rand.nextInt(10000000) + 1
      val class_id: Int = rand.nextInt(10) + 1
      val paper_id: Long = rand.nextInt(1000) + 1
      val paper_class: Int = rand.nextInt(6) + 1
      val subject_id: Long = rand.nextInt(100) + 1
      val subject_type: Int = rand.nextInt(2) + 1
      val subject_category_id = rand.nextInt(100) + 1
      val subject_score: Int = rand.nextInt(15) + 1
      val subject_answer: Int = rand.nextInt(2)
      val datatime: String = smt.format(new Date(now - rand.nextInt(oneYear)))

      val record = s"${user_id},${class_id},${paper_id},${paper_class},${subject_id},${subject_type},${subject_category_id},${subject_score},${subject_answer},${datatime}"
      //      System.err.println(record)
      //      Thread.sleep(rand.nextInt(20))
//            val resp = pruducer.send(new ProducerRecord[String, String]("edu", record.trim))
//            val metadata = resp.get()
      pruducer.send(new ProducerRecord[String, String]("edu", record.trim), new Callback {
        override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
          if (exception != null) {
            System.err.println("发送失败：" + exception.getMessage)
          }
        }
      })
    }
    pruducer.close()
  }
}
