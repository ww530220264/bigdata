package com.ww.flink

import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{MapState, MapStateDescriptor, StateTtlConfig}
import org.apache.flink.api.common.time.Time
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.util.Collector
import org.apache.flink.api.scala._
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup

object STREAMING_4_State {
  /**
   * 配置本地启动Queryable State服务
   * 创建试用WebUI的本地执行环境
   */
  private val configuration = new Configuration()
  configuration.setBoolean("queryable-state.enable", true)
  //  暂时不清楚
//  configuration.setBoolean("state.backend.local-recovery",true)
  val env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(configuration)

  def main(args: Array[String]): Unit = {

    _map_state
    env.execute("State_Value_State")
  }

  def _map_state = {
    env.setParallelism(2)
    //  状态后端(配置状态数据保存位置):文件系统/HDFS/Memory/RocksDB
    env.setStateBackend(new FsStateBackend("file:///../../mybackend"))
    //  允许检查点,如果不开启的话,任务异常则Job失败,不会从检查点恢复
    //  目前Flink对存在迭代Iteration的Job不提供保证,对迭代作业启动检查点将导致异常,为了强制启用检查点,如下操作
    //  env.enableCheckpointing(interval, CheckpointingMode.EXACTLY_ONCE, force = true).
    //  注意，在失败期间，迭代循环中的飞行记录边缘（以及与之相关的状态更改）将丢失。
    env.enableCheckpointing(5000)
    //  每次CheckPoint之间最小间隔时间
    env.getCheckpointConfig.setMinPauseBetweenCheckpoints(500)
    //  如果超过该超时时间,CheckPoint没有完成,则放弃
    env.getCheckpointConfig.setCheckpointTimeout(60000)
    //  最大并发执行CheckPoint尝试次数
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(1)
    //  设置CheckPoint持久化选项
    env.getCheckpointConfig.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)
    env.socketTextStream("centos7-1", 9999)
      .map(x => x.split(","))
      .map(x => (x(0).toLong, x(1)))
      .keyBy(0)
      .flatMap(new CountWindowAverage)
      .printToErr()
  }
}

class CountWindowAverage extends RichFlatMapFunction[(Long, String), (Long, String, Long)] {

  val stateTTL = StateTtlConfig
    .newBuilder(Time.seconds(5))
    .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
    .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
    .build()
  //  目前TTL和queryable不兼容
  //  mapState.enableTimeToLive(stateTTL)
  var map: MapState[String, Long] = _

  override def open(parameters: Configuration): Unit = {
    val mapState = new MapStateDescriptor[String, Long](
      "state_map",
      classOf[String],
      classOf[Long]
    )
    //  配置该状态可查询,且指定可查询名称
    mapState.setQueryable("query-name")
    map = getRuntimeContext.getMapState(mapState)
  }

  override def flatMap(in: (Long, String), collector: Collector[(Long, String, Long)]): Unit = {
    var count: Long = 1
    if (map.contains(in._2)) {
      count += map.get(in._2)
    }
    map.put(in._2, count)
    collector.collect((in._1, in._2, count))
  }
}



