package com.ww.flink.batch

import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.api.scala._

object Flink_WordCount {

  def main(args: Array[String]): Unit = {
    val env = ExecutionEnvironment.getExecutionEnvironment
    val path: String = "E:\\workspace\\bigdata\\src\\main\\resources\\words.txt"
    val output: String = "output_wordcount"
    val text = env.readTextFile(path)
    val counts = text.flatMap { x => x.toLowerCase.split("\\s+") }
      .map {
        (_, 1)
      }.groupBy(0).sum(1).setParallelism(2)

    counts.writeAsCsv(output, "\n", "->>")

    env.execute("Word Count")
  }
}
