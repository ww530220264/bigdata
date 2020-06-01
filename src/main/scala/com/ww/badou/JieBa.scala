//package com.ww.badou
//
//import com.huaban.analysis.jieba.{JiebaSegmenter, SegToken}
//import com.huaban.analysis.jieba.JiebaSegmenter.SegMode
//import org.apache.spark.SparkConf
//import org.apache.spark.sql.SparkSession
//import org.apache.spark.sql.functions._
//
//object JieBa {
//
//  def main(args: Array[String]): Unit = {
//
//    val conf = new SparkConf().registerKryoClasses(Array(classOf[JiebaSegmenter]))
//
//    val spark = SparkSession.builder()
//      .appName("结巴分词")
//      .enableHiveSupport()
//      .config(conf)
//      .getOrCreate()
//
//    val df = spark.sql("select * from badou.newss")
//
//    val segmenter = new JiebaSegmenter()
//    val broadcastSeg = spark.sparkContext.broadcast(segmenter)
//
//    val jiebaUDF = udf { sentence: String =>
//      val exeSegmenter = broadcastSeg.value
//      exeSegmenter.process(sentence.toString, SegMode.INDEX)
//        .toArray().map(_.asInstanceOf[SegToken].word)
//        .filter(_.length > 1).mkString("/")
//    }
//    val df_seg = df.withColumn("seg", jiebaUDF(col("line")))
//    df_seg.show(50)
//  }
//}
