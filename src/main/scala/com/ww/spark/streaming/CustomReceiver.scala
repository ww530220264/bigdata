package com.ww.spark.streaming

import java.io.{BufferedReader, InputStreamReader}
import java.net.Socket
import java.nio.charset.StandardCharsets

import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.receiver.Receiver

object CustomReceiver {
  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      System.err.println("Usage: CustomReceiver <hostname> <port>")
      System.exit(1)
    }
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("CustomReceiver")
    val ssc = new StreamingContext(sparkConf, Seconds(1))

    val lines = ssc.receiverStream(new CustomReceiver(args(0), args(1).toInt))
    val words = lines.flatMap(_.split(" "))
    val wordCounts = words.map(x => (x, 1L)).reduceByKey(_ + _)
    wordCounts.print()

    ssc.start()
    ssc.awaitTermination()
  }
}

class CustomReceiver(host: String, port: Int)
  extends Receiver[String](StorageLevel.MEMORY_AND_DISK_2) {

  override def onStart(): Unit = {
    new Thread("Socket Receiver") {
      override def run(): Unit = receive()
    }.start()
  }

  override def onStop(): Unit = {

  }

  private def receive(): Unit = {
    var socket: Socket = null
    var userInput: String = null

    try {
      println(s"Connecting to $host : $port")
      socket = new Socket(host, port)
      println(s"Connected to $host : $port")
      val reader = new BufferedReader(
        new InputStreamReader(socket.getInputStream, StandardCharsets.UTF_8)
      )
      userInput = reader.readLine()
      while (!isStopped && userInput != null) {
        store(userInput)
        userInput = reader.readLine()
      }
      reader.close()
      socket.close()
      println("Stopped receiving")
      restart("Trying to connect again")
    } catch {
      case e: java.net.ConnectException =>
        restart(s"Error connecting to $host : $port")
      case t: Throwable =>
        restart("Error receiving data", t)
    }
  }
}
