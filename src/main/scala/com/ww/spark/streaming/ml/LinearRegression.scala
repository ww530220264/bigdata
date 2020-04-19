//package org.ww.spark.streaming.ml
//
//
//import java.io.File
//
//import breeze.linalg.DenseVector
//import org.apache.spark.SparkConf
//import org.apache.spark.mllib.linalg.Vectors
//import org.apache.spark.mllib.regression.{LabeledPoint, StreamingLinearRegressionWithSGD}
//import org.apache.spark.storage.StorageLevel
//import org.apache.spark.streaming.{Seconds, StreamingContext}
//
//import scala.reflect.io.Directory
//
///**
// * Spark Streaming 还可以做在线机器学习工作
// * 目前支持Streaming Linear Regression,Streaming KMeans
// */
//object LinearRegression {
//  def main(args: Array[String]): Unit = {
//    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("Streaming Linear Regression")
//    val ssc = new StreamingContext(sparkConf, Seconds(10))
//    val stream = ssc.socketTextStream("master", 6666, StorageLevel.MEMORY_AND_DISK_2)
////        val stream = ssc.textFileStream(new Directory(new File("E:\\streaming_dir\\")).path)
//    val numFeatures = 2
//    val zeroVector = DenseVector.zeros[Double](numFeatures)
//    val model = new StreamingLinearRegressionWithSGD()
//      .setInitialWeights(Vectors.dense(zeroVector.data))//初始化权重
//      .setNumIterations(20)//迭代次数
//      .setRegParam(0.8)//正则化参数
//      .setStepSize(0.01)//学习步进
//
//    val labeledStream = stream.map { line =>
//      val split = line.split(",")
//      val y = split(numFeatures).toDouble
//      val features = split.slice(0, numFeatures).map(_.toDouble)
//      LabeledPoint(label = y, features = Vectors.dense(features))
//    }
//    model.trainOn(labeledStream)
//    model.predictOnValues(labeledStream.map(lp => (lp.label, lp.features))).print()
//
//    ssc.start()
//    ssc.awaitTermination()
//  }
//}
