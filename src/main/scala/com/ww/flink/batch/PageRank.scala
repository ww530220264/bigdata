package com.ww.flink.batch

import java.lang

import com.ww.utils.{PageRankData}
import org.apache.flink.api.common.functions.GroupReduceFunction
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala._
import org.apache.flink.util.Collector
import scala.collection.JavaConverters._
import org.apache.flink.api.java.aggregation.Aggregations.SUM

case class Link(sourceId: Long, targetId: Long)

case class Page(pageId: Long, rank: Double)

case class AdjacencyList(sourceId: Long, targetIds: Array[Long])

object PageRank {

  // 阻尼系数 防止网页排名收敛于0
  private final val DAMPENING_FACTOR: Double = 0.85
  // 阈值
  private final val EPSILON: Double = 0.0001

  def main(args: Array[String]): Unit = {
    val params = ParameterTool.fromArgs(args)
    //创建执行环境
    val env = ExecutionEnvironment.getExecutionEnvironment
    env.getConfig.setGlobalJobParameters(params)
    // （页面ID，页面数量）
    val (pages, numPages) = getPagesDataSet(env, params)
    // 页面之间的链接关系（source页面ID-->>目标页面ID）
    val links = getLinksDataSet(env, params)
    // 最大迭代次数
    val maxIterations = params.getInt("iterations", 10)
    // 给页面初始排名（pageId，rank）
    val pagesWithRanks = pages.map(p => Page(p, 1.0 / numPages)).withForwardedFields("*->pageId")
    // 获取边之间的连接关系（sourceId，targetIds-->sourceId所指向的所有的page的ID)
    val adjacencyLists = links.groupBy("sourceId").reduceGroup(
      new GroupReduceFunction[Link, AdjacencyList] {
        override def reduce(values: lang.Iterable[Link], out: Collector[AdjacencyList]): Unit = {
          var outputId = -1L
          val outputList = values.asScala map { t => outputId = t.sourceId; t.targetId }
          out.collect(AdjacencyList(outputId, outputList.toArray))
        }
      }
    )
    // 迭代
    val finalRanks = pagesWithRanks.iterateWithTermination(maxIterations) {
      currentRanks =>
        val newRanks = currentRanks.join(adjacencyLists).where("pageId").equalTo("sourceId") {
          // （当前页面，当前页面rank），（当前页面，当前页面指向的所有页面）
          (page, adjacent, out: Collector[Page]) =>
            val targets = adjacent.targetIds
            val len = targets.length // 出度
            // 产生的每个page的分数是指向他们的页面的分数/指向他们页面的出度
            adjacent.targetIds foreach { t => out.collect(Page(t, page.rank / len)) }
        }
          // 聚合 一个页面的最终的分数是由所有（指向这个页面的父页面分数/父页面出度）之和
          .groupBy("pageId").aggregate(SUM, "rank")
          // 使用阻尼系数防止网页分数收敛于0
          .map { p =>
            Page(p.pageId, (p.rank * DAMPENING_FACTOR) + ((1 - DAMPENING_FACTOR) / numPages))
          }.withForwardedFields("pageId")

        val termination = currentRanks.join(newRanks).where("pageId").equalTo("pageId") {
          (current, next, out: Collector[Int]) =>
            if (math.abs(current.rank - next.rank) > EPSILON) out.collect(1)
        }

        (newRanks, termination)
    }

    val result = finalRanks

    if (params.has("output")) {
      result.writeAsCsv(params.get("output"), "\n", " ")
      env.execute("Basic PageRank Example")
    } else {
      println("Printing result to stdout. Use --output to specify output path.")
      result.print()
    }
  }

  /**
   * 获取pages数据
   *
   * @param env
   * @param params
   * @return
   */
  private def getPagesDataSet(env: ExecutionEnvironment, params: ParameterTool): (DataSet[Long], Long)

  = {
    if (params.has("pages") && params.has("numPages")) {
      val pages = env
        .readCsvFile[Tuple1[Long]](params.get("pages"), fieldDelimiter = " ", lineDelimiter = "\n")
        .map(x => x._1)
      (pages, params.getLong("numPages"))
    } else {
      println("Execution PageRank example with default pages data set.")
      println("Use --pages and --numPages to specify file input")
      (env.generateSequence(1, 15), PageRankData.getNumberOfPages)
    }
  }

  /**
   * 获取links数据
   *
   * @param env
   * @param params
   * @return
   */
  private def getLinksDataSet(env: ExecutionEnvironment, params: ParameterTool): DataSet[Link]

  = {
    if (params.has("links")) {
      env.readCsvFile[Link](params.get("links"), fieldDelimiter = " ", includedFields = Array(0, 1))
    } else {
      println("Executing PageRank example with default links data set.")
      println("Use --links to specify file input.")
      val edges = PageRankData.EDGES.map {
        case Array(v1, v2) => Link(v1.asInstanceOf[Long], v2.asInstanceOf[Long])
      }
      env.fromCollection(edges)
    }
  }
}
