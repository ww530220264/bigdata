package com.ww.多线程.保护性暂挂模式

import java.util.{Timer, TimerTask}
import java.util.concurrent.Callable
import java.util.concurrent.locks.{Condition, Lock, ReentrantLock}

class AlarmAgent {
  @volatile private var connectedToServer = false

  private val agentConnected = new Predicate() {
    override def evaluate(): Boolean = connectedToServer
  }

  private val blocker: Blocker = new ConditionBlocker()

  private val heartbeatTimer = new Timer(true)

  def sendAlarm(alarm: String): Unit = {
    val guardedAction = new GuardedAction[Unit](agentConnected) {
      override def call(): Unit = {
        doSendAlarm(alarm)
      }
    }
    blocker.callWithGuard(guardedAction)
  }

  def doSendAlarm(alarm: String): Unit = {
    println("sending alarm : " + alarm)

    try {
      Thread.sleep(50)
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def init(): Unit = {
    val connectingThread: Thread = new Thread(new ConnectingTask())
    connectingThread.start()
    heartbeatTimer.schedule(new HeartbeatTask(), 60000, 2000)
  }

  def disconnect(): Unit = {
    println("Disconnected from alarm server...")
    connectedToServer = false
  }

  def onConnected(): Unit = {
    try {
      blocker.signalAfter(new Callable[Boolean] {
        override def call(): Boolean = {
          connectedToServer = true
          println("connected to server...")
          true
        }
      })
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  def onDisconnected(): Unit = {
    connectedToServer = false
  }

  class ConnectingTask extends Runnable {
    override def run(): Unit = {
      //...

      //...
      try {
        Thread.sleep(100)
      } catch {
        case e: InterruptedException => e.printStackTrace()
      }
      onConnected()
    }
  }

  class HeartbeatTask extends TimerTask {
    override def run(): Unit = {
      if (!testConnection()) {
        onDisconnected()
        reconnect()
      }
    }

    private def testConnection(): Boolean = {
      //...
      true
    }

    private def reconnect(): Unit = {
      val connectingThread = new ConnectingTask()
      connectingThread.run()
    }
  }

}


trait Predicate {
  def evaluate(): Boolean
}

abstract class GuardedAction[V](val guard: Predicate) extends Callable[V] {

}


trait Blocker {
  def callWithGuard[V](guardedAction: GuardedAction[V]): V

  def signalAfter(stateOperation: Callable[Boolean]): Unit

  def signal(): Unit

  def broadcastAfter(stateOperation: Callable[Boolean]): Unit
}

class ConditionBlocker() extends Blocker {

  var lock: Lock = new ReentrantLock()
  var condition: Condition = lock.newCondition();

  def this(lock: Lock) {
    this()
    this.lock = lock
    this.condition = lock.newCondition()
  }

  override def callWithGuard[V](guardedAction: GuardedAction[V]): V = {
    lock.lockInterruptibly()
    var result: V = null.asInstanceOf[V]
    try {
      val guard: Predicate = guardedAction.guard
      while (!guard.evaluate()) {
        condition.await()
      }
      result = guardedAction.call()
      result
    } finally {
      lock.unlock()
    }
  }

  override def signalAfter(stateOperation: Callable[Boolean]): Unit = {
    lock.lockInterruptibly()
    try {
      if (stateOperation.call()) {
        condition.signal()
      }
    } finally {
      lock.unlock()
    }
  }

  override def signal(): Unit = {
    lock.lockInterruptibly()
    try {
      condition.signal()
    } finally {
      lock.unlock()
    }
  }

  override def broadcastAfter(stateOperation: Callable[Boolean]): Unit = {
    lock.lockInterruptibly()
    try {
      if (stateOperation.call()) {
        condition.signalAll()
      }
    } finally {
      lock.unlock()
    }
  }
}

