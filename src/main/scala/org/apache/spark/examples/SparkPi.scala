/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// scalastyle:off println
package org.apache.spark.examples

import org.apache.spark.sql.SparkSession

import scala.math.random

/** Computes an approximation to pi */
object SparkPi {
  def main(args: Array[String]) {
    val spark = SparkSession
      .builder
      .appName("Spark Pi")
      .master("spark://169.254.37.8:7077")
      .getOrCreate()
    //    sparkPi(spark,args)
//    sparkKeyBy(spark)
    spark.sparkContext.addJar("E:\\workspace\\bigdata\\target\\bigdata.jar")
    spark.sparkContext.parallelize(List("dog", "salmon", "salmon", "rat", "elephant"), 2)
      .keyBy(_.length)//根据函数生成key
      .reduceByKey(_ + "," + _)
      .collect()
      .foreach(kv => {
        println(kv._1 + "->" + kv._2)
      })
    spark.stop()
  }

  def sparkKeyBy(spark: SparkSession) = {
    spark.sparkContext.parallelize(List("dog", "salmon", "salmon", "rat", "elephant"), 2)
      .keyBy(_.length)//根据函数生成key
      .reduceByKey(_ + "," + _)
      .collect()
      .foreach(kv => {
        println(kv._1 + "->" + kv._2)
      })
  }

  def sparkPi(spark: SparkSession, args: Array[String]) = {
    val slices = if (args.length > 0) args(0).toInt else 2
    val n = math.min(100000L * slices, Int.MaxValue).toInt
    val count = spark.sparkContext.parallelize(1 until n, slices).map {
      i =>
        val x = random * 2 - 1
        val y = random * 2 - 1
        if (x * x + y * y <= 1) 1 else 0
    }.reduce(_ + _)
    println(s"Pi is roughly ${4.0 * count / (n - 1)}")
  }
}

// scalastyle:on println
