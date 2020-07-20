package com.ww.多线程

import java.util.concurrent.Executors

object 基础 {

  def main(args: Array[String]): Unit = {
    val thread = getNewThread(() => {
      Thread.sleep(5000)
      println(Thread.currentThread().getName + "结束....")
    })

    /**
     * 如果设置为Daemon,那么JVM不等待守护线程,直接结束
     * 如果不设置Daemon,那么就是用户线程,用户线程会阻止JVM的正常停止
     */
    thread.setDaemon(true)
    thread.start()
    println("Main 线程结束")
  }

  def getNewThread(runFun: () => Unit): Thread = {
    new Thread(new Runnable {
      override def run(): Unit = {
        runFun()
      }
    })
  }

}
