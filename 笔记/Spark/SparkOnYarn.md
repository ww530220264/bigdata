# Spark On Yarn

## 概念

### RDD

> 分区、容错、不可变

```
val conf = new SparkConf().setAppName("").setMaster("")
val sc = new SparkContext(conf)
```

```
val data = Array(1,2,3,4,5)
val distData = sc.parallelize(data[,numPartitions])
sc.textFile("/my/directory")
//sc.textFile("/my/directory/*.txt")
//sc.textFile("/my/directory/*.gz")
```

+ 分区

  > 默认情况下一个Block（HDFS）一个分区
  >
  > 【注意】分区数不能少于Block的数量

### DataSet

### DataFrame

### Transformation

> 从一个已存在的RDD创建一个新的RDD

### Actions

> 基于dataset计算之后将计算结果返回给Driver驱动程序

### 共享变量

#### broadcast variables

> 可被用来在所有节点的内存上缓存一些值

```
val broadcastVar = sc.broadcast(Array(1,2,3))
broadcastVar.value //get value
```



#### accumulators

> 只能被added的变量，例如counters和sums

```
val accum = sc.longAccumulator("My Accumulator")
sc.parallelize(Array(1,2,3,4)).foreach(x=>accum.add(x))
accum.value //get value【只有Driver端能读取累加器的value】
```

### Driver

#### 启动

+ 创建DriverEnv【Spark执行环境】
  + 启动sparkDriver服务并监听相应端口
  + 初始化序列化器、序列化管理器、闭包序列化器
  + 初始化广播管理器、MapOutputTracker【注册】
  + 创建BlockManagerMaster并注册、创建BlockManager
  + 初始化ShortShuffleManager、内存管理器
  + 创建OutputCommitCoordinator并注册
    + OutputCommitCoordinator在Driver和Executors端都会被初始化，如果再Executors端初始化，则会包含一个Driver的with OutputCommitCoordinatorEndpoint一个引用，因此，提交输出的请求将会被转发到Driver端的OutputCommitCoordinator
+ 创建TaskScheduler
  + 默认调度策略：FIFO
+ 创建DAGScheduler
  + 作用
    + 生成DAG
    + 根据当前缓存的状态决定执行Task的首选位置，并把这些信息传递给TaskScheduler
    + 处理在Shuffle阶段输出文件丢失的情况，可能会重新提交之前的Stages【在一个Stage内部出现的错误是由TaskScheduler处理的，比如重试。。。】
    + 跟踪哪些RDD被缓存了并且记住哪些ShuffleMapStage已经产生了输出文件
    + 当一些Job执行完之后，他们依赖的哪些数据将被清理，避免在一个长时间运行的Application时发生内存泄漏
  + 注意事项
    + Job完成时，他们依赖的数据需要被清理
    + 避免在一个长时间运行的任务中使用无限的状态累加

## 命令

+ Cluster

  ```
  $ ./bin/spark-submit --class org.apache.spark.examples.SparkPi \
      --master yarn \
      --deploy-mode cluster \
      --driver-memory 4g \
      --executor-memory 2g \
      --executor-cores 1 \
      --queue thequeue \
      examples/jars/spark-examples*.jar \
      10
  ```

+ Client

  ```
  $ ./bin/spark-shell --master yarn --deploy-mode client
  ```

+ Add Jar

  ```
  # 添加client本地的文件到分布式缓存中，然后Driver和Executor可以从分布式缓存中下载该文件到他们的机器上
  $ ./bin/spark-submit --class my.main.Class \
      --master yarn \
      --deploy-mode cluster \
      --jars my-other-jar.jar,my-other-other-jar.jar \
      my-main-jar.jar \
      app_arg1 app_arg2
  ```

+ Debugging

  ```
  yarn log -applicationId <app ID>
  ```

## External Shuffle Service

> 为了在每个NodeManager上启动Spark Shuffle Service，按照以下步骤进行配置
>
> 【1】使用对应的yarn版本编译Spark
>
> 【2】定位spark-<version>-yarn-shuffle.jar文件【默认在$SPARK_HOME/common/network-yarn/target/scala-<version>】
>
> 【3】将这个jar文件添加到集群中每一个NodeManager的类路径下
>
> 【4】编辑yarn-site.xml
>
> ```
> <property>
>         <name>yarn.nodemanager.aux-services</name>
>         <value>spark_shuffle,mapreduce_shuffle</value>
> </property>
> <property>
>         <name>yarn.nodemanager.aux-services.mapreduce_shuffle.class</name>
>         <value>org.apache.hadoop.mapred.ShuffleHandler</value>
> </property>
> <property>
>         <name>yarn.nodemanager.aux-services.spark_shuffle.class</name>
>         <value>org.apache.spark.network.yarn.YarnShuffleService</value>
> </property>
> ```
>
> 【5】在yarn-env.sh中设置YARN_HEAPSIZE来增加NodeManager的堆大小，以避免shuffle期间的垃圾回收问题
>
> 【6】重启NodeMan

History Server

> 可以使用Spark History Server替代Spark WebUI，当WebUI是disabled状态的时候。这样可以减少Driver的内存使用。按照以下步骤进行配置
>
> 【1】在application端，通过在SparkConfiguration中配置spark.yarn.historyServer.allowTracking=true来告诉Spark如果application UI是disabled的话使用HistoryServer URL作为跟踪URL
>
> 【2】在Spark HistoryServer端，添加org.apache.spark.deploy.yarn.YarnProxyRedirectFilter到spark.ui.filters属性配置的过滤器列表中
>
> 【注意事项】history Server中的信息可能不是application的最新状态信息

