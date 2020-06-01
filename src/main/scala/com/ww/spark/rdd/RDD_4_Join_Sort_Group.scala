package com.ww.spark.rdd

import org.apache.spark.sql.SparkSession

object RDD_4_Join_Sort_Group {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .appName("app_4")
      .master("local[3]")
      .getOrCreate()
        task_1(spark)
//    task_2(spark)
  }

  def task_2(spark: SparkSession): Unit = {
    val rdd1 = spark.sparkContext.parallelize(1 to 10, 10)
    val rdd2 = spark.sparkContext.parallelize((1 to 18).map(x => "n" + x), 10)
    //
    val rdd3 = rdd1.zipPartitions(rdd2, true)((iter1, iter2) => {
      iter1.zipAll(iter2, -1, "empty").map({
        case (x1, x2) => x1 + "-" + x2
      })
    })
    rdd3.collect.foreach(println(_))
  }

  def task_1(spark: SparkSession): Unit = {
    // 每个商品ID及对应的销售额
    val primaryRdd = spark.sparkContext.textFile("file:///E:\\sourcecode\\spark\\spark-2.4.4\\examples\\src\\main\\resources\\my\\github\\ch04\\ch04_data_transactions.txt")
      .map(_.split("#"))

    val tranRdd = primaryRdd.keyBy(_ (3).toInt)
      //  .map(x => (x(3).toInt, x(5).toDouble)) //或者使用下面两行
      .mapValues(tran => tran(5).toDouble)
      .reduceByKey(_ + _)
    tranRdd.take(10).foreach(println)

    val prodRdd = spark.sparkContext.textFile("file:///E:\\sourcecode\\spark\\spark-2.4.4\\examples\\src\\main\\resources\\my\\github\\ch04\\ch04_data_products.txt")
      .map(_.split("#"))
      .map(x => (x(0).toInt, x))

    val joinRdd = tranRdd.join(prodRdd)
    joinRdd.take(10).foreach(t => println(s"商品ID：${t._1},商品名称：${t._2._2(1)},销售额：${t._2._1}"))
    println("没有被购买过的商品(left join)：")
    val missingProRdd = prodRdd.leftOuterJoin(tranRdd).filter(x => x._2._2 == None).collect.foreach(x => println(x._2._1.mkString(",")))
    println("没有被购买过的商品(subtractByKey)：")
    // 【差集 前者--后者】返回后者RDD的key没有在前者RDD的key中出现的前者RDD的元素
    val reduceProRdd = prodRdd.subtractByKey(tranRdd).collect.foreach(x => println(x._2.mkString(",")))
    println("没有被购买过的商品(cogroup)：")
    val coGroupRdd = prodRdd.cogroup(tranRdd).filter(x => x._2._2.isEmpty).collect.foreach(_._2._1.foreach(x => println(x.mkString(","))))
    // 购买的商品的单价的最小值，最大值，平均值，总的消费
    println("购买的商品的单价的最小值，最大值，平均值，总的消费(combineByKey)：")
    primaryRdd.keyBy(_ (2).toInt)
      .combineByKey(
        (tran: Array[String]) => {
          val quantity = tran(4).toInt
          val price = tran(5).toDouble
          (price / quantity, price / quantity, quantity, price)
        },
        (res: (Double, Double, Int, Double), tran: Array[String]) => {
          val quantity = tran(4).toInt
          val price = tran(5).toDouble
          (scala.math.min(res._1, price / quantity),
            scala.math.max(res._2, price / quantity),
            res._3 + quantity,
            res._4 + price
          )
        },
        (res1: (Double, Double, Int, Double), res2: (Double, Double, Int, Double)) => {
          (scala.math.min(res1._1, res2._1),
            scala.math.max(res1._2, res2._2),
            res1._3 + res2._3,
            res1._4 + res2._4
          )
        }
      ).filter(_._1 == 96)
      .map(t => (t._1,(t._2._1, t._2._2, t._2._4 / t._2._3,t._2._3, t._2._4)))
      .take(10).foreach(println)
  }

}
