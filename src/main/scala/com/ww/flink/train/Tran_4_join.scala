package com.ww.flink.train

import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.api.scala._
import org.apache.flink.util.Collector

object Tran_4_join {
  def main(args: Array[String]): Unit = {
    //    joinDataSet
//    coGroupDataSet
    crossDataSet
  }

  def crossDataSet(): Unit ={
    val env = ExecutionEnvironment.getExecutionEnvironment
    val ele1 = env.fromElements(("a", 1), ("b", 2), ("c", 3))
    val ele2 = env.fromElements(("b", 1), ("c", 2), ("c", 3), ("d", 3))

    ele1.cross(ele2).printToErr()
  }

  def coGroupDataSet(): Unit = {
    val env = ExecutionEnvironment.getExecutionEnvironment
    val ele1 = env.fromElements(("a", 1), ("b", 2), ("c", 3))
    val ele2 = env.fromElements(("b", 1), ("c", 2), ("c", 3), ("d", 3))
    ele1.coGroup(ele2).where(0).equalTo(0) {
      (first, second, out: Collector[String]) => {
        val fs = first.toList
        val ss = second.toList
        for (f <- fs) {
          for (s <- ss) {
            out.collect(f + "..." + s)
          }
        }
      }
    }.printToErr()
  }

  def joinDataSet(): Unit = {
    val env = ExecutionEnvironment.getExecutionEnvironment
    val ele1 = env.fromElements(("a", 1), ("b", 2), ("c", 3))
    val ele2 = env.fromElements(("b", 1), ("c", 2), ("c", 3), ("d", 3))
    ele1.join(ele2).where(0).equalTo(0) {
      (first, second) => {
        first + "....." + second
      }
    }.printToErr()

    ele1.leftOuterJoin(ele2).where(0).equalTo(0) {
      (first, second) => {
        first + "....." + second
      }
    }.printToErr()

    ele1.rightOuterJoin(ele2).where(0).equalTo(0) {
      (first, second) => {
        first + "....." + second
      }
    }.printToErr()

    ele1.fullOuterJoin(ele2).where(0).equalTo(0) {
      (first, second) => {
        first + "....." + second
      }
    }.printToErr()
  }
}
