package com.ww.flink.train

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.util.Collector
import org.apache.flink.api.scala._

object Tran_2_FraudDetect {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val eles = env.fromElements(Transaction(1L,0.99),Transaction(1L,500.99),
      Transaction(1L,0.99),Transaction(1L,0.89),
        Transaction(1L,1.99),Transaction(1L,500.99),
      Transaction(1L,0.99),Transaction(1L,500.99))
    eles.keyBy(t=>t.clientId).process(new FraudDetector).name("fraud-detector").printToErr()
    env.execute("fraud-detector")
  }
}


case class Transaction(clientId: Long, amount: Double)

case class Alert(clientId: Long)

object FraudDetector {
  val SMALL_AMOUNT: Double = 1.00
  val LARGE_AMOUNT: Double = 500.00
  val ONE_MINUTE: Long = 60 * 1000
}

class FraudDetector extends KeyedProcessFunction[Long, Transaction, Alert] {

  @transient private var valueState: ValueState[Boolean] = _
  @transient private var timerState: ValueState[Long] = _

  @throws[Exception]
  override def open(parameters: Configuration): Unit = {
    valueState = getRuntimeContext.getState(new ValueStateDescriptor("valueState", classOf[Boolean]))
    timerState = getRuntimeContext.getState(new ValueStateDescriptor[Long]("timerState", classOf[Long]))
  }

  override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[Long, Transaction, Alert]#OnTimerContext, out: Collector[Alert]): Unit = {
    valueState.clear()
    timerState.clear()
  }

  override def processElement(value: Transaction, ctx: KeyedProcessFunction[Long, Transaction, Alert]#Context, out: Collector[Alert]): Unit = {
    if (valueState.value() != null) {
      if (value.amount > FraudDetector.LARGE_AMOUNT) {
        out.collect(Alert(value.clientId))
      }
      cleanUp(ctx)
    }
    if (value.amount < FraudDetector.SMALL_AMOUNT) {
      valueState.update(true)
      val timer = ctx.timerService().currentProcessingTime() + FraudDetector.ONE_MINUTE
      ctx.timerService().registerProcessingTimeTimer(timer)
      timerState.update(timer)
    }

    @throws[Exception]
    def cleanUp(ctx: KeyedProcessFunction[Long, Transaction, Alert]#Context): Unit = {
      ctx.timerService().deleteProcessingTimeTimer(timerState.value())
      valueState.clear()
      timerState.clear()
    }
  }
}
