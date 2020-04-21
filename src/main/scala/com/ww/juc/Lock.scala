package com.ww.juc

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

import org.openjdk.jol.info.ClassLayout

object Lock {
  def main(args: Array[String]): Unit = {
    //    _cas
    _synchronized
  }

  def _synchronized = {
    /**
     * 对象内存布局(可以使用工具包JOL):
     * markword:
               * 锁信息:
                     * 偏向锁
                     * 轻量级锁
                     * 重量级锁
               * gc信息[分代年龄]
               * hashcode
     * class pointer[指向T.class]
     * instance data[实例数据|成员数据]
     * byte for alignment(用作字节对齐,每8个字节)
     */
    val o = new Object()
    System.err.println(ClassLayout.parseInstance(o).toPrintable)

    case class People(name: String)
    val p = People("wangweiaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
    System.err.println(ClassLayout.parseInstance(p).toPrintable)

    case class People1(age: Long)
    val p1 = People1(123)
    System.err.println(ClassLayout.parseInstance(p1).toPrintable)


  }

  /**
   * CAS 底层实现指令: Lock cmpxchg(compare and exchange) 指令
   * 加lock的原因是,保证compare和exchange两个操作是原子的,lock执行后面指令的时候锁定一个北桥信号(而不采用锁总线),不允许别的CPU打断
   */
  def _cas = {
    val atomicInteger: AtomicInteger = new AtomicInteger(0)
    val threadArr = new Array[Thread](100)
    val countDown: CountDownLatch = new CountDownLatch(threadArr.length)
    for (i <- 1 to threadArr.length) {
      new Thread() {
        override def run(): Unit = {
          for (j <- 1 to 100) {
            atomicInteger.incrementAndGet()
          }
          countDown.countDown()
        }
      }.start()
    }
    countDown.await()
    System.err.println(atomicInteger)

  }
}
