package com.ww.spark.rdd

import org.apache.spark.HashPartitioner
import org.apache.spark.sql.SparkSession

import scala.collection.mutable.ArrayBuffer

object Spark_3_Partition {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder
      .appName("app_3")
      .master("local[3]")
      .getOrCreate
    task_groupByKeyAndSortValues(spark)
  }

  def task_groupByKeyAndSortValues(spark: SparkSession): Unit = {
    val rdd = spark.sparkContext.parallelize(Seq(
      (1, 2), (1, 1), (1, 3), (2, 2), (2, 1), (2, 3), (3, 2), (3, 1), (3, 3)
    ), 3)
    val rdd2 = rdd.map((_, null))
    val sortedRdd = rdd2.repartitionAndSortWithinPartitions(new MyPartitioner(3))
    sortedRdd.mapPartitionsWithIndex((index, x) => {
      val result = new ArrayBuffer[(Int, Int)]()
      x.foreach(ele => {
        println(index + "---" + ele._1)
        result.append(ele._1)
      })
      result.iterator
    }).collect
  }
}

import org.apache.spark.Partitioner
// 自定义分区器
class MyPartitioner(partitions: Int) extends Partitioner {
  override def numPartitions: Int = partitions

  override def getPartition(key: Any): Int = {
    val k = key.asInstanceOf[(Int,Int)]
    k._1.hashCode() % partitions
  }
}
