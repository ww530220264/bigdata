package com.ww.flink.train

import org.apache.flink.api.common.accumulators.IntCounter
import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.scala._
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

object Tran_1_Batch_Example {
  val env = StreamExecutionEnvironment.getExecutionEnvironment

  //  val env = ExecutionEnvironment.getExecutionEnvironment

  case class Person(name: String, age: Int)

  def main(args: Array[String]): Unit = {
    //    val eles = env.fromElements(("a",1),("a",2),("a",3),("b",2),("b",3),
    //      ("b",4),("e",1),("e",2),("f",1))
    //    eles.groupBy(0).reduce((x1,x2)=>(x1._1,x1._2+x2._2)).printToErr()
    //======
    //    val eles1 = env.fromElements(("a",1),("b",2))
    //    val eles2 = env.fromElements(("b","3"))
    //    eles1.join(eles2).where(0).equalTo(0){
    //      (ele1,ele2)=>(ele2._2,ele1._2)
    //    }.printToErr()
    //    env.execute()
    //======

    val eles1 = env.fromElements(("a", 1), ("b", 2)).map(new RichMapFunction[(String, Int), (String, Int)] {
      private val numEles = new IntCounter()

      override def open(parameters: Configuration): Unit = {
        getRuntimeContext.addAccumulator("numEles", this.numEles)
      }

      override def map(value: (String, Int)): (String, Int) = {
        this.numEles.add(1)
        value
      }
    })
    val eles2 = env.fromElements(("b", 3)).map(new RichMapFunction[(String, Int), (String, Int)] {
      private val numEles = new IntCounter()

      override def open(parameters: Configuration): Unit = {
        getRuntimeContext.addAccumulator("numEles", this.numEles)
      }

      override def map(value: (String, Int)): (String, Int) = {
        this.numEles.add(1)
        value
      }
    })
    val client = env.executeAsync()
    val result = client.getJobExecutionResult(this.getClass.getClassLoader).get()
    val value = result.getAccumulatorResult("numEles").asInstanceOf[Int]
    System.err.println(value)
  }

}
