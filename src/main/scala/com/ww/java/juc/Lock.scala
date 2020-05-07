package com.ww.java.juc

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

import org.openjdk.jol.info.ClassLayout

object Lock {
  def main(args: Array[String]): Unit = {
    //    _cas
    _java_obj_layout
  }

  def _java_obj_layout = {
    /**
     * 对象内存布局(可以使用工具包JOL):
     * markword:(8个字节)
               * 锁信息:
                     * 偏向锁
                     * 轻量级锁
                     * 重量级锁
               * gc信息[分代年龄]
               * hashcode
     * class pointer[指向T.class]（4个字节）
     * instance data[实例数据|成员数据]
     * byte for alignment(用作字节对齐,每8个字节)
     */
    Thread.sleep(5000)

    val o = new Object()
    System.err.println(ClassLayout.parseInstance(o).toPrintable)


    o.synchronized{
      System.err.println(ClassLayout.parseInstance(o).toPrintable)
    }

//    case class People(name: String)
//    val p = People("hello")
//    System.err.println(ClassLayout.parseInstance(p).toPrintable)
  }

  /**
   *  CAS
      * 底层实现指令: Lock cmpxchg(compare and exchange)
      * 加lock的原因是,保证compare和exchange两个操作是原子的,
      * lock执行后面指令的时候锁定一个北桥信号(而不采用锁总线)(在对一块内存做compareAndSet操作的时候)，会对这块内存上锁,不允许别的CPU打断
   *  用户态和内核态
      * 内核能访问所有的操作系统指令
      * 用户态只能访问能访问的执行
      * Intel指令分为ring0--ring3级，Linux内核工作在ring0级，能访问所有的指令，Linux用户态工作在ring3级，某些直接的指令不能访问
   *  JVM
      * JVM工作在用户态，锁的资源需要通过操作系统才能申请到（申请资源必须通过kernal（内核态），系统调用（0X80指令），所以早期JDK的synchronized是重量级锁）
      * 现在对synchronized做了一些优化，在某些状态下，不需要向操作系统申请资源，通过用户空间就能解决问题
   * 对象内存布局(可以使用工具包JOL):
      * markword:(8个字节)
        * 锁信息:
          * 偏向锁
          * 轻量级锁
          * 重量级锁
        * gc信息[分代年龄]
        * hashcode
      * class pointer[指向T.class]（4个字节）
      * instance data[实例数据|成员数据]
      * byte for alignment(用作字节对齐,每8个字节)
   *  给对象上锁，就是修改对象的markword
   * 锁升级过程
      *
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
