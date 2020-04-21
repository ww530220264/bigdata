package com.ww.flink.note

import java.util.concurrent._

import org.apache.flink.api.common.JobID
import org.apache.flink.api.common.state.{MapStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.queryablestate.client.QueryableStateClient
import org.apache.flink.queryablestate.exceptions.UnknownKeyOrNamespaceException
import org.apache.flink.runtime.query.UnknownKvStateLocation


object STREAMING_4_Query_state {

  def main(args: Array[String]): Unit = {
    _query_state("68a04588abb8f3f60e1c3f08ad45d3c1", "query-name")
  }

  def _query_state(jobId: String, queryName: String): Unit = {
    //  JobID
    val jobID = JobID.fromHexString(jobId)
    //  查询的Key 比如某个UserID
    val key: Long = 7L
    //  状态查询客户端
    var client = new QueryableStateClient("localhost", 9069)
    //  查询对象描述
    val state_map = new MapStateDescriptor[String, Long]("state_map", classOf[String], classOf[Long])
    //  周期执行线程
    val scheduledExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    scheduledExecutor.scheduleWithFixedDelay(new Runnable {
      override def run(): Unit = {
        try {
          val future = client.getKvState(
            jobID,
            queryName,
            key,
            TypeInformation.of(classOf[Long]),
            state_map
          )
          //  获取结果
          val iter = future.get(2, TimeUnit.SECONDS).iterator()
          while (iter.hasNext) {
            val value = iter.next()
            System.err.println(value.getKey + "\t" + value.getValue)
          }
          System.err.println(Thread.currentThread.getName() + "---------------")
        } catch {
          case ex: Exception => {
            ex.getCause match {
              case e1: UnknownKeyOrNamespaceException => {
                e1.printStackTrace()
                Thread.sleep(1000)
                System.err.println("查询的指标暂时没有值....")
              }
              case e2: UnknownKvStateLocation => {
                e2.printStackTrace()
                System.err.println("链接异常,3秒后重建链接客户端....")
                Thread.sleep(3000)
                client = new QueryableStateClient("localhost", 9069)
              }
              case e3: RuntimeException => {
                e3.printStackTrace()
                Thread.sleep(5000)
                System.err.println("查询链接异常,请检查相关服务运行状态!")
              }
              case _: Exception => {
                Thread.sleep(5000)
                System.err.println("系统异常,请联系管理员!")
              }
            }
          }
        }
      }
    }
      , 1
      , 1L
      , TimeUnit.SECONDS
    )
  }

}
