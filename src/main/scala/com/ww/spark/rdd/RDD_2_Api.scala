package com.ww.spark.rdd

import org.apache.spark.sql.SparkSession

object RDD_2_Api {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder.appName("app_API").master("local[3]").getOrCreate()
    task_1(spark)
  }

  /**
   * 给交易最多的顾客送一个熊娃娃
   * 购买两个或更多芭比娃娃购物中心玩具可享受5%的折扣
   * 为购买五本以上的词典添加牙刷
   * 给花了最多钱的顾客送一条睡衣
   *
   * @param spark
   */
  def task_1(spark: SparkSession): Unit = {
    val fileRdd = spark.sparkContext.textFile("file:///E:\\sourcecode\\spark\\spark-2.4.4\\examples\\src\\main\\resources\\my\\github\\ch04\\ch04_data_transactions.txt")
    val customRdd = fileRdd.map(_.split("#")).keyBy(_ (2).toInt)
    customRdd.cache()
    println("总交易数：" + customRdd.count())
    // 每个用户购买过哪些商品
    customRdd.aggregateByKey(List[String]())(
      (prods, tran) => prods ::: List(tran(3)),
      (prod1, prod2) => prod1 ::: prod2)
      .sortByKey()
      .collect()
      .foreach(println)
    val maxTrasns = customRdd.countByKey().toSeq.sortBy(_._2).last
    println(s"顾客${maxTrasns._1}的交易次数最多，交易次数为${maxTrasns._2}")
    println("他的交易清单如下：")
    customRdd.lookup(maxTrasns._1).map(_.mkString(",")).foreach(println)
    // 给交易最多的顾客送一个熊娃娃
    var freeArr = Array(Array("2015-03-30", "11:59 AM", maxTrasns._1.toString, "4", "1", "0.00"))
    // 购买两个或更多芭比娃娃购物中心玩具可享受5%的折扣
    val count_1 = customRdd.filter(kv => kv._2(3).toInt == 25 && kv._2(4).toInt > 1).count()
    println(s"统计：购买两个或更多芭比娃娃购物中心玩具的交易有${count_1}笔")
    val accumu_1 = spark.sparkContext.longAccumulator("count_1")
    val mapValueRdd = customRdd.mapValues(tran => {
      if (tran(3).toInt == 25 && tran(4).toInt > 1) {
        tran(5) = (tran(5).toDouble * 0.95).toString
        accumu_1.add(1)
      }
    })
    println(mapValueRdd.count())
    println(s"累加器：购买两个或更多芭比娃娃购物中心玩具的交易有${accumu_1.value}笔")
    // 为购买五本以上的词典添加牙刷
    val newCustomRdd = customRdd.flatMapValues(tran => {
      if (tran(3).toInt == 81 && tran(4).toDouble >= 5) {
        val cloned = tran.clone()
        cloned(5) = "0.00"
        cloned(3) = "70"
        cloned(4) = "1"
        List(tran, cloned)
      } else {
        List(tran)
      }
    })
    // 给花了最多钱的顾客送一条睡衣
    val costMax = customRdd
      .map(tran => (tran._1, tran._2(5).toDouble))
      .reduceByKey(_ + _)
      .collect.toSeq.sortBy(_._2).last

    println(s"花费最多的顾客为${costMax._1},费用为${costMax._2}")
    freeArr = freeArr :+ Array("2015-03-30", "11:59 AM", costMax._1.toString, "63", "1", "0.00")
    newCustomRdd.union(spark.sparkContext.parallelize(freeArr).map(tran => (tran(2).toInt, tran)))
      .map(tran => tran._2.mkString("#"))
      .saveAsTextFile("file:///E:\\sourcecode\\spark\\spark-2.4.4\\examples\\src\\main\\resources\\my\\github\\ch04\\ch04_data_transactions_back.txt")
  }
}
