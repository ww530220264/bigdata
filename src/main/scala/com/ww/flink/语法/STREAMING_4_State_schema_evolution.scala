package com.ww.flink.语法

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

object STREAMING_4_State {

  /**
   * 状态模式演化步骤
   * step1:给Streaming作业做一个savepoint检查点
   * step2:更新状态类型(eg:修改Avro type schema)
   * step3:从savepoint还原作业,当第一次访问状态时,Flink将评估改状态的模式是否已经更改,并在必要时迁移状态模式
   * 注意:目前模式演化只支持POJO和Avro类型
   */
  private val configuration = new Configuration()
  configuration.setBoolean("queryable-state.enable", true)
  val env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(configuration)

}



