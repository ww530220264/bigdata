# Spark

## 概念

### App

### Job

### Stage

### Task

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

> 创建方式

```
将RDD转换为DataFrame
	RDD[Tuple].toDF
	RDD[case class].toDF
	sparkSession.createDataFrame(RDD,Schema)
运行SQL查询
加载外部数据
```

### Transformation

> **aggregateByKey**(zeroValue)(seqOp,combOp,[numPartitions])
>
> ```
> // 第一个参数代表一个初始化的值
> // 第一个函数作用于每个分区
> // 第二个函数是合并每个分区的结果
> customRdd.aggregateByKey(List[String]())(
>       (prods, tran) => prods ::: List(tran(3)),
>       (prod1, prod2) => prod1 ::: prod2)
> ```

> **map**(fun)：对rdd中的每个元素使用该函数

> **filter**(func)：对rdd中的每个元素使用该函数，过滤掉返回false的元素

> **flatMap**(func)：对rdd中的每个元素使用该函数然后返回一个集合，该集合中的每个元素对应目标rdd的中一个元素

> **mapPartitions**(func)
>
> ```
> 对每rdd中的每个分区应用一次该函数，当需要将rdd中的数据输出到外部存储系统时可以采用该方法，避免频繁创建连接
> ```

> **mapPartitionsWithIndex**(func)
>
> ```
> 类似mapPartitions，不过函数参数中增加了一个分区序号
> ```

> **sample**(withReplacement,fraction,seed)
>
> ```
> withReplacement:是否有放回的抽样
> fraction：0-1之间的double数据，表示抽样率
> seed：随机种子【可用于调试】
> 场景：通过抽样出一部分样本，并取出出现次数最多的key，这个key就可能就是导致数据倾斜的key
> ```

> **union**(otherDataset)：合并两个rdd中的元素

> **intersection**(otherDataset)：取两个rdd中元素的交集

> **distinct**([numPartitions])：去重rdd中的元素

> **groupByKey**([numPartitions])：按key将rdd中的元素分组

> **reduceByKey**(func,[numPartitions])：按key将rdd中相同key对应的值应用给定的函数

> **sortByKey**([ascending],[numPartitions])：按key进行排序

> **join**(otherDataset,[numPartitions])：按key进行join
>
> ```
> (K,V).join(K,W)==>(K,(V,W))
> 支持leftOuterJoin、rightOuterJoin、fullOuterJoin
> ```

> **cogroup**(otherDataset,[numPartitions])
>
> ```
> (K,V).cogroup(K,W)==>(K,(Iterable<V>,Iterable<W>))
> ```

> **cartesian**(otherDataset)：笛卡尔积
>
> ```
> (T).cartesian(U)==>(T,U)
> ```

> **pipe**(command,[envVars])：通过shell命令对rdd的每个分区进行管道传输

> **coalesce**(numPartitions)：将rdd中的分区数降低到指定的numPartitios，这样有助于在筛选大数据集之后更有效的操作

> **repartition**(numPartitions)：随机的整理rdd中的数据来创建更多或更少的分区并且在分区直接平衡数据，这总是会通过网络shuffle所有的数据

> **repartitionAndSortWithinPartitions**(partitioner)
>
> ```
> 根据提供的partitioner进行repartition操作，然后在每个partition内根据key对数据进行排序。
> 这个操作比repartition+sort更有效，因为它会将排序操作下推到shuffle操作中进行执行
> ```

### Actions

> **reduce**(func)

> **collect**()

> **count**()

> **first**()

> **take**(n)

> **takeSample**(withReplacement,fraction,[seed])：返回一个抽样的元素数组

> **takeOrdered**(n,[ordering])：使用自然顺序排序或自定义比较器排序返回前n个元素

> **saveAsTextFile**(path)

> **saveAsSequenceFile**(path)

> **saveAsObjectFile**(path)：使用java序列化器和简单的格式将数据集中的元素写入文件。可以调用SparkContext.objectFile(path)加载

> **countByKey**()

> **foreach**(func)

### DAG

#### Dependency

+ Narrow Dependency
  + OneToOneDependency
    + 使用在其他的没有shuffle操作的转换中
  + RangeDependency
    + 只会在union转换操作中存在
    + 将多个parent的RDD合并到一个单独的依赖中
+ Wide Dependency

### 共享变量

#### broadcast variables

> 可被用来在所有节点的内存上缓存一些值

```
val broadcastVar = sc.broadcast(Array(1,2,3))
broadcastVar.value //get value
```

> 销毁广播变量

```
destroy：执行该操作之后，Driver和Executos中的变量都会销毁，再次使用的话，将会抛出异常
unpersist：执行该操作之后，将移除executors上的广播变量，再次使用的话，变量重新从Driver传输到Executors
注意：在变量超过他的作用范围之后，Spark会自动执行unpersist操作，因此不用明确的执行unpersist操作。相反，可以在Driver中移除broadcast variable的引用
```

> 相关变量

```
spark.broadcast.compress：在传输前压缩，压缩类spark.io.compression.codec
spark.broadcast.blockSize：只是传输广播变量的数据块的大小，默认4096
spark.python.worker.reuse：默认true
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
> 【6】重启NodeManager

## History Server

> 可以使用Spark History Server替代Spark WebUI，当WebUI是disabled状态的时候。这样可以减少Driver的内存使用。按照以下步骤进行配置
>
> 【1】在application端，通过在SparkConfiguration中配置spark.yarn.historyServer.allowTracking=true来告诉Spark如果application UI是disabled的话使用HistoryServer URL作为跟踪URL
>
> 【2】在Spark HistoryServer端，添加org.apache.spark.deploy.yarn.YarnProxyRedirectFilter到spark.ui.filters属性配置的过滤器列表中
>
> 【注意事项】history Server中的信息可能不是application的最新状态信息

# Spark SQL

# Spark Streaming

## Transformation

> **map**(func)

> **flatMap**(func)

> **filter**(func)

> **repartition**(numPartitions)：通过创建更多或更少的分区改变数据流的并行级别

> **union**(otherStream)：合并其他的数据流

> **count**()：统计数据流中每个rdd中元素个数

> **reduce**(func)：聚合数据流中每个rdd中的元素

> **countByValue**()：统计数据流中每个rdd中元素出现的频率【K==>(K,V)】

> **reduceByKey**(func,[numTasks])

> **join**(otherStream,[numTasks])

> **cogroup**(otherStream,[numTasks])

> **transform**(func)：将func应用在数据流中每个rdd上并产生一个新的数据流

> **updateStateByKey**(func)：返回一个新的state数据流，使用该函数、先前key的状态及该key最新的值来更新key的state

> **mapWithState**(StateSpec):
>
> **相比updateStateByKey有功能改进和性能提升。可以维持比updateStateByKey10倍多的keys并且快6倍**
>
> ```scala
> (Time, KeyType, Option[ValueType], State[StateType]) => Option[MappedType]
> val updateAmountState = (clientId:Long, amount:Option[Double], 	state:State[Double]) => {
>      var total = amount.getOrElse(0.toDouble)
>      if(state.exists())
>         total += state.get()
>      state.update(total) 
>      Some((clientId, total))
> }
> val amountState = amountPerClient.mapWithState(StateSpec.function(updateAmountState)).stateSnapshots()
> 
> 【注意】：如果没有stateSnapshots方法，那么只会返回当前当前窗口内存在的key的state数据，如果加上这个方法，则返回所有的key的states
> ```

## Window Operations

> **window**(windowLength,slideInterval)

> **countByWindow**(windowLength,slideInterval)

> **reduceByWindow**(func,windowLength,slideInterval,[numTasks])

> **reduceByKeyAndWindow**(func,invFunc,windowLength,slideInterval,[numTasks])
>
> ```
> 一个更有效的reduceByKey窗口计算方法，如果前后两个滑动窗口有重叠的话，那么可以使用上一次窗口计算的结果和新的进入窗口的数据使用func，同时对离开窗口的数据使用invFunc
> ```
>

## Outout Operations

> **print**()

> **saveAsTextFiles**(prefix,[suffix])

> **saveAsObjectFiles**(prefix,[suffix])

> **saveAsHadoopFiles**(prefix,[suffix])

> **foreachRDD**(func)
>
> ```
> dstream.foreachRDD { rdd =>
>   rdd.foreachPartition { partitionOfRecords =>
>     // ConnectionPool is a static, 
>     // lazily initialized pool of connections
>     val connection = ConnectionPool.getConnection()
>     partitionOfRecords.foreach(record => connection.send(record))
>     // return to the pool for future reuse
>     ConnectionPool.returnConnection(connection)  
>   }
> }
> // 连接池中的连接需要按需创建，并且有一定的超时限制，这将实现向外部系统写入数据的最有效方式
> ```

## Cache/Persistence

> 基于窗口的操作，如reduceByWindow，reduceByKeyAndWindow，默认会缓存相关的rdd，不需要显示调用
>
> 基于网络接收的数据，默认将数据备份到两个节点上以实现容错
>
> 默认持久化级别是在内存中序列化数据

## 监控指标

+ Input Rate：每秒输入的记录数量

+ Scheduling Delay：新的mini-batch等待他们的job被调度的时间

+ Processing Time：处理每个mini-batch的job需要的时间

+ Total Delay：处理一个mini-batch花费的总时间（等待调度时间+处理时间）

+ 注意事项

  ```
  	每个mini-batch的总的processing time（total delay）应该要小于生成mini-batch的时间（生成mini-batch的duration时间）且或多或少是恒定的。如果总的处理时间持续上升，那么这个应用是不可持续运行的。必须降低processing time或者increate parallelism或者limit the input rate
  ```

## 性能优化

+ **降低Processing Time**

  + 避免不必要的shuffles操作
  + 如果需要把数据持久化到外部存储系统，需要在分区内重用连接或者使用一些连接池
  + 提高生成mini-batch的duration
    + 因为job-schedule，task-serialization和data-shuffle相关操作的时间比较耗时，如果对大的数据集执行这些操作，那么就可以降低每个记录的平均处理时间
    + 但是如果设置的太高的话，会增加每个mini-batch的内存需要。另外，低频率的输出可能不符合一些业务场景
  + 增加集群资源
    + 增加内存可能会降低GC频率
    + 增加CPU可能会提升处理速度

+ **提升并行度【有效的使用所有的CPU的核数并且获得更高的吞吐量】**

  + 在input source端提高并行度【Kafka Partitions】，比如创建多个【receiver】
  + 调整spark.streaming.blockInterval参数。对于大多数receivers来说，他们接收到的数据在存储在Spark内存之前将会合并为数据块（blocks of data），每个batch的block数量决定了将要处理接收到数据的任务的数量。例如interval为200ms的话，在2s内将创建10个task。如果task的数量太少的话，可用的CPU cores将不会用来处理数据，那么它将是低效的。可以通过降低blockInterval时间来增加每个batch内的task的数量。如果任务数量太多的话，那么启动任务所花费的开销将是一个问题。建议最小的blockInterval时间为50ms
  + 使用repartition方法将DStreams重分区为更多数量的分区【一般来说receivers的数量不要超过可用的CPU核数或executors的数量】
  + reduceByKey、reduceByKeyAndWindow的 操作的默认并行task的数量是spark.default.parallelism指定的，可以传递一个大的数或者修改spark.default.parallelism的值来提升操作的并行度来充分利用集群资源

+ **限制输入数据的速率**

  + 如果上述操作仍然不能降低schedule delay的话，可以限制摄取数据的速率

    ```
    【receiver-based Consumer】spark.streaming.receiver.maxRate
    【direct Consumer】spark.streaming.kafka.maxRatePerPartition
    ```

  + 启用反压策略 

    + 如果出现了调度延迟，将自动限制最大获取的数据的数量。如果设置了上面两个参数，那么自动调整的速率将不会超过上面的两个参数设置的值

      ```
      spark.streaming.backpressure.enable=true
      ```
  
+ **数据序列化**

  + Input data
    + 默认情况下Receivers接收的数据保存在Executors的Memory中，存储级别为StorageLevel.MEMORY_AND_DISK_SER_2。也就是说数据被序列化以减少GC开销并且进行备份以实现容错。数据首先保存在内存中，如果内存不足以容纳所有流计算所需的所有输入数据的话，数据将会溢出到磁盘。这种序列化显然是有开销的，Receiver必须反序列化接收到的数据，并使用Spark的序列化格式重新序列化它

  + Persisted RDDs generated by Streaming Operations
    + 在流计算中生成的rdds可能会缓存在内存中。例如：窗口操作将数据持久化在内存中，因为他们将被多次处理。持久化老数据中间的rdd的默认存储级别是StorageLevel.MEMORY_ONLY_SER以减少GC开销
  + 在这两种情况下，使用Kryo序列化可以同时减少CPU和Memory开销。【考虑注册自定义类并禁用对象引用跟踪】。如果流计算中需要保留的数据量不大的情况下，可以将数据作为反序列化对象持久化，而不会产生过多的GC开销。例如，在使用较小的batch interval且没有窗口操作的情况下，可以通过指定存储级别来禁用在持久化数据时的序列化操作。这将减少由于序列化导致的CPU开销，从而在没有太多GC开销的情况下提高性能

+ **设置合适的Batch Interval**

## 内存优化

### 背景

> 默认情况下，java对象的访问速度已经很快了，但是java对象平均比字段中原始数据多占用2-5倍空间
>
> ```
> 每个不同的对象都有一个对象头，里面包含了markword、class pointer等信息，如果对象的数据占用空间很小的话，相对而言对象头则比数据占用了更多的空间
> ```
>
> ```
> String类型数据比原始数据多了大约40字节的开销，因为它使用字符数组存储数据，且里面包含length等数据，因为String内部使用UTF-16编码，因此每个字符占用2个字节。因此一个10个字符的字符串将占用60字节空间
> ```
>
> ```
> 一些集合类比如HashMap、LinkedList使用链接的数据结构，其中每个条目都有一个包装器Warpper对象，比如Map.Entry，这个对象不仅有一个header，而且还有指向列表中下一个对象的指针（一般8个字节）
> ```
>
> ```
> 原始类型的集合通常将他们存储为装箱对象，例如java.lang.Integer
> ```

### 内存管理

### 措施

+ Persistence Level of DStreams

  + 使用Kryo序列化器【conf.set("spark.serializer":"org.apache.spark.serializer.KryoSerializer")】

    ```scala
    // 注册自定义Kryo序列化器
    val conf = new SparkConf().setMaster(...).setAppName(...)
    conf.registerKryoClasses(Array(classOf[MyClass1], classOf[MyClass2]))
    val sc = new SparkContext(conf)
    // 如果对象比较大的话，需要增加缓冲区的值
    【spark.kryoserializer.buffer】来容纳将要序列化的最大的对象
    // 如果不注册自定义的序列化器，Kryo仍然会生效，但是它将保存所有对象的全类名，这比较浪费
    ```

  + 通过压缩可以进一步减少内存使用【spark.rdd.compress】,但是会增加CPU开销

+ Clear old data

  + more情况下Spark会自动清除由DStream转换生成的所有输入数据和持久化rdd。Spark Streaming根据所使用的转换决定决定何时清除数据。如果使用一个10分钟大小的窗口操作，然后Spark Streaming将保留大约10分钟的数据，并主动丢弃旧数据，通过设置，数据可以保留更长的时间【StreamingContext.remember】

+ CMS Garbage Collector

  + 虽然CMS会降低系统的总体吞吐量，但仍然建议使用它来实现更一致的批处理时间
  + 确保在dirver【--driver-java-options】端和executor【spark.executor.extraJavaOptions】端都使用了CMS

+ 其他

  + 使用OFF_HEAP持久化rdds
  + 使用更多更小的Heap Size的Executors以减少单个JVM heap的GC压力

+ 注意事项

  + 一个DStream和一个Receiver关联，可以创建多个Receiver提高并行度。一个Receiver运行在一个Executor上并且占用一个CPU core。因此在分配给Receiver一个CPU core后需要确保仍有足够的cores来处理数据。因此spark.cores.max参数值需要考虑到Receiver所占用的core。Receiver是以轮询的方式分配给Executors
  + 当Receiver接收到数据后，Receiver将创建blocks of data。每个blockInterval将生成一个block。一个bacth的数据块的数量N=batchInterval / blockInterval。这些数据块将被当前executor上的BlockManager分布到其他executors的BlockManager上。在这之后，运行在Driver端的网络输入跟踪器将被告知block的位置信息以供进一步处理
  + 一个block代表当前batch中rdd的一个分区，如果batchInterval == blockInterval意味着，一个batch rdd的分区数为1，而且可能在本地处理这个数据
  + 大的blockInterval意味这更少的分区（更少的任务），更大的块。如果spark.locality.wait的值比较大则意味着在数据本地节点上处理数据的机会更大（增加了数据本地性的机会），需要在这两个参数进行平衡
  + 除了blockInterval和batchInterval，也可以通过调用inputStream.repartition(n)定义分区的数量，这将对数据执行随机shuffle操作来创建n个分区。rdd的处理是被Driver端的jobScheduler作为一个job进行调度执行的。在给定的时间点上，只有一个job处于活跃状态，因此如果一个job正在执行，那么其他作业将排队执行
  + 如果有多个输入流，单独在这两个流上执行操作，那么将会产生两个job，然后一个接一个被调度执行。可以将这两个流合并为一个流来避免这个问题。这将会只产生一个job。但是rdd的分区不会受到影响
  + 如果batch processing time超过batchInterval那么Receiver的内存将会开始被填充并最终引发异常。目前没有办法暂停Receiver。可以配置spark.streaming.receiver.maxRate的值来限制Receiver接收数据的速率

## 容错

### Executor Failures

```
	当运行Receiver的Executor失败的话，Driver会自动在另一个节点上重启一个executor，而且相关的数据也会恢复。不需要手动指定，spark自动处理
```

### Driver Failures

```
	当Driver失败时，相关Executors的连接也会丢失，应用需要被重启。集群管理器会自动重启Driver进程（在Standlone模式下需要--supervise，在其他资源管理器上需要使用cluster模式）
```

```
	当Driver进程被重启的时候，Spark Streaming通过读取Streaming Context的checkpoint state来恢复之前应用的状态。
StreamingContext.getOrCreate()方法会先检查checkpoint目录是否存在一些状态数据，如果存在的话，就加载之前的checkpoint state并跳过StreamingContext的初始化。如果不存在的话就调用StreamingContex的初始化方法
```

### Exactly Once

+ 使用batchTime和partitionIndex作为唯一标识提交数据到外部系统

  ```scala
  dstream.foreachRDD { (rdd, time) =>
    rdd.foreachPartition { partitionIterator =>
      val partitionId = TaskContext.get.partitionId()
      val uniqueId = generateUniqueId(time.milliseconds, partitionId)
      // use this uniqueId to transactionally commit the data in partitionIterator
    }
  }
  ```


## CheckPointing

> **spark Streaming需要checkpoint足够的信息到一个可容错的存储系统以实现当运行Driver驱动程序的节点失败时可以恢复应用**

> **总而言之，元数据检查点主要用于从Driver驱动程序中恢复，而如果使用stateful transformation，数据或rdd检查点对基本功能也是需要的**

> 需要衡量checkpointing的频率，如果太频繁，将会影响增加batches的processing time，如果太不频繁，可能会造成依赖链和任务数量的增加。对于stateful transformation操作中需要checkpointing相关rdd的默认时间间隔是批处理间隔的倍数，至少为10秒【**dstream.checkpoint(checkpointInterval)**】。**通常，数据量的检查点间隔为5-10个滑动间隔是一个很好的尝试设置**

> **Accumalator和Broadcast Variables不能从checkpoint点恢复。在spark streaming中如果要启用checkpoint和使用Accumulator和Broadcast Variables时，需要为Accumulators和Boradcast Variables创建延迟初始化单例对象，此时他们在Driver失败的时候可以被重新初始化**
>
> ```scala
> object WordBlacklist {
>   @volatile private var instance: Broadcast[Seq[String]] = null
>      def getInstance(sc: SparkContext): Broadcast[Seq[String]] = {
>          if (instance == null) {
>              synchronized {
>                 if (instance == null) {
>                   val wordBlacklist = Seq("a", "b", "c")
>                   instance = sc.broadcast(wordBlacklist)
>                 }
>              }
>          }
>          instance
>      }
>    }
>   object DroppedWordsCounter {
>     @volatile private var instance: LongAccumulator = null
>     def getInstance(sc: SparkContext): LongAccumulator = {
>          if (instance == null) {
>            synchronized {
>                if (instance == null) {
>                    instance = sc.longAccumulator("WordsInBlacklistCounter")
>                }
>            }
>          }
>          instance
>      }
>    }
>    wordCounts.foreachRDD { (rdd: RDD[(String, Int)], time: Time) =>{
>        // Get or register the blacklist Broadcast
>        val blacklist = WordBlacklist.getInstance(rdd.sparkContext)
>       // Get or register the droppedWordsCounter Accumulator
>     	val droppedWordsCounter = DroppedWordsCounter.getInstance(rdd.sparkContext)
>     // Use blacklist to drop words and use droppedWordsCounter to count them
>     val counts = rdd.filter { case (word, count) =>
>        if (blacklist.value.contains(word)) {
>          droppedWordsCounter.add(count)
>          false
>        } else {
>          true
>        }
>        }.collect().mkString("[", ", ", "]")
>        val output = "Counts at time " + time + " " + counts
>    }
>    ```

## When to enable Checkpointing

+ 当使用有状态转换的时候，比如【updateStateByKey】或【reduceByKeyAndWindow】的时候
+ 需要从Driver驱动程序中恢复的时候

## Metadata checkpointing

> 将流计算的定义信息保存到一个容错系统【HDFS】，当运行Driver驱动程序的节点失败时可以进行恢复

+ Configuration：用来创建流应用的配置信息
+ DStream operations：定义流应用的DStream操作的集合
+ Incomplete batches：已经在队列中但是没有完成的Batches

## Data checkpointing

> 将生成的rdd保存到可信赖的文件系统。在一些合并多个batches中的数据的stateful转换操作中是需要checkpointing的。在一些操作中一些生成的rdd依赖之前的bacthes，这导致依赖链的长度随着时间不断增加。为了避免恢复时间的无限增长（与依赖链的长度成正比），把在stateful转换操作中生成的rdd周期性的checkpointing到可信赖的系统来斩断依赖链

## Deploying Application

### Requirements

+ 使用集群管理器
+ 打包应用jar
+ 为executors配置足够的内存
+ 配置checkpointing
+ 为applicaion Driver配置自动重启
+ 配置WAL
+ 配置最大接入速率
  + spark.streaming.receiver.maxRate：【**for receivers**】
  + spark.streaming.kafka.maxRatePerPartition：【**for Direct Kafka**】

## Upgrade Application Code

+ 将升级后的Spark流应用程序启动并与现有的应用程序并行运行。一旦新的（接收到与旧的相同的数据）已经预热并准备好进入时，可以停止运行旧的应用。需要支持将数据发送到多个目的地的数据源才可以这样做
+ 优雅的停止流应用程序（StreamingContext.stop()）,这样可以确保在停止之前，已经被接收的数据被完全处理，然后将升级后的应用程序启动，他将从先前应用程序停止的同一点开始处理【注意，这只能通过支持源端缓冲数据的输入源，比如Kafka、Flume来完成，因为前一个应用程序关闭而升级的应用尚未启动时需要缓冲数据】。不能使用之前应用的checkpoint信息进行重启，因为checkpoint信息本质上包含Scala/Java/Python的对象，试图用新的、修改过的类反序列化对象可能会导致错误

