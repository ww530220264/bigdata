## 分布式运行时环境

### Tasks and Operator Chains

> 对于分布式执行，Flink会将Operator算子子任务链接到一起生成tasks，每个task将被一个thread执行
>
> **优化**：**将Operator链接到一起是一个有效的优化，它减少了线程间切换和缓冲的开销，提高了整体吞吐量的同时降低了延迟**【如果可能的话，默认是使用的】。**下图有5个subtask将被执行，因此将由5个并行的线程**
>
> ![Operator chaining into Tasks](.\image\tasks_chains.svg)
>
> **配置**：
>
> + StreamingExecutionEnvironment.disableOperatorChaining()：在整个Job中禁用chaining
>
> + eg
>
>   ```scala
>   someStream.filter.map(...).startNewChain().map(...)
>   someStream.map(...).disableChaining()
>   //设置插槽共享组
>   someStream.filter(...).slotSharingGroup("name")
>   ```

### JobMangers,TaskManagers,Clients

+ JobManager【Master】

  + 负责协调分布式Execution，调度tasks，协调checkpoint，协调从failure中恢复
  + 至少有一个JobManager。HA设置将由多个JobManagers，其中一个是leader，其他的是standby

+ TaskManager【Worker】

  + 负责执行dataflow中的tasks【更具体的说，是子任务】，且缓冲和交换数据流
  + 至少有一个TaskManager
  + 连接到JobManager，向他们申明自己是可用的，然后被分配工作

+ Client

  + client不是runtime和程序执行的一部分，但是可以被用来准备和发送一个dataflow到JobManager。在这之后，client可以断开连接或者保存连接以接收进度报告。
  + client要么作为触发执行的Java/Scala程序的一部分运行，要么在命令行中执行：./bin/flink run

  ![The processes involved in executing a Flink dataflow](.\image\processes.svg)

### TaskSlots and Resources

+ 每个Worker【TaskManager】就是一个JVM进程，并且可以在单独的线程中执行一个或多个子任务

+ 为了控制一个Worker可以接受多少任务，一个Worker有了所谓的Task Slot【至少一个】

+ Task Slot

  + 每个Task Slot代表TaskManager资源的一个自己。如果一个TaskManager有3个Task Slot，代表着将其1/3的托管内存【managed memory】专用于每个slot。将资源分档意味着子任务不会与其他Job中的子任务竞争managed memory，因为它有一定数量的Managed memory。需要注意的是，在这里没有CPU隔离，当前Slot只分离tasks的Managed memory
  + 通过调整task slot的数量，可以定义subtasks是如何互相隔离的。如果一个TaskManager有一个Task Slot意味着每个task group运行在一个单独的JVM【eg：可以在一个单独的Container中启动】中。有多个Slots意味着更多的subtasks共享同一个JVM。在同一个JVM中的Tasks共享TCP连接【通过多路复用】和heartbeat消息。他们还可以共享数据集和数据结构，从而减少每个任务的开销

  ![A TaskManager with Task Slots and Tasks](.\image\tasks_slots.svg)

  + 默认情况下，Flink运行subtasks共享slots，即使他们是不同任务的subtasks，只要他们来自同一个Job。这可能会让一个slot可以容纳Job整个的Pipeline。允许slot share有两个主要的好处：

    + Flink集群中所需的任务slot数量与Job中使用的最高的并行度完全相同。不需要计算一个program总共包含多少个tasks【具有不同的parallelism】

    + 这样更容易获得更好的资源利用率，如果没有slot share，非密集型source、map子任务将阻塞和资源密集型window子任务一样多的资源。使用slot share的话，将上面例子中的并行度设置为6可以充分利用slotted资源，同时确保在TaskManager中公平的分配繁重的子任务：

      ![TaskManagers with shared Task Slots](.\image\slot_sharing.svg)

    + 根据经验，一个好的默认task slot数量应该是CPU核数，使用hyper-threading【超线程】，每个slot将占用2个或更多的hardware thread context

### State Backends

### SavaPoints

## DataSet

### Transformations

> map

> flatMap

> mapPartition

> filter

> reduce

> reduceGroup

> aggregate

> distinct

> join

> outerJoin

> coGroup

> cross

> union

> rebalance

> partitionByHash

> partitionByRange

> partitionCustom

> sortPartition

> first

> project

> minBy
>
> maxBy

## DataStream

### Transformations

https://ci.apache.org/projects/flink/flink-docs-release-1.10/dev/stream/operators/#datastream-transformations

> **map**：DataStream-->DataStream

> **flatMap**：DataStream-->DataStream

> **filter**：DataStream-->DataStream

> **keyBy**：DataStream-->KeyedStream
>
> ```
> 逻辑上将流划分为不相交的分区，所有相同key的记录都分配给同一个分区。默认使用hash分区
> ```
>
> 不能作为key的类型：
>
> + POJO但是没有重写hashCode（）方法，依赖对象的hashCode
> + 任何数组类型

> **reduce**：KeyedStream-->DataStream

> fold：KeyedStream-->DataStream

> **min**：KeyedStream-->DataStream
>
> **minBy**：KeyedStream-->DataStream
>
> **max**：KeyedStream-->DataStream
>
> sum：KeyedStream-->DataStream

> window：KeyedStream-->DataStream
>
> ```scala
> DataStream.keyBy(0).window(TumblingEventTimeWindow.of(Time.seconds(5)))
> ```

> windowAll：DataStream-->AllWindowedStream
>
> 在多数情况下，这是一种non-parallel transformation。windowAll operator将所有的元素聚集到一个任务中
>
> ```scala
> dataStream.windowAll(TumblingEventTimeWindow.of(Time.seconds(5)))
> ```

> 【Window】apply：WindowedStream-->DataStream：windowedStream.apply(WindowFunction)
>
> 【Window】apply：AllWindowedStream-->DataStream：allWindowedStream.apply(AllWindowFunction)
>
> 【Window】reduce：返回当前窗口中最后一个reduced的value
>
> 【Window】fold：返回当前窗口中最后一个folded的value
>
> 【Window】sum、min、minBy、max、maxBy

> union：DataStream*-->DataStream
>
> 【如果一个stream，union他自己的话，那么在result stream中每个element将出现两次】

> 【Window】join：DataStream,DataStream-->DataStream：在指定的key和公共window上join两个dataStream：
>
> ```scala
> dataStream.join(otherStream)
>         .where(<key selector>)
>         .equalTo(<key selector>)
>         .window(TumblingEventTimeWindow.of(Time.seconds(5)))
>         .apply(...)
> ```
>
> 【Window】coGroup：DataStream,DataStream-->DataStream：在指定的key和公共window上coGroup两个dataStream
>
> ```scala
> dataStream.coGroup(otherStream)
>         .where(0).equalTo(1)
>         .window(TumblingEventTimeWindow.of(Time.seconds(5)))
>         .apply{}
> ```

> connect：DataStream，DataStream-->ConnectedStreams
>
> ```scala
> val connectedStream = someDataStream.connect(otherStream)
> ```
>
> coMap：ConnectedStream-->DataStream
>
> ```scala
> connectedStream.map(
> 	(_:Int)=>true,
>     (_:String)=>false
> )
> ```
>
> coFlatMap：ConnectedStream-->DataStream
>
> ```scala
> connectedStream.flatMap(
> 	(_:Int)=>true,
>     (_:String)=>false
> )
> ```

> split：DataStream-->SplitStream
>
> ```scala
> val split = someDataStream.split(
> 	(num:Int)=>{
>         (num % 2) match{
>             case 0 => List("even")
>             case 1 => List("odd")
>         }
>     }
> )
> ```
>
> select：SplitStream-->DataStream
>
> ```scala
> val even = split select "even"
> val odd = split select "odd"
> val all = split.select("even","odd")
> ```

> iterate：DataStream-->IterativeStream-->DataStream
>
> ```scala
> initialStream.iterate{
> 	iteration=>{
>         val iterationBody = iteration.map{/*do something*/}
>         //前者将feedback、后者将被转发到下游
>         (iterationBody.filter(_ > 0) , (iterationBody.filter))
>     }
> }
> ```

> assignTimestamps：DataStream-->DataStream
>
> ```scala
> stream.assignTimestamps{timestampExtractor}
> ```

### One-to-one streams

### Redistributing streams

https://ci.apache.org/projects/flink/flink-docs-release-1.10/dev/stream/operators/#physical-partitioning

+ 逻辑分区

  + keyBy【repartition by hashing the key】

+ 物理分区

  + 自定义分区器

    ```scala
    dataStream.partitionCustom(partitioner,"someKey")
    dataStream.partitionCustom(partitioner,0)
    ```

  + 随机分区【随机均匀的划分元素】

    ```scala
    dataStream.shuffle()
    ```

  + 重平衡【Rebalance】【round-robin】【轮询】【**为每个分区创建相同的负载，对于存在数据倾斜的性能优化非常有用**】

    ```scala
    dataStream.rebalance()
    ```

  + 分区伸缩【Rescaling】

    + 希望将每个source的parallel实例的数据扇出到下游算子的子集中来分配负载，但是又不希望完全的Rebalance，而rebalance方法是完全的rebalance。这种方式只需要本地数据传输，而不需要通过网络传输数据，具体取决于其他配置值，如果TaskManager的slot的数量

    + eg：如果upstream operator的并行度为2，downstream operator的并行度为4，那么upstream operator的其中一个流中的元素将分发到下游的两个operator实例上，另一个流中的元素将分发到下游算子的另外两个operator的实例上。如果upstream operator并行度为4，downstream operator的并行度为2，那么upstream operator的其中两个流中的元素将分配到downstream中的一个operation上，upstream operator上的另外两个流中的元素将分配到downstream中的另外两个operation上。如果并行度不是彼此的倍数，那么一个或多个downstream operations将就有来自upstream operations的不通数量的输入

      ![Checkpoint barriers in data streams](.\image\rescale.svg)

      ```scala
      dataStream.rescale()
      ```

+ boradcast【将元素广播到每个分区】

### Setting Parallelism 【设置并行度】

> 【除了client level和system level之外】，在调用setParallelism的地方也可以调用setMaxParallelism来指定最大并行度。
>
> 默认大致为operatorParallelism + (operatorParallelism / 2)，下限为128，上限为32768
>
> 如果将最大并行度设置为非常大可能会损害性能，因为某些状态后端必须保留可随键组数量扩展的内部数据结构（键组是可伸缩状态的内部实现机制）

+ #### Operator Level：setParallelism(5)

+ #### Execution Environment Level：env.setParallelism(3)

  ```
  为所有的operators、data sources、data sinks设置并行度为3
  ```

+ #### Client Level

  + 命令行【-p】

    ```bash
    ./bin/flink run -p 10 ../examples/*WordCount-java*.jar
    ```

  + 程序中

    ```scala
    try {
        PackagedProgram program = new PackagedProgram(file, args)
        InetSocketAddress jobManagerAddress = RemoteExecutor.getInetFromHostport("localhost:6123")
        Configuration config = new Configuration()
    
        Client client = new Client(jobManagerAddress, new Configuration(), program.getUserCodeClassLoader())
    
        // set the parallelism to 10 here
        client.run(program, 10, true)
    
    } catch {
        case e: Exception => e.printStackTrace
    }
    ```

+ #### System Level

  + 配置文件flink-conf.yaml中配置parallelism.default

### Side Outputs

https://ci.apache.org/projects/flink/flink-docs-release-1.10/dev/stream/side_output.html

+ 定义OutputTag：

  ```
  val outputTag:OutputTag[String] = OutputTag[String]("side-output")
  ```

+ 支持Side Output的Functions

  + ProcessFunction
  + KeyedProcessFunction
  + CoProcessFunction
  + KeyedCoProcessFunction
  + ProcessWindowFunction
  + ProcessAllWindowFunction

+ eg

  ```scala
  val input:DataStream[Int] = ...
  val outputTag = OutputTag[String]("side-output")
  val mainStream = input.process(new ProcessFunction[Int,Int]{
  	override def processElement(
          value:Int,
          ctx:ProcessFunction[Int,Int]#Context,
          out:Collector[Int]):Unit={
          out.collect(value)
          ctx.output(outputTag,"sideout-" + String.valueOf(value))
      }
  })
  val outputTag = OutputTag[String]("side-output")
  val mainStream = ...
  val sideOutputStream:DataStream[String] = mainDataStream.getSideOutput(outputTag)
  ```

  

## Iterations

+ ### Iterate

  + ```scala
    initialStream.iterate{
    	iteration=>{
            val iterationBody = iteration.map{/*do something*/}
            //前者将feedback、后者将被转发到下游
            (iterationBody.filter(_ > 0) , (iterationBody.filter))
        }
    }
    ```

  + Bulk Iteration

    ```scala
    val initialStream = env.fromElements(0)
    val count = initialStream.iteration(10000){
        iterationInput: DataSet[Int]=>{
            val result = iterationInput.map{i=>{
                val x = Math.random()
                val y = Math.randow()
                i + (if (x*x + y*y < 1) 1 else 0)
            }}
        }
    }
    val result = count.map{c => c * 4 / 10000}
    ```

    

+ ### Delta Iterate

## Windows

https://flink.apache.org/news/2015/12/04/Introducing-windows.html

### Lifecycle

> 创建：当一个属于该窗口的元素达到的时候，该窗口被创建
>
> 移除：时间【事件时间或processing时间】+ 指定的延迟时间【allowed lateness】时窗口被完全移除
>
> Flink保证只删除基于时间的窗口，而不删除其他类型的窗口【global window】

### Keyed Windows&No-Keyed Windows

> **Keyed Windows是多个任务并行执行**
>
> **Non-Keyed-Windows不会将原始流切分为多个逻辑流且整个窗口逻辑将由单个任务执行【with parallelism 1】**

### Allowed Lateness

> 默认情况下，allowed lateness为0，也就是说在watermak之后的elements将被丢弃
>
> 如果指定了allowed lateness = 5，那么在watermark到达窗口end之后，将在5之内到达的元素添加到这个窗口，根据使用的trigger，迟到但是未丢弃的数据可能造成窗口重新计算【EventTimeTrigger】

> **将迟到的元素作为一个side output**
>
> ```scala
> val lateOutputTag = OutputTag[T]("late-data")
> val input:DataStream[T] = ...
> val result = input
> 				.KeyBy(...)
> 				.window(...)
> 				.allowedLateness(<time>)
> 				.sideOutputLateData(lateOutputTag)
> 				.<windowed transformmation>(<window function>)
> val lateStream = result.getSideOutput(lateOutputTag)
> ```

### 连续窗口操作 【Consecutive Windowed Operations】

> ```scala
> val input:DataStream[Int] = ...
> val resultPerKey = input.keyBy(...)
> 					  .window(TumblingEventTimeWindow.of(Time.seconds(5)))
> 					  .reduce(new Summer())
> val globalResults = resultPerKey.windowAll(TumblingEventTimeWindow.of(Time.seconds(5))).process(new TopKWindowFunction())
> ```

### Triggers

+ onElement()：每个元素到达窗口时触发
+ onEventTime()：当注册的event-time-timer触发的时候触发
+ onProcessingTime()：当注册的processing-time-timer触发时候触发
+ onMerge()：与有状态的触发器有关，当两个触发器的相应窗口合并时【例如会话窗口】合并两个触发器的状态
+ clear()：移除相应窗口执行时所需的任何操作
+ 注意事项：
  + 前三个方法的返回值将决定如何处理调用事件，动作如下
    + CONTINUE：什么都不做
    + FIRE：触发窗口计算
    + PURGE：清除窗口的元素
    + FIRE_AND_PURGE：触发计算之后清除窗口的元素
      + purge只是简单的移除窗口的内容并且保留有关窗口和任何触发器状态的任何潜在元信息
  + 所有的这些方法可以用来注册processing或event-time timer来执行以后的操作
+ 默认触发器
  + EventTimeTrigger：基于event-time，使用watermark衡量
  + ProcessingTimeTrigger：基于processing-time
  + CountTrigger：当窗口的元素数量到达指定的值时触发
  + PurgingTrigger：将其他的trigger作为参数并将它转换为清除触发器
  + 自定义触发器：需参考Trigger抽象类
  + 注意事项
    + 所有的event-time window assigner都有个默认的EventTimeTrigger，当watermarker超过窗口的结束点时触发
    + GlobalWindow默认触发器时NeverTrigger，永远不fire，因此当时用GlobalWindow时需要自定义一个trigger

> 前两者比较高效，当每个窗口的元素到达时，Flink可以增量的聚合他们
>
> ProcessWindowFunction持有一个包含窗口全部元素的Iterable及窗口的元数据，但是他没有前两者高效，因为在函数调用之前Flink必须缓冲这个窗口全部的元素。可以结合ReduceFunction、Aggregate Function、FoldFunction来同时获取增量聚合及窗口的元数据来缓解这种不够高效的方式

### Window Functions

+ ReduceFunction【高效】

  ```scala
  input.keyBy(..)
  	 .window(..)
  	 .reduce{(v1,v2)=>(v1._1,v1._2+v2._2)}
  ```

+ AggregateFunction【高效】

  ```scala
  class AvgAggregator extends AggregateFunction[(String,Long),(Long,Long),Double]{
  	override def createAccumulator()=(0L,0L)
      override def add(value:(String,Long),accumulator:(Long,Long))={
          (accumulator._1+value._2,accumulator._2 + 1L)
      }
      override def getResult(accumulator:(Long,Long)) = accumulator._1 / accumulator._2
      override def merge(a:(Long,Long),b:(Long,Long))={
          (a._1+b._1,a._2+a._2)
      }
  }
  input.keyBy(...)
  	 .window(...)
  	 aggregate(new AvgAggregator)
  ```

+ FoldFunction

  ```
  不能被用在session window或其他可合并的windows
  ```

  ```scala
  input
  	.keyBy(...)
  	.window(...)
  	.fold(""){(acc,v)=>acc+v._2}
  ```

+ ProcessWindowFunction【WindowFunction是ProcessWindowFunction的旧版本】

  ```scala
  input.keyBy(...).timeWindow(Time.minutes(5)).process(new MyProcessWindowFunction())
  	
  class MyProcessWindowFunction extends ProcessWindowFunction[(String,Long),String,String,TimeWindow]{
  	def process(key:String,context:Context,input:Iterable[(String,Long)],out:Collector[String]){
          var vount = 0L
          for (in <- input){
              count = count + 1
          }
          out.collect(s"window ${context.window} count: ${count}")
      }
  }
  ```
  
  ```scala
  input.keyBy(...).timeWindow(...).reduce(
		(r1:SensorReading,r2:SensorReading) => {if (r1.value > r2.value) r2 else r1},
  		(key:String,
  		 context:ProcessWindowFunction[_,_,_,TimeWindow]#Context),
  		 minReadings:Iterable[SensorReading],
            out:Collector([Long,SensorReading]) => {
                val min = minReadings.iterator.next()
                out.collect((context.window.getStart,min))
            }
  	)
  ```
  
  ```scala
  input.keyBy(...).timeWindow(...).aggregate(new AvgAggregator(),new MyProcessWindowFunction())
  
  class AvgAggregator extends AggregateFunction[(String,Long),(Long,Long),Double]{
      override def createAccumulator() = (0L,0L)
    override def add(value:(String,Long),accumulator:(Long,Long)) = {
          (accumulator._1 + value._2, accumulator._2 + 1L)
      }
      override def getResult(accumulator:(Long,Long)) = {
          (accumulator._1 / accumulator._2)
      }
      override def merge(a:(Long,Long),b:(Long,Long)) = {
          (a._1 + b._1,a._2 + b._2)
      }
  }
  
  class MyProcessWindowFunction extends ProcessWindowFunction[Double,(String,Double),String,TimeWindow] {
  	def process(key:String,context:Context,averages:Iterable[Double],out:Collector[(String,Double)]) = {
          val average = averages.iterator.next()
          out.collect((key,average))
      }
  }
  ```
  
  ```scala
  input.keyBy(...).timeWindow(...).fold(
      	("",0L,0),
          (acc:(String,Long,Int),r:SensorReading) => {("",0L,acc._3 + 1)},
          (key:String,
           window:TimeWindow,
           counts:Iterable[(String,Long,Int)]) => {
              val count = counts.iterator.next()
            out.collect((key,window.getEnd,count._3))
          }
      )
  ```

### Evictors

> 能在触发器触发后，在window function执行之前或之后移除窗口的元素

+ ```scala
  //在window function执行之前执行
  //在window function执行之前收回的元素将不会被处理
  void evictBefore(Iterable<TimestampedValue<T>> elements,int size,W window,EvictorContext evictorContext)
  ```

+ ```scala
  //在window function执行之后执行
  void evictAfter(Iterable<TimestampedValue<T>> elements,int size,W window,EvictorContext evictContext)
  ```

+ ### 预先实现的Evictors

  + **CounterEvictor**：从窗口保留用户指定数量的元素，并从窗口缓冲区的开头丢弃其余的元素
  + **DeltaEvictor**：持有DeltaFunction和threshold，计算最后一个元素和buffer中其他元素之间的delta值，如果>=threshold,则移除相应的元素
  + **TimeEvictor**：以毫秒间隔interval作为参数，对于给定的窗口，在窗口中的元素中查找最大时间max_ts,然后删除时间戳小于【max_ts-interval】的所有元素
  + 注意事项
    + 默认情况下，所有预先实现的evictors在window function之前应用他们的逻辑
    + 指定evictor可以防止任何预聚合，因为在应用计算之前，必须将窗口的所有元素传递给evictor
    + Flink不保证窗口中元素的顺序，尽管evictor可能会从窗口的开头移除元素，但是这些元素不一定是最先到达或最后到达的元素


### Window Assigners

+ ### tumbling windows【no overlap没有重叠】

  + timeWindow(Time.minutes(1))：翻滚窗口，窗口大小为1min
  + countWindow(100)：翻滚窗口，每个窗口100个event

  ```scala
  input.keyBy(...)
  	 .window(TumblingEventTimeWindows.of(Time.seconds(5)))
  	 ....
  input.keyBy(...)
  	 .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
  	 ....
  //可以调整窗口时间偏移量，比如调整到中国的时区
  input.keyBy(...)
  	 .window(TumblingEventTimeWindows.of(Time.days(1),Times.hours(-8)))
  	 ....
  ```

+ ### sliding windows【with overlap可能有重叠】

  + countWindow(100,10)：滑动窗口，每个窗口100个元素并且每隔10个元素滑动一次

  ```scala
  input.keyBy(...)
  	 .window(SlidingEventTimeWindows.of(Time.seconds(10),Time.seconds(5)))
  	 ....
  input.keyBy(...)
  	 .window(SlidingProcessingTimeWindows.of(Time.seconds(10),Time.seconds(5)))
  	 ....
  //偏移
  input.keyBy(...)
  	 .window(SlidingEventTimeWindows.of(Time.hours(12),Time.hours(1),Times.hours(-8)))
  	 ....
  ```

+ ### session windows【时断时续的不活跃性】

  > 没有固定的start和end,在内部，session window operator为每个到达的record创建一个新的window并且如果这些窗口之间的间隔比定义的gap小的话，将合并这些接近的window。因此session window operator需要一个merge trigger和merge window function【ReduceFunction、AggregateFunction、ProcessWindowFunction】【**FoldFunction不能merge**】

  

  ```scala
  //动态间断需要实现SessionWindowTimeGapExtractor接口
  input.keyBy(...)
  	 .window(EventTimeSessionWindows.withGap(Time.minutes(10)))
  	 ....
  input.keyBy(...)
  	 .window(EventTimeSessionWindows.withDynamicGap((element)=>{
  	 	//自定义获取sessionGap
  	 }))
  	 ....
  input.keyBy(...)
  	 .window(ProcessingTimeSessionWindows.withGap(Time.minutes(10)))
  	 ....
  input.keyBy(...)
  	 .window(ProcessingTimeSessionWindows.withDynamicGap((element)=>{
  	 	//自定义获取sessionGap
  	 }))
  	 ....
  ```

+ ### global windows

  > 将具有相同key的所有元素分配给同一个全局窗口，只有在指定了自定义的触发器时，此窗口schema才有用，否则，将不会执行任何计算，因为global没有全局的结束

  

  ```
  input.keyBy(...)
  	 .window(GlobalWindows.create())
  	 ...
  ```

+ ### 自定义windowAssigner【extends WindowAssigner】

  https://flink.apache.org/news/2015/12/04/Introducing-windows.html

### Using per-window state in ProcessWindowFunction

> process()方法执行的时候，接收到Context对象有两个方法可以访问两种类型的state
>
> + globalState()：可以访问不在窗口范围内的keyed state
> + windowState()：可以访问同时作用于窗口的keyed state
>
> 注意事项：
>
> + 当使用window state的时候，需要注意当window被清除的时候也要清除这个state，这应该在clear()方法中处理

### 窗口存储估计

+ Flink会为每个窗口的每个元素创建一个副本，因此Tumbling windows会保留每个元素的一个副本，如果是Sliding Windows而且窗口直接重叠很多的话，那么每个元素将被拷贝几份，分属不同的window
+ ReduceFunction、AggregateFunction、FoldFunction可以很明显的降低存储需求，他们每个窗口只存储一个值，使用ProcessWindowFunction的话，急需要累积所有的元素
+ 使用Evictor来防止预聚合，所有的元素在被计算之前，必须先通过evictor

## Time

+ ### Ingestion Time

+ ### Processing Time

+ ### Event Time

  https://ci.apache.org/projects/flink/flink-docs-release-1.10/dev/event_timestamps_watermarks.html#assigning-timestamps

  ```scala
  val env = ExecutionEnvironment.getExecutionEnviroment
  env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
  ```

  + #### 分配时间戳

    + ##### 直接在数据源中分配

      ```scala
      override def run(ctx:SourceContext[MyType]):Unit={
          while(...){
              val next:MyType getNext()
              ctx.collectWithTimestamp(next,next.eventTimestamp)
              if(next.hasWatermarkTime){
                  ctx.emitWatermark(new Watermark(next.getWatermarkTime))
              }
          }
      }
      ```

    + ##### 通过timestamp assigner和watermark assigner

      + **With Periodic Watermarks**【周期性生成watermark】

        + 自动生成水印的时间间隔通过executionConfig.setAutoWatermarkInterval设置

      + **With Punctuated Watermarks**【根据标记决定是否生成watermark】

        + 先调用extractTimestamp给element分配一个timestamp，然后立刻、在这个element上调用checkAndGetNextWatermark，之前分配的timestamp将会传递给该方法，该方法决定是否想生成一个watermark。如果返回non-null且watermark比最新的之前的watermark大的话，新的watermark将被发射

          ```scala
          class PunctuatedAssigner extends AssignerWithPunctuatedWatermarks[MyEvent]{
              override def extractTimestamp(element:MyEvent,priviousElementTimestamp:long){
                  element.getCreationTime
              }
              override def checkAndGetNextWatermark(lastElement:MyEvent,extractTimestamp:Long){
                  if (lastElement.hasWatermarkMarker()) new Watermark(extractTimestamp) else null
              }
          }
          ```

      + ### Timestamps per Kafka Partition

        ```scala
        val kafkaSource = new FlinkKafkaConsumer09[MyType]("myTopic",schema,props)
        kafkaSource.assignTimestampAndWatermarks(new AscendingTimestampExtractor[MyType]){
            def extractAscendingTimestamp(element:MyType):Long = element.eventTimestamp
        }
        val stream:DataStream[MyType] = env.addSource(kafkaSource)
        ```

        ![Generating Watermarks with awareness for Kafka-partitions](.\image\parallel_kafka_watermarks.svg)

      + ### 预定义Timestamp Extractors--Watermark Emitters

        + Assigners with ascending timestamps

          ```scala
          val stream:DataStream[MyEvent] = ...
          val withTimestampAndWatermarks = stream.assignAscendingTimestamp(_.getCreationTime)
          ```

        + Assigners allowing a fixed amount of lateness

          ```scala
          val stream:DataStream[MyEvent] = ...
          val withTimestampAndWatermarks = stream.assignTimestampAndWatermarks(new BoundedOutofOrdernessTimestampExtractor[MyEvent])(Time.seconds(10))(_.getCreationTime)
          ```

      + 注意事项

        + **如果太频繁的生成watermark的话将降低性能，因为每个watermmark的产生将引起下游的一些计算**

## Stateful Operations

> 有状态操作的operators的state被严格的分区和分布在被这些 operators读取的流中。因此访问key-velue state只能在keyBy方法之后的keyed stream中访问，并且只能访问与当前key关联的state。将streams的key和state保持一致可以保证所有的state更新都是本地操作，从而保证一致性不会产生事务开销。这种机制允许Flink透明的重新分布状态来调整流的分区

## ProcessFunction

> 允许访问所有流式应用的所有构建块
>
> + **events【stream elements】**
> + **state【fault-tolerant，consistent，only on keyed stream】**
> + **timers【event time and processing time，only on keyed stream】**
>   + event-time-->当watermarker的时间到达或超过timer的时间戳时，调用onTimer方法
>   + processing-time-->当节点时间到达指定的timer时间后，调用onTimer方法
>   + **在onTimer调用期间，可以控制注册该timer时的key的所有的states**

## Timers

> processing-time-timers和event-time-timers内部是使用TimerService维护的，并且排队执行

> TimerService根据key和timestamp去重，因此，每个key&timestamp最多只有一个timer，如果注册了多个，那么只会执行一次

> Flink会同步执行onTimer和processingElement方法，因此不用当心当前state被修改

> Timers也是Fault Tolerant（容错的）,并且和应用的state一起被checkPoint。当错误恢复或从一个savepoint启动应用时进行恢复
>
> 如果一个processing-time-timer应该在恢复之前触发的话，那么恢复应用时将会被立即触发
>
> 除了RocksDB backend/with increment snapshots/with heap-based timer这类组合外，timer的checkpoint是异步的。大量的timer会增加checkpiont的时间，因为timers也是checkpoint state的一部分。可以参考一些减少timers的策略
>
> 可以将Timer粒度细化，比如一个包含毫秒值的时间戳，可以向下取整精确到秒，这样每个key每一秒最多只有一个timer

## State

### Using Managed Keyed State

+ ValueState<T>：update(T)、T value()

+ ListState<T>：add(T)、addAll(List<T>)、Iterable<T> get()、update(List<T>)

+ ReducingState<T>：add(T) + ReduceFunction进行聚合

+ AggregatingState<IN , OUT>：add(IN) + AggregateFunction()进行聚合

+ FoldingState<T , ACC>：add(T) + FoldFunction进行聚合【将过期，使用AggregatingState替代】

+ MapState<UK,UV>：put(UK,UV)、putAll(Map<UK,UV>)、get(UK)、entries()、keys()、values()、isEmpty()

+ 注意事项

  + 状态不一定存储在内部，但可能位与磁盘或其他位置
  + 一个state的值只与输入元素的key相关
  + 创建相应的StateDescriptor，然后使用RuntimeContext访问，因此只能在rich function中进行使用

+ eg：

  ```scala
  class CountWindowAverage extends RichFlatMapFunction[(Long,Long),(Long,Long)]{
      private var sum:ValueState[(Long,Long)] = _
      override def flatMap(input:(Long,Long),out:Collector[(Long,Long)]):Unit = {
          val tmpCurrentSum = sum.value
          val currentSum = if (tmpCurrentSum != null){
              tmpCurrentSum
          }else{
              (0L,0L)
          }
          val newSum = (currentSum._1 + 1,currentSum._2 + input._2)
          sum.update(newSum)
          if(newSum._1 >= 2){
              out.collect((input._1,newSum._2/newSum._1))
              sum.clear()
          }
      }
      override def open(parameters:Configuration):Unit = {
          sum = getRuntimeContext.getState(new ValueDescriptor([Long,Long]("average",createTypeInformation([Long,Long]))))
      }
  }
  
  object ExampleCountWindowAverage{
      val env = ExecutionEnvironment.getExecutionEnvironment
      env.fromElements(
          (1L, 3L),
          (1L, 5L),
          (1L, 7L),
          (1L, 4L),
          (1L, 2L)
      ).keyBy(_._1).flatMap(new CountWindowAverage()).print()
      
      env.execute("ExampleManagedState")
  }
  ```

+ TTL【Time-To-Live】

  + TTl可以应用在所有类型的keyed state上，也可以用在集合元素的每个元素上

    ```scala
    val ttlConfig = StateTtlConfig
                .newBuilder(Time.seconds(1))
    		   //在创建和写入时更新
     		   //OnReadAndWrite在读取和写入时更新
                .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
    		   //过期的元素【暂未clean up】不会返回
    		   //ReturnExpiredIfNotCleanUp【如果没有clean up的话将返回】
                .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
                .build
    val stateDescriptor = new ValueStateDescriptor[String]("text state",classOf[String])
    stateDescriptor.enableTimeToLive(ttlConfig)
    ```

  + 注意事项

    + 启用TTL将增加状态后端对存储资源的开销，因为需要维护value和最后一次修改的时间
    + 目前只支持对processing time引用的TTL
    + 尝试使用启用了TTL的程序从之前未启用TTL的状态中恢复可能造成兼容性失败和StateMigrationException
    + TTL不是checkpoint和savepoint的一部分，而是Flink在当前运行的Job中如何处理它的一种方式
    + 只有用户的serializer可以处理null值时，配置TTL的map state当前才支持null值。如果serializer不支持null值的话，则可以用NullableSerializer对其进行包装，但会以序列化的形式中的额外字节为代价

  + CleanUp of Expired State

    + 默认情况下，在读取一个expired的value时，将显示的删除过期的value。如果配置的状态后端支持的话，也可以在后台定期进行垃圾回收。可以在StateTtlConfig中禁用后台清理

      ```scala
      import org.apache.flink.api.common.state.StateTtlConfig
      val ttlConfig = StateTtlConfig
                          .newBuilder(Time.seconde(1))
                          .disableCleanupInbackground.build
      ```

    + 细粒度控制后台cleanup

      + Heap State Backend依赖于增量清理，RocksDB后端使用压缩过滤器进行后台清理

      + 此外,可以在执行full snapshot是激活清理，浙江减少快照的大小。本地状态在当前实现下并不会清除，但在从上一个快照还原时将不包括已删除的过期状态。可以在StateTtlConfig中配置

      + 单独配置

        ```scala
        import org.apache.flink.api.common.state.StateTtlConfig
        import org.apache.flink.api.time.Time
        //此配置不适用与RocksDB状态后端中的增量检查点
        //对于现有Job，此清理策略可以在StateTtlConfig中随时启用或停用
        val ttlConfig = StateTtlConfig.newBuilder(Time.seconds(1)).cleanupFullSnapshot.build
        ```

      + 增量清除

        + 以增量方式触发某些state项的清除。可以在对每个state进行访问或处理的回调中进行触发。如果这个策略在某个state上是激活状态，那么存储后端会为这个state的所有entries保留一个lazy全局迭代器。每次增量清理被触发，这个迭代器将被激活。被遍历到的state entries将被检查，如果过期了将被clean up

          ```scala
          import org.apache.flink.api.common.state.StateTtlConfig
          //param_1：每次清理触发时选中的状态条目数，每次状态访问都会触发它
          //param_2：每个记录处理时是否触发
          //默认Heap Backend每次检查5个state entries且在记录处理时不触发清理
          val ttlConfig = StateTtlConfig.newBuilder(Time.seconds(1)).cleanupIncreamentally(10,true).build
          ```

        + 注意事项

          + 如果没有访问state或处理记录的情况发生，那么过期的state将一直存在
          + 增量clean up将会增加记录处理的延迟
          + 目前只支持Heap State Backend增量清除，RocksDB无效
          + 如果Heap State Backend使用同步snapshotting，则全局Iterator在迭代的时候讲保留所有Keys的副本，因为他的实现不支持并发修改。因此此功能将增加内存消耗，而异步snapshot不存在该问题
          + 对于现有作业，此清理策略可以在StateTtlConfig中的任何时候激活或停用，eg：savepoint重新启动

      + RocksDB cleanup

        + 如果使用RocksDB state backend，将调用特定于Filnk的压缩Filter进行后台清理。RocksDB定期运行异步压缩来合并状态更新以减少存储。Flink压缩Filter使用TTL检查state entries，并排除过期的值

          ```scala
          import org.apache.flink.api.common.state.StateTtlConfig
          //RocksDB压缩过滤器在每次处理一定数量的state entries之后，从Flink查询当前时间戳来检查过期时间。可以将自定义的值传递给cleanupInRocksdbCompactFilter(n),频繁的更新时间戳可以提高清理速度但是会降低压缩【compaction】的性能，因为它使用JNI调用本地方法。默认的后台清理实在每次处理1000个state entries之后查询当前时间戳
          //通过激活FlinkCompactionFilter的DEBUG级别，可以从RocksDB Filter的native code中激活调试日志【log4j.logger.org.rocksdb.FlinkCompactionFilter=DEBUG】
          val ttlConfig = StateTtlConfig.newBuilder(Time.seconds(1)).cleanupInRocksdbCompactFilter(1000).build
          ```

        + 注意事项

          + 在compaction期间调用TTL filter将降低compaction的速度。TTL filter必须解析最后一次访问的时间戳来检查每个正在压缩的key的state entries的到期时间，在集合state类型的情况下，也会对每个存储的元素进行检查
          + 如果此功能与具有非固定字节长度元素的list state一起使用，则native TTL filter必须在每个状态项(其中至少第一个元素已经过期)上通过JNI调用额外的Flink java类型的serializer来确定下一个未过期元素的偏移量offset
          + 对于现有作业，次清理策略可以通过StateTtlConfig在任何时间激活或停用，eg：从savepoint重启

+ #### State in the scala DataStream API

  + 函数需要从一个Option中获取ValueState当前的值，并且必须返回一个将用于更新state的值

    ```scala
    val stream:DataStream[(String,Int)] = ...
    val counts:DataStream[(String,Int)] 
    = stream.keyBy(_._1).mapWithState(
        (int:(String,Int),count:Option[Int])=>{
            count match{
                case Some(c) => ((in._1,c),Some(c + in._2))
                case None => ((in._1,0),Some(in._2))
            }
        }
    )
    ```

### Using Managed Operator State

> 为了使用managed operator state，一个stateful function可以实现更通用的CheckpointedFunction接口或者ListCheckpointed<T extends Serializable>接口

+ ### CheckpointedFunction

  + 这个接口提供了访问对具有不同重新分发约束的non-keyed state的两种方法

    + void snapshotState(FunctionSnapshotContext context) throws Exception
    + void initializeState(FunctionInitializationContext context) throws Exception

  + 当执行checkpoint的时候，snapshotState()方法将会被调用，相反，当user-defined function初始化的时候【function第一次初始化或当函数从之前的checkpoint恢复的时候】，initializeState()方法将被调用。鉴于此，initializeState()方法不仅是初始化不同类型状态的地方，而且也是包含state恢复逻辑的地方

  + 目前list-style managed operator state是支持的。state应该是可以序列对象的List，彼此独立，因此可以在rescaling的时候重新分布。也就是说说，这些对象是可以可以重新分布non-keyed state的最佳粒度。根据访问state的方法，定义了如下redistribution的schema【分配模式】

    + Even-split redistribution：每个operator返回了一个state元素的List,整个状态在逻辑上是所有List的串联。当restor或redistribution的时候，列表被均匀的划分为尽可能多的子列表，因为有并行operator。每个operator获取一个子列表，子列表可以为空，也可以包含一个或多个元素。eg：对于并行度1，operator的checkpoint的state包含ele1，ele2，将并行度增加到2时，ele1可能会在operator_0中，ele2将转到operator_2中

    + Union redistribution：每个operator将返回一个state元素的一个List。整个状态在逻辑上是所有List的串联，在restor或redistribution时，每个operator获得完整的state元素的List

    + eg:

      ```scala
      class BufferingSink(threshold: Int = 0) extends SinkFunction[(String,Int)] with CheckpointedFunction{
          @transient
          private var checkpointedState:ListState[(String,String)] = _
          private var bufferedElements = ListBuffer[(String,Int)]()
          
          override def invoke(value: (String,Int), context: Context):Unit = {
              bufferedElements += value
              if (bufferedElements.size == threshold){
                  for (element <- bufferedElements){
                      //send it to the sink
                  }
                  bufferedElements.clear()
              }
          }
          //ListState将清除前一个检查点包含的所有对象，然后填充我们要checkpoint的对象
          override def snapshotState(context: FunctionSnapshotContext): Unit = {
              checkpointedState.clear()
              for(element <- bufferedElements){
                  checkpointedState.add(element)
              }
          }
          //FunctionInitializationContext用来初始化non-keyed "containers",这些是ListState类型的container，也就是non-keyed state对象在checkpoint时存储的位置
          override def initializeState(context: FunctionInitializationContext):Unit={
              val descriptor = new ListStateDescriptor[(String,Int)](
              	"buffered-elements",
                  TypeInformation.of(new TypeHint[(String,Int)](){})
              )
              //方法名称约定了包含其再分配模式及状态结构
              //getUnionListState(descriptor)表明使用了union distribution schema
              //getListState(descriptor)表明了使用了even-split redistribution schema
              checkpointedState = context.getOperatorStateStore.getListState(descriptor)
              //在初始化container之后，使用isRestored()方法我们是否正从一个failure中恢复。如果是的话，比如我们正在从failure中恢复，那么恢复逻辑将被应用
              if(context.isRestored){
                  for(element <- checkpointedState.get()){
                      bufferedElements += element
                  }
              }
          }
      }
      ```

    + **另外，keyed state也可以在initializeState()方法中初始化，可以使用提供的FunctionInitializationContext完成**

+ ### ListCheckpointed

  > 这个接口是CheckpointedFunction的一个更有有限的个体，他只支持列表样式的状态，在恢复是只支持Even-split redistribution schema。也需要实现两个方法
  >
  > ```scala
  > List<T> snapshotState(long checkpointId,long timestamp) throws Exception
  > void restoreState(List<T> state) throws Exception
  > ```
  >
  > 在snapshotState方法执行时，operator将返回一个对象列表用来checkpoint
  >
  > restoreState方法执行时使用这样的列表来进行恢复,如果state是not re-partitionable，那么可以一直返回一个Collections.singleList(MY_STATE)
  >
  > eg:【Stateful Source Functions】
  >
  > ```scala
  > class CounterSource extends RichParallelSourceFunction[Long] with ListCheckpoint[Long]{
  >     @volatile
  >     private var isRunnint = true
  >     private var offset = 0L
  >     
  >     override def run(ctx: SourceFunction.SourceContext[Long]): Unit = {
  >         val lock = ctx.getCheckpointLock
  >         while(isRunning){
  >             lock.synchronized({
  >                 ctx.collect(offset)
  >                 offset += 1
  >             })
  >         }
  >     }
  >     
  >     override def cancel(): Unit = isRunning = false
  >     
  >     override def restoreState(state: util.List[Long]): Unit = {
  >         for (s <- state){
  >             offset = s
  >         }
  >     }
  >     
  >     override def snapshotState(checkpointId:Long,timestamp:Long):util.List[Long] = {
  >         Collections.singletonList(offset)
  >     }
  > }
  > ```

+ **小提示**：当一个checkpoint被Flink完全确认时，一些operators可能需要这些信息来与外界通信，在这种情况下，可以参考【org.apache.flink.runtime.state.CheckpointListener】接口

### Broadcast State Pattern

> 场景：来自一个流的数据需要广播到所有的下游任务，这些这些任务存储在本地，并用于处理另一个流上的所有的输入元素。eg：一个低吞吐的流包含一组规则，然后使用另外一个六中的所有元素来评估这些规则。
>
> 和其他operator state的区别在于：
>
> + 是map格式
> + 仅适用具有broadcasted stream和一个non-broadcasted stream的operator
> + 这样的operator可以有多个不同名称的broadcast states

> API
>
> ```scala
> //如果non-broadcasted stream是non-keyed stream
> public abstract class BroadcastProcessFunction<IN1,IN2,OUT> extends BaseBroadcastProcessFunction{
>     public abstract void processElement(IN1 value,ReadOnlyContext ctx,Collector<OUT> out) throws Exception;
>     public abstract void processBroadcastElement(IN2 value,Context ctx,Collector<OUT> out) throws Exception;
> }
> //如果non-broadcasted stream是keyed stream
> public abstract class KeyedBroadcastProcessFunction<KS,IN1,IN2,OUT>{
>     public abstract void processElement(IN1 value,ReadOnlyContext ctx,Collector<OUT> out) throws Exception;
>     public abstract void processBroadcastElement(IN2 value,Context ctx,Collector<OUT> out) throws Exception;
>     public void onTimer(long timestamp,OnTimerContext ctx,Collector<OUT> out) throws Exception;
> }
> ```
>
> processElement和processBroadcastElement两个方法共同点：
>
> + 都允许访问广播状态【ctx.getBroadcastState(MapStateDescriptor<K,V> stateDescriptor)】
> + 都允许查询element的时间戳【ctx.timestamp】
> + 都可以获取当前watermark【ctx.currentWatermark】
> + 都可以获取当前processing-time【ctx.currentProcessingTime】
> + 都可以将元素输出到side-outputs【ctx.output(OutputTag<X> outputTag,X value)】
>
> 不同点：
>
> + broadcasted端有读写访问权限，non-broadcast端有只读权限。原因是Flink中没有跨任务通信，因此，为了保证broadcast state中的内容在所有operator的并行实例中是一样的，只给了broadcast端读写权限，可以在所有任务中看到相同的元素，并且在所有的任务中，这一端每个传入的元素上的计算都是相同的，忽略此规则将破坏state的一致性，导致结果不一样，并且常常难以调试

> KeyedBroadcastProcessFunction比BroadcastProcessFunction功能多的地方：
>
> + 在processElement方法中的ReadOnlyContext可以访问底层timer service，它允许注册事件和/或处理processing-time timers。当timer触发时，onTimer方法将结合OnTimerContext被调用,OntimerContext暴露了和ReadOnlyContext plus相同的功能:
>   + 能够询问触发的timer是event-time timer还是processing-time timer
>   + 能查询与timer关联的key
> + processBroadcastElement方法中的Context包含applyToKeyedState(StateDescriptor<S, VS> stateDescriptor, KeyedStateFunction<KS,S> function).这允许注册一个KeyedStateFunction以应用于与提供的stateDescriptor相关联的所有的keys的states

> eg： 
>
> ```scala
> val ruleStateDescriptor:MapStateDescriptor = new MapStateDescriptor[(String,Rule)]("RulesBroadcastState",BasicTypeInfo.STRING_TYPE_INFO,TypeInformation.of(new TypeHint<Rule>() {}))
> val ruleBroadcastStream:BroadcastStream = ruleStream.broadcast(ruleStateDescriptor)
> val output:DataStream[String] = colorPartitionedStream.connect(ruleStream)
> .process(
>     //keyed stream的key
>     //non-broadcast stream中元素的类型
>     //broadcast stream中元素的类型
>     //结果类型
>     new KeyedBroadcastProcessFunction<Color,Item,Rule,String>(){
>     	private final MapStateDescriptor<String,List<Item>> mapStateDesc = new MapStateDescriptor<>(
>             "items",
>             BasicTypeInfo.STRING_TYPE_INFO,
>             new ListTypeInfo<>(Item.class)
>         )
>         private final MapStateDescriptor<String,Rule> ruleStateDescriptor = new MapStateDescriptor<>(
>             "RUlesBroadcastState",                                                                       BasicTypeInfo.STRING_TYPE_INFO,
>             TypeInformation.of(new TypeHint<Rule>(){})
>         )
>         
>         @Override
>         public void processBroadcastElement(Rule rule,
>                                            Context ctx,
>                                            Collector<String> out) throws Exception{
>             ctx.getBroadcastState(ruleStateDescriptor).put(value.name,value)
>         }
>         @Override
>         public void processElement(Item value,
>                                   ReadOnlyContext ctx,
>                                   Collector<String> out) throws Exception{
>             final MapState<String,List<Item>> state = getRuntimeContext().getMapState(mapStateDesc);
>             final Shape shape = value.getShape();
>             for (Map.Entry<String,Rule> entry : ctx.getBroadcastState(ruleStateDescriptor).immutableEntries()){
>                 final String ruleName = entry.getKey();
>                 final Rule rule = entry.getValue();
>                 
>                 List<Item> stored = state.get(ruleName);
>                 if(stored == null){
>                     stored = new ArrayList();
>                 }
>                 if(shape == rule.second && !stored.isEmpty()){
>                     for(Item i: stored){
>                         out.collect("MATCH: " + i + " - " + value);
>                     }
>                 }
>                 if(shape.equals(rule.first)){
>                     stored.add(value);
>                 }
>                 if(stored.isEmpty()){
>                     state.remove(ruleName);
>                 }else{
>                     state.put(ruleName,stored);
>                 }
>             }
>         }
> 	}
> )
> ```

> 注意事项：
>
> + 任务之间没有通信
> + broadcast state下的事件顺序可能因任务而异
> + 所有的任务会checkpoint他们的broadcast state
> + broadcast state在运行时是保存在内存中，应相应的进行内存配置，这适用于所有operator states

## Checkpoints for Fault Tolerance

https://ci.apache.org/projects/flink/flink-docs-release-1.10/internals/stream_checkpointing.html

+ 使用流回放和checkpoint的组合来实现容错

+ checkpoint和每个input stream相应的点及每个operator相应的state相关

+ stream dataflow可以从检查点恢复，同时还可以通过还原operators的state和从checkpoint重放event来保持一致性【exactly-once】

+ checkpoint interval是用恢复时间【需要重放的事件数】来权衡在执行期间的容错开销的一种方法

### 前提条件

+ 可以在一定时间内重放记录的持久化的数据源【Kafka、RabbitMQ或HDFS...】
+ 持久化存储State系统，通常为分布式文件系统

### 启用和配置

```scala
val env = StreamExecutionEnvironment.getExecutionEnvironment
//启用+checkpoint-interval
env.enableCheckpointint(1000)
//全局配置：配置文件中配置
state.checkpoints.dir: hdfs:///checkpoints/
//为每个Job配置
env.setStateBackend(new RocksDBStateBackend("hdfs:///checkpoints-data"))
//模式
env.getCheckpointConfig.setCheckpointMode(CheckpointMode.EXACTLY_ONCE)
//checkpoint直接至少间隔时间
env.getCheckpointConfig.setMinPauseBetweenCheckpoints(500)
//如果该时间内checkpoint没有完成就丢弃
env.getCheckpointConfig.setCheckpointTimeout(60000)
//如果再checkpoint中发生了错误，为了防止任务失败，checkpoint将被拒绝
env.getCheckpointConfig.setFailTasksOnCheckpointingErrors(false)
//只允许同时运行一个检查点
env.getCheckpointConfig.setMaxCurrentCheckpoints(1)
```

```scala
val config = env.getCheckpointConfig
//Job取消的时候，保留checkpoint
config.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)
//Job取消的时候，删除checkpoint，只有失败的时候，检查点才可用
config.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION)
```

### 目录结构

```
/user-defined-checkpoint-dir
    /{job-id}
        |
        + --shared/
        + --taskowned/
        + --chk-1/
        + --chk-2/
        + --chk-3/
        ...
        
 SHARED：可能是多个检查点的一部分
 TASKOWNED：是JobManager决不能删除的状态
 EXCLUSIVE：是仅属于一个checkpoint的状态
```

### 相关参数

```scala
state.backend
state.backend.async
state.backend.fs.memory-threshold
state.backend.fs.write-buffer-size
state.backend.incremental
state.backend.local-recovery
state.checkpoints.dir
state.checkpoints.num-retained
state.savepoints.dir
taskmanager.state.local.root-dirs
```

### 恢复

```bash
//如果元数据文件不是自己包含的，JobManager需要访问它所引用的数据文件
flink run -s :checkpointMetaDataPath [:runArgs]
```



### State Backend

> 默认情况下，state保存在TaskManager的内存，且checkpoints保存在JobManager的内存中，为了保持更大的state，Flink支持在其他状态后端存储和检查状态的各种方法
>
> ```scala
> SteramExecutionEnvironment.setStateBackend()
> ```

+ MemoryStateBackend【default】

  + 将数据作为对象保存在Java Heap上。Key/Value state和Window operator使用hash tables存储值和触发器等等

  + 在checkpoint时，状态后端将为state生成快照并将快照【？】作为确认消息的一部分发送给JobManager，然后JobManager也将它也存储在Heap中

  + 默认异步snapshots，如果禁用异步：

    ```
    new MemoryStateBackend(Max_MEM_STATE_SIZE,false)
    ```

  + 场景限制

    + 默认state内存限制大小为5MB
    + 无论配置的state最大大小为多少，都不能比akka框架的大小大
    + 聚合state的大小必须适合JobManager的大小

+ FsStateBackend

  + 将运行中的数据保存在TaskManager的内存中，在checkpoint时，即将state快照写入到配置的文件系统中

  + 默认异步snapshots

    ```scala
    val path="hdfs://namenode:40010/flink/checkpoints"
    val path="file:///data/flink/checkpoints"
    new FsStateBackend(path,false)
    ```

  + 适用场景

    + Job with large state，long windows，large key-value states
    + 所有的高可用设置

+ RocksDBStateBackend【支持增量checkpoint】

  > https://ci.apache.org/projects/flink/flink-docs-release-1.10/ops/state/state_backends.html

  > State的数量只受限于磁盘空间的大小，和FsStateBackend将State保存在内存相比可以保存大量的state，然而，这也意味着使用此状态后端后将降低可以实现的最大的吞吐量。从该状态后端的所有读写都必须经过序列化和反序列化来检索和存储状态，这也比从基于内存的状态后端低效

  > 增量检查点建立在（通常是多个）以前的检查点上，Flink利用RocksDB内部的压实机制，这种机制会随着时间的推移而自我巩固，因此Flink中的增量检查点历史不会无限增长，旧的检查点最终会被包含并被自动修剪

  > 和全量检查点相比，增量检查点的恢复时间可能更长或更短
  >
  > + 如果网络带宽是瓶颈，从增量检查点恢复可能需要更长的时间，因为这需要读取更多的数据（增量数据）
  >
  > + 如果CPU或IOP是瓶颈，从增量检查点恢复可能更快，因为从增量检查点恢复不需要根据Flink的规范key-value快照格式重建RocksDB Tables【在savepoint和full checkpoints是需要的】
  >
  > 配置
  >
  > + ```
  >   flink-conf.yaml: state.backend.incremental: true
  >   或
  >   RocksDBStateBackend backend = new RocksDBStateBackend(checkpointDirURI, true)
  >   ```

  + 将运行时数据保存在RocksDB中，该数据库（默认情况下）存储在TaskManager的数据目录中，在checkpoint时，整个RocksDB将被checkpoint到配置的文件系统中

  + 默认异步snapshots

  + 场景限制

    + 每个Key和每个value支持的最大大小为2^31字节，在RocksDB的一些操作中，比如ListState可以将值积累到>2^31,然后在下次检索时将会失败。这是RocksDB JNI目前的一个限制

  + 适用场景

    + Job with large state，long windows，large key-value states
    + 所有的高可用设置

  + 注意事项

    + 需要检查在taskExecutor上为RocksDB配置的内存

  + 配置

    ```scala
    val env = StreamExecutionEnvironment.getExecutionEnvironment()
    env.setStateBackend(new FsStateBackend("hdfs://namenode:40010/flink/checkpoints"))
    ```

    ```xml
    <!-- 如果在代码中没有RocksDB相关的代码，则不必引入此依赖 -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-statebackend-rocksdb_2.11</artifactId>
        <version>1.10.0</version>
        <scope>provided</scope>
    </dependency>
    ```

    ```xml
    flink-conf.yaml
    state.backend: filesystem
    state.checkpoints.dir: hdfs://namenode:40010/flink/checkpoints
    ```

    

### 注意事项

> **Flink目前没有为迭代的Job提供处理保证，如果在迭代job中启用checkpoint将产生异常。为了在迭代程序上强制检查点，可以在启用检查点时设置一个特殊标记：**
>
> ```
> env.enableCheckpointint(interval,CheckpointMode.EXACTLY_ONCE,force=true)
> ```

## Batch on Streaming

> 在批处理程序上不使用容错机制，因为可以重放数据流来实现。这会增加恢复的成本，但是会降低常规处理的成本，因为这避免了检查点

## Savepoint

> https://ci.apache.org/projects/flink/flink-docs-release-1.10/ops/state/savepoints.html

### 区别和联系

> Savepoint
>
> + 被用户created、owned、deleted
> + 用于有计划的手动的备份和恢复
>   + 更改Flink的版本了
>   + 更改Job Graph了
>   + 更改并行度了
>   + fork了新的job了
>   + 等等
> + 必须在作业终止后存在，保存点的生成和恢复成本可能更高一些，并且更关注可移植性和对上述提到的作业的更改的支持

> Checkpoint
>
> + 提供恢复机制防止意料之外的作业失败
> + 生命周期由Flink管理。被Flink created、owned、released，不需要用户参与
> + 作为一种恢复和周期性触发的方法，Checkpoint的实现有两个主要涉及目标
>   + 轻量级
>   + 尽可能快的恢复
> + 一般在用户终止job之后清除相关的Checkpoint数据【除了显示配置为保留Checkpoint】

> 检查点和保存点的当前实现基本上使用相同的代码并产生相同的格式。不过，目前有一个例外，我们可能会在未来引入更多的差异 。例外情况是带有RocksDB状态后端的增量Checkpoint，他们正在使用一些RocksDB的内部格式，而不是Flink的本地保存点格式。与Savepoint相比，这使他们那成为更轻量级checkpoint机制的一个实例

### Assigning Operator IDs

> 如果可能在未来升级程序，那么强烈建议手动设置Operators的IDs。如果没有手动指定的话，他们将会被自动生成，只要这些ID不改变，就可以从Savepoint恢复，但是ID的生成取决于程序的结构，并且对程序更改敏感，建议手动设置Operators的IDs
>
> ```scala
> DataStream<String> stream = env.
>   // Stateful source (e.g. Kafka) with ID
>   .addSource(new StatefulSource())
>   .uid("source-id") // ID for the source operator
>   .shuffle()
>   // Stateful mapper with ID
>   .map(new StatefulMapper())
>   .uid("mapper-id") // ID for the mapper
>   // Stateless printing sink
>   .print(); // Auto-generated ID
> ```

### Savepoint State

> 可以认为Savepoint为一个包含每个stateful operator的一个Operator ID --> State的一个映射
>
> ```plain
> Operator ID | State
> ------------+------------------------
> source-id   | State of StatefulSource
> mapper-id   | State of StatefulMapper
> ```

### Operations

> 使用命令行触发Savepoint、使用Savepoint取消job、从Savepoint恢复、清除Savepoint
>
> Flink >= 1.2.0,也可以从WebUI中恢复了

## Memory Management

https://ci.apache.org/projects/flink/flink-docs-release-1.10/ops/memory/mem_detail.html#overview

### Task Executors

https://ci.apache.org/projects/flink/flink-docs-release-1.10/ops/memory/mem_setup.html#managed-memory

![Simple memory model](.\image\memory-task-executor.svg)

参数

```
Total Flink memory (taskmanager.memory.flink.size)
Total process memory (taskmanager.memory.process.size)
```

## 重启策略

https://ci.apache.org/projects/flink/flink-docs-release-1.10/dev/task_failure_recovery.html

### Fixed Dealy Restart Strategy

> 给定重启次数，每次重启之间间隔固定的时间，如果超过最大尝试次数，作业最终将失败
>
> ```scala
> val env=ExecutionEnvironment.getExecutionEnvironment
> env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3,Time.of(10,TimeUnit.SECONDS)))
> ```
>
> flink-conf.yaml
>
> ```yaml
> restart-strategy: fixed-delay
> restart-strategy.fixed-delay.attempts: 3
> restart-strategy.fixed-delay.delay: 10 s
> ```

### Failure Rate Restart Strategy

> Job失败之后重启Job，当超过失败率（每个时间间隔的失败数）时，Job失败，两次连续重启尝试之间，重新启动策略将等待固定时间
>
> ```scala
> val env=ExecutionEnvironment.getExecutionEnvironment
> env.setRestartStrategy(RestartStrategies.failureRateRestart(3,Time.of(5,TimeUnit.MINUTES,Time.of(10,TimeUnit.MINUTES))))
> ```
>
> flink-conf.yaml
>
> ```yaml
> restart-strategy: failure-rate
> restart-strategy.failure-rate.max-failures-per-interval: 3
> restart-strategy.failure-rate.failure-rate-interval: 5 min
> restart-strategy.failure-rate.delay: 10 s
> ```

### No Restart Strategy

> ```scala
> val env=ExecutionEnvironment.getExecutionEnvironment
> env.setRestartStrategy(RestartStrategies.noRestart())
> ```
>
> ```yaml
> restart-strategy: none
> ```

### Fallback Restart Strategy【后备重启策略】

> 使用集群定义的重启策略，这对于启动检查点的流式应用程序很有帮助，默认情况下，如果没有定义其他重启策略，则选中Fixed Delay Stratety

## 故障转移策略

### Restart All Failover Strategy

> 如果任务失败的话，重新启动作业中所有的任务

### Restart Pipelined Region Failover Strategy

## 传递参数到Functions

+ via constructor

  ```scala
  val toFilter = env.fromElement(1,2,3)
  toFilter.filter(new MyFilter(2))
  
  class MyFilter(limit: Int) extends FilterFunction[Int]{
      override def filter(value: Int): Boolean = {
          value > limit
      }
  }
  ```

+ via withParameters(Configuration)

  ```scala
  val toFilter = env.fromElements(1,2,3)
  val c = new Configuration()
  c.setInteger("limit",2)
  
  toFilter.filter(new RichFilterFunction[Int](){
      var limit = 0
     	override def open(config:Configuration): Unit = {
          limit = config.getInteger("limit",0)
      }
      def filter(in: Int): Boolean = {
          in > limit
      }
  }).withParameter(c)
  ```

+ Globally via ExecutionConfig

  ```scala
  val env = ExecutionEnvironment.getExecutionEnvironment
  val conf = new Configuration()
  config.setString("myKey","myValue")
  env.getConfig.setGlobalJobParameters(conf)
  
  public static final class Tokenizer extends RichFlatMapFunction<String,Tuple2<String,Integer>>{
      private String myKey;
      @Override
      public void open(Configuration parameters) throws Exception{
          super.open(parameters)
          ExecutionConfig.GlobalJobParameters globalParams = getRuntimeContext().getExecutionConfig().getGlobalJobParameters();
          Configuration globConf = (Configuration)globalParams;
          myKey = globConf.getString("myKey",null)
      }
      //and more...
  }
  ```

+ via command line arguments

## 分布式缓存

> 可用于共享包含静态外部数据【如dictionaries或机器学习的回归模型】的文件

> 程序在其ExecutionEnvironment中将本地或远程文件系统【HDFS或S3】的文件或目录以特定名称注册为缓存文件。当程序执行时，Flink自动拷贝目录的文件到所有Workers的本地文件系统。然后用户函数可以查找指定名称下的文件或目录，并从Worker的本地文件系统中访问该文件或目录
>
> ```scala
> val env = ExecutionEnvironment.getExecutionEnvironment
> env.registerCachedFile("hdfs:///path/to/your/file","hdfsFile")
> env.registerCachedFile("file:///path/to/exec/file","localExecFile",true)
> 
> val input: DataSet[String] = ...
> val result: DataSet[Integer] = input.map(new MyMapper())
> //...
> env.execute()
> 
> class MyMapper extends RichMapFunction[String,Int]{
>     override def open(config:Configration):Unit = {
>         val myFile: File = getRuntimeContext.getDistributedCache.getFile("hdfsFile")
>         //...
>     }
>     
>     override def map(value: String): Int = {
>         //...
>     }
> }
> ```

## Accumulators & Counter

> job中所有的Accumulator共享一个单独的namespace，因此可以在不动的operator function中使用相同的Accumulator，Flink内部会合并所有相同name的Accumulators
>
> ```scala
> val eles2 = env.fromElements(("b", 3)).map(new RichMapFunction[(String, Int), (String, Int)] {
>    private val numEles = new IntCounter()
>    override def open(parameters: Configuration): Unit = {
>      getRuntimeContext.addAccumulator("numEles", this.numEles)
>    }
>    override def map(value: (String, Int)): (String, Int) = {
>      this.numEles.add(1)
>      value
>    }
>  })
>  val client = env.executeAsync()
>  val result = client.getJobExecutionResult(this.getClass.getClassLoader).get()
>  val value = result.getAccumulatorResult("numEles").asInstanceOf[Int]
>  System.err.println(value)
> ```
>

+ IntCounter、LongCounter、DoubleCounter
+ Histogram

## Broadcast Variables

> 广播变量允许将数据集提供给Operation的所有并行实例，以及operation的常规操作。这对于辅助数据集或依赖于数据的参数化非常有用。数据集将作为一个Collection在Operator处被访问

> 通过withBroadcastSet(DataSet,String)方法注册
>
> 通过getRuntimeContext().getBroadcastVariable(String)方法在目标operator处访问
>
> ```scala
> val toBroadcast = env.fromElements(1,2,3)
> val data = env.fromElements("a","b")
> 
> data.map(new RichMapFunction[String,String](){
>     var broadcastSet: Traversable[String] = null
>     
>     override def open(config: Configuration): Unit = {
>         broadcastSet = getRuntimeContext().getBoradcastVariable[String]("broadcastSetName").asScala
>     }
>     
>     override def map(value: String): String = {
>         //...
>     } 
> }).withBroadcastSet(toBroadcast,"broadcastSetName")
> ```

> 注意：由于broadcast variable的内容保存在每个节点的内存中，因此不应该太大。对于标量之类的值可以简单的将参数作为函数闭包的一部分或者使用withParameters方法传入相关配置

## 语义注解

> 语义注解可以用来为function的行为提供提示。他们告诉系统函数读取和计算的是函数输入的哪些字段，以及哪些未修改的字段从输入转发到输出。语义注解是加快执行速度的一种强大手段，因为他们允许系统在多个操作之间重新使用排序顺序或分区。使用语义注解最终可以避免不必要的数据shuffle或不必要的排序，并显著提高程序的性能

> 语义注解的使用是可选的。然而，在提供语义注解时保持保守是绝对重要的。不正确的语义注解将导致Flink对程序作出错误的假设，并可能最终导致不正确的结果。如果operator的行为不能明显的预测，则不应提供注解。需要仔细阅读相关文档

### Forward Fields Annotation