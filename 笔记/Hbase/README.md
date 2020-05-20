# start

## background

> 通用的关系型数据库无法很好的应对数据规模剧增时导致的系统扩展性和性能问题。因此业界出现了一类面向半结构化数据的存储和处理的高扩展、低延迟、低写入的系统。如键值存储系统、文档存储系统和类BigTable存储系统，这些特性各异的系统可统称为NoSql系统。

## column-oriented 【列式存储数据库】

+ ### 特点

  + 稀疏的、分布式的、持久化的、多维的映射，由行键、列键和时间戳索引
  + 行的存取操作是原子的，可以读写任意数目的列，目前不支持跨行事务和跨表事务。【支持单行事务】
  + 单元格的值可以当做计数器使用，并且能够支持原子更新。【基于此可以实现全局的、强一致的、连续的计数器】
  + 可以在服务器的地址空间中执行来自客户端的代码，支持这种功能的服务器端框架叫做【协处理器】，这个代码能直接访问服务器本地的数据，可以用于实现轻量级批处理作业或者使用表达式并给予各种操作来分析和汇总数据
  + 通过包装器集成MapReduce框架，该包装器能够将表转换为MR作业的输入源和输出目标

+ ### 优点

  + 减少IO
  + 更有利于压缩【增量压缩】【前缀压缩】
  + NULL值不需要存储

## MPP 【大规模并行处理数据库】

## 命名空间 NameSpace

> + ### 预定义的命名空间
>
>   + hbase：存放HBase内部表
>   + default
>
> + ### 命令
>
>   + ```
>     create_namespace 'my_ns'
>     ```
>
>   + ```
>     create 'my_ns:my_table','cf'
>     ```
>
>   + ```
>     drop_namespace 'my_ns'
>     ```
>
>   + ```
>     alter_namespace 'my_ns',{METHOD=>'set','PROPERTY_NAME'=>'PROPERTY_VALUE'}
>     ```

## 模式 Schema

> + ### 备注
>
>   + 当表的模式或列族的模式改变之后，会在下次Major Compaction和StoreFiles重写之后生效

## 架构 architecture

### Client

### Filters

### Master【HMaster】

+ 控制RegionServer Failover
+ 控制Region Splits
+ 运行后台进程
  + LoadBalancer
    + 均衡集群内Region的分布，以均衡集群的负载
  + CatalogJanitor
    + 周期性的检查和清理hbase:meta表

### RegionServer【HRegionServer】

> 服务和管理Regions

> 运行其他后台进程
>
> + CompactSplitThread
>   + 检查splits切片文件并处理minor compaction
> + MajorComparctionChecker
>   + 检查Major Compaction情况
> + MemStoreFlusher
>   + 周期性的刷出MemStore中的数据到StoreFile
> + LogRoller
>   + 周期性的检查WAL文件
> + Coprocessors
>   + 协处理器
> + Blcok cache
>   + 块缓存

#### Split【切分】

+ 大多数情况下应该自动切分【建议】
+ 手动切分
  + 需要了解key的分布情况
  + 可以减轻Region创建和移动负载不足的情况
  + 可以更容易执行基于时间交错的【错峰】的major compaction分散集群网络IO负载
+ 相关参数
  + hbase.regionserver.region.split.policy
  + hbase.hregion.max.filesize
  + hbase.regionserver.regionSplitLimit
  + hbase.hregion.max.filesize = 100G【用来禁用自动切分，不建议设置成LONG.MAX_VALUE】
+ **切分步骤**
  + prepare【准备阶段】【启动切分事务】【SPLIT TRANSACTION IS STARTED】
    + 【1】获取表的共享读锁，因防止在切分过程中表模式被修改
    + 【2】在zookeeper的/hbase/region-in-transition/parent-region-name节点并设置状态state为SPLITING
  + 通知Master
    + Master在/hbase/region-in-transition注册有Watcher
  + 在HDFS的parent Region目录下创建.splits文件夹
  + 【OFFLINE parent Region】
    + 【1】RegionServer关闭parent Region并在本地数据结构中标记该parent Region为OFFLINE状态
    + 【2】在该状态下，该parent Region的客户端请求会抛出【NotServingRegionException】异常，客户端会使用一些退避原则进行重试，The closing region is flushed.
  + RegionServer在.splits目录下回创建两个文件目录daughterA和daughterB并创建必要的数据。然后切分store files，即在parent Region中为每个store file创建两个引用文件【Reference files】，这些引用文件指向parent Region的文件
  + RegionServer在HDFS中创建实际的daughterA和daughter Region目录，然后把上面的引用文件分别移动到daughterA和daughterB Region目录下
  + RegionServer发送一个put请求到.META.表，在.META.表中将parent Region标记为OFFLINE并添加daughter Regions信息。当clieng浏览.META.表的时候就会发现parent Region正在Split，但是在daughter Regions出现在.META.表之前客户端是看不到的。如果该PUT请求成功之后，parent Region将被实施SPLIT。如果该PUT请求成功之前RegionServer服务挂了，Master和下一个RegionServer将打开parennt Region并且清除这个region split的脏数据。如果.META.表数据更新了，Master仍然会回滚相应的region split信息
  + RegionServer并行打开daughterA和B
  + RegionServer添加daughterA和B到.META.表中，以及相关的region信息。【包含引用parent Region的切分区域daughterA和B此时为ONLINE状态】。在此之后，客户端可以发现新的daughterA和B regions并可以向他们发送请求。
  + RegionServer更新zookeeper结点/hbase/region-in-transition/parent-region-name的状态为SPLIT，因此Master可以接收到通知。如果必要的话，balancer可以将daughter region重新分配到其他的RegionServers。【此时SPLIT TRANSACTION结束】
  + split之后,.META.表和HDFS仍然包含parent Region的引用信息。当子Region压缩重写的时候，这些引用文件将被移除。垃圾回收任务运行时会周期性的检查是否有daughter regions仍然引用parent Region的文件，如果不包含，那么parent Region将会被删除

#### Block Cache

+ 可以在RegionServer Web UI中查看相关指标

+ 计算集群内多少内存可以用来做缓存
  + num RegionServers * headSize * hfile.block.cache.size \* 0.99
+ 支持压缩【默认关闭】【hbase.block.data.cachecompressed=true】。如果RegionServer承载的数量超过了缓存的大小，【SNAPPY】压缩之后会增加吞吐量，但是会增加延迟，增加CPU利用率，增加垃圾回收负载。

+ LRUBlockCache

  + 区域划分
    + Single-access priority
      + 第一次从文件中加载的block放入该区域
    + Multi-access priority
      + single-access区域中的block中的数据被再次访问的时候，block将移动到该区域
    + In-memory priority
      + 配置列族的IN_MEMORY属性为true【HColumnDescriptor.setInMemory(true)】或者【create  't', {NAME => 'f', IN_MEMORY => 'true'}】
  + 开启需要考虑的因素
    + Working Set Size【数据集大小】
    + Catalog Tables【hbase:meta表强制缓存在block cache并且有in_memory优先级】
    + HFile Indexes【数据块的多级索引也可能被缓存在LRU】
    + Keys【因为value是和Keys-----rowKey+family qualifier+timestamp 一起存储的】
    + Bloom Filters【像HFile一样，也可能会被缓存在LRU】
  + 不适用场景
    + 【完全随机读模式】：此模式下缓存命中率很低，接近0，会浪费内存和CPU资源，如果读取大量的数据的话，可能频繁触发垃圾回收
    + 【MapReduce】：把一个表作为MR作业的输入的时候，每一行数据只会使用一次，此时没有必要缓存这些数据。可以通过扫描器设置缓存选项为false，禁用当次扫描缓存。
    + 【集群缓存大小 < 需要读取的一批数据大小】：此时缓存是不够存放整个数据的，经过一段时间，缓存被填满，继续缓存新的数据的时候就会造成频繁的写入新读取的数据，移除最少使用的缓存数据

+ BucketCache【OFF-HEAP|ON-HEAP|FILE】

  + 【如果启用该选项时，相当于启用了两层的缓存系统】

    ```
    <property>
      <name>hbase.bucketcache.ioengine</name>
      <value>offheap</value>
      <description>配置bucketcache为直接内存模式</description>
    </property>
    <property>
      <name>hfile.block.cache.size</name>
      <value>0.2</value>
      <description>配置LRUCache内存大小占堆内存大小的比例</description>
    </property>
    <property>
      <name>hbase.bucketcache.size</name>
      <value>4196</value>
      <description>bucketcache大小</description>
    </property>
    ```

+ Combine Block Cache【策略】

  + L1【LRUBlockCache】

    + 保存Meta Block，Index 和 BLOOM Blocks

  + L2【BucketCache】

    + 保存DataBlock缓存

  + 1.0之后可以设置column family的元数据和数据块存放在L1中

    + ```
      HColumnDescriptor.setCacheDataInL1(true)
      或
      hbase(main):003:0> create 't', {NAME => 't', CONFIGURATION => {CACHE_DATA_IN_L1 => 'true'}}
      ```

#### Write Ahead Log【WAL】

+ Purpose 【目的】

  > 记录HBase中所有的变化【PUT&DELETE】，是基于文件的存储，在正常情况下是没有必要的，但是当RegionServer在【已经写到HLog但】MemStore的数据刷写到磁盘之前时崩溃或者变得不可用的时候，WAL可以确保可以回放数据的改变。当然，如果数据写入WAL的时候失败了，那么整个操作就失败了

+ 正常情况下一个RegionServer一个WAL实例

+ MultiWAL【多个WAL Stream】

  > 1.0以后支持MultiWal，这允许一个RegionServer通过在底层的HDFS上使用多个传输管道并行的写入多个WAL流，这种方式可以提高数据写入阶段的吞吐量，这种并行是按Region划分的修改数据实现的，因此当前实现对提升单个Region的吞吐量是没有帮助的

  > RegionServers使用原始的WAL或者MultiWAL都能处理任意一组WAL的恢复，因此可以使用滚动重启实现零停机配置更新

  > ```
  > <property>
  >   <name>hbase.wal.provider</name>
  >   <value>multiwal</value>
  > </property>
  > ```

### Regions

### Bulk Loading

### HDFS

### Timeline-consistent High

## 服务端

+ ### write
  + 先将数据记录在提交日志（commit log）中【预写日志WAL】
  + 然后将数据写入内存的memstore中
  + 一旦memstore中保存的写入数据的累计大小超过了一定的阈值，系统会将这些数据移除内存作为HFile文件刷写到磁盘中。
  + 数据移出内存之后，系统会丢弃对应的提交日志，只保留未持久化到磁盘中的提交日志。
  + 在将数据移出memstore写入磁盘的过程中，可以非阻塞读写，通过滚动内存中的memstore也就是用新的空memstore获取更新数据，将旧的满的memstore转换成一个文件。
  + memstore中的数据以及按照rowkey排序，持久化到磁盘中的HFile时也是按照这个顺序排列的，所以不必执行排序或其他特殊处理

+ ### read

+ #### update

+ ### delete 【墓碑标记】

## 客户端

+ ### 读

  + #### 【Gets】批量读取

    + 如果批量读取中有一个错误将会导致整个get()操作终止
    + 对于批量操作中的局部错误，有一种 更为精细的处理方法，batch方法

  + #### 【getRowOrBefore】

    + 查找一个特定的行或这个请求行之前的一行
    + 需指定一个存在的列族。否则服务器端会抛出NullPointerException

+ ### 写

  + #### 缓冲区 【默认禁用】

    + setAutoFlush(false) //将自动刷写设置为false来激活写缓冲区
    + setWriteBufferSize(long writeBufferSize) //设置写缓冲区大小【默认2M】
    + 刷写
      + flushCommits() //【显示】强制将数据写到服务端【通常是不必要的】
      + put()或setWriteBufferSize()方法时自动触发，会将目前占用缓冲区的大小与用户配置的大小作比较。如果超出限制则会调用flushCommits()方法。如果缓冲区被禁用，可以设置setAutoFlush(true)，这样每次调用put方法时都会触发刷写。
      + 调用Htable类的close方法也会无条件的隐式触发刷写
    + 注意事项
      + 如果缓冲区的记录涉及到多行或多个regionServer，在与服务器通信时，会将缓冲区中的记录按regionServer分组，然后分别将各组数据传输到对应的regionServer服务器上
      + 在调用Htable.put操作时，客户端会先把所有的 put操作插入到写缓冲区中，然后隐式的调用flushCache，在插入每个put实例的时候，客户端会检查Put实例，如果检查失败(比如Put实例为空)，将抛出异常，二前面检查通过的则会被添加到缓冲区
      + 在客户端检查通过后，服务器端处理put操作，在服务器端执行失败的put实例将继续保存在客户端本地写缓冲区中，可调用HTable的getWriterBuffer方法进行访问。客户端通过异常报告远程错误，可查询操作失败、出错的原因以及重试的次数。对于错误列族，服务器端重试次数自动设置为1，因为这是不可恢复的错误
      + 无法控制服务器端执行put的顺序
    + 全局配置
      + habse-site.xml
        + hbase.client.write.buffer=10240000

  + #### compare-and-set 原子性操作

    + checkAndPut
      + ![image-20200517162431469](.\image\checkAndPut.png)
    + 注意事项
      + 该操作只能检查和修改【同一行】，与其他许多操作一样，这个操作只提供【同一行】数据的原子性保证。检查和修改分别针对不同行数据时会抛出异常

+ ### 删

  + #### api

    + ![image-20200517171914085](.\image\api_delete.png)

  + #### 【compare-and-delete】

    + ![image-20200517172627893](.\image\compareAndDelete.png)
    + 只能对同一行数据进行检查和删除

+ ### batch

  + #### 批量操作

    + 使用batch功能时，put实例不会放入到客户端写入缓冲区中，batch请求是同步的，会把操作直接发送到服务器端，这个过程没有延迟或其他操作

  + #### 可能的返回结果

    + ![image-20200517173419256](.\image\batch_result.png)

+ ### scan 扫描 

  + 扫描操作不会通过一次RPC请求返回所有匹配的行，而是以行为单位进行返回
  + 扫描器租约时长配置hbase-site.xml【hbase.regionservers.lease.period】
  + **缓存 【一次RPC请求可以获取多行数据】【面向行】**
    + **可以为少量的RPC请求次数和客户端以及服务端的内存消耗找到平衡点**
    + 可以在表的层面启用，会对这个表的所有扫描实例的缓存都会生效【HTable】
      + ![image-20200517194652208](.\image\scanner_cache.png)
    + 也可以在扫描层面启用，只会影响当前的扫描实例【scanner】
      + ![image-20200517195045369](.\image\scanner_cache_scanner.png)
    + 全局配置
      + ![image-20200517195121751](.\image\scanner_cache_global.png)

  + **批量处理【面向列】**
    + 可以让用户选择每一次scanner.next操作要取回多少列，如果一行中的列数超过了批量设置中设置的值，则可以将这一行分片，每次next返回一片
    + 如果一行的列数不能被批量设置中的值整除时，最后一次返回的result实例中会包含比较少的列
    + ![image-20200517195726232](.\image\scanner_batch.png)
  + **组合使用缓存和批量处理，可以让用户方便的控制扫描一个范围内的行键时所需要的的RPC调用次数**
    + ![image-20200517200234233](.\image\scanner_cache_and_batch.png)
  + **调用close方法释放所有由扫描器控制的资源**

## 过滤器

+ ### 比较运算符【compareFilter】

  + ![image-20200517200945373](.\image\comparator_opeator.png)

+ ### 比较器 【comparator】

  + ![image-20200517201103595](.\image\filter_comparator.png)

  + **注意事项**
    + BigComparator、RegexStringComparator和SubstringComparator只能与EQUAL、NOT_EQUAL运算符搭配使用

+ 比较过滤器

  + 行过滤器 【RowFilter】
    + 基于rowKey过滤数据
  + 列族过滤器 【FamilyFilter】
    + 基于比较列族
  + 列名过滤器 【QualifierFilter】
    + 基于列名继续筛选
  + 值过滤器 【ValueFilter】
    + 根据单元格的值进行筛选
  + 参考列过滤 【DependentColumnFilter】
    + 根据指定的列族的列的时间戳（如果指定了比较器，那么该列的单元格的值需要满足指定的条件）对其他的列进行过滤【其他列的时间戳要在参考的列的时间戳中】
    + 该过滤器与Scanner的批量处理模式不兼容，因为过滤器需要查看整行数据来决定哪些数据被过滤，使用批量处理可能会导致取到的数据中不包含参考列，因此结果有错

+ 专用过滤器

  + 单列值过滤器 【SingleColumnValueFilter】
  + 单列排除过滤器 【SingleColumnValueExcludeFilter】
  + 前缀过滤器 【PrefixFilter】
  + 分页过滤器 【PageFilter】
  + 行键过滤器 【KeyOnlyFilter】
  + 首次行键过滤器 【FirstKeyOnlyFilter】
  + 包含结束的过滤器 【InclusiveStopFilter】
  + 时间戳过滤器 【TimestampsFilter】
  + 列计数过滤器 【ColumnCountGetFilter】
  + 列分页过滤器 【ColumnPaginationFilter】
  + 列前缀过滤器 【ColumnPrefixFilter】
  + 随机行过滤器 【RandomRowFilter】

+ 附加过滤器

  + 跳转过滤器 【SkipFilter】
  + 全匹配过滤器 【WhileMatchFilter】

+ 过滤器列表

  + 组合多个过滤器的功能来实现某种效果
  + ![image-20200517203057169](.\image\FilterList.png)

+ 自定义过滤器

+ 总结

  + ![image-20200517203317248](.\image\Filter_summary.png)

## 计数器

## 协处理器

## versions 【数据版本化】

+ 默认保留3个版本的数据【降序】
+ 如果用户未指定单元格的时间戳的话，那么数据存入时有regionServer决定
+ scan和get操作只会返回最新包版本的数据
+ scan 'test', {VERSIONS=>3}

## lock 【锁】

+ ### 行锁

  + 默认的锁超时时间是一分钟，可以在hbase-site.xml中配置【扫描器超时也适用这个配置】
    + hbase.regionserver.lease.period【120000】
  + 读取数据时是不需要加锁的，因为服务器端应用了一个多版本的并发控制机制来保证行级读操作



## count

```
$HBASE_HOME/bin/hbase org.apache.hadoop.hbase.mapreduce.RowCounter 'tablename'
```

### coprocessor

```
# 装载处理器
alter 'stu_tmp', METHOD => 'table_att', 'coprocessor' => \
'hdfs://mycluster/app-jars/bigdata.jar|com.ww.hbase.coprocessor.MyCoprocessor_1|1001|'
# 卸载处理器
alter 'stu_tmp',METHOD=>'table_att_unset',NAME=>'coprocessor$1'
```

## MemStore

### 概念

+ 数据写入时，先写入HLog,然后再写入MemStore，用于写缓存

### 刷出

+ ##### 概念

  + 当系统满足一些条件时，会触发memStore的输出操作。**刷出的最小单位是region，当输出操作被触发时，所有属于同一个region的memStore都将被刷出**

+ ##### 刷出条件

  + **MemStore级别**：当一个MemStore的大小达到【hbase.hregion.memstore.flush.size】设置的大小是，同一个region的MemStores都将被刷出到磁盘
  + **Region级别**：当Region中的所有Memstore的大小达到了上限【hbase.hregion.memstore.block.multiplier * hbase.hregion.memstore.flush.size--默认2*128M=256M】，会触发MemStore刷出
  + **RegionServer级别**：当一个RegionServer上的所有的MemStore的大小总和达到【hbase.regionserver.global.memstore.upperLimit】时，按照从大到小的顺序将MemStore刷到磁盘，直到所有MemStore的大小总和降到或略低于【hbase.regionserver.global.memstore.lowerLimit】指定的大小时
  + **RegionServer HLog级别**：当一个regisonServer上的WAL日志数量达到了【hbase.regionserver.max.logs】配置的个数时，按照时间最旧的顺序将MemStore刷出到磁盘，直到WAL日志数量低于【hbase.regionserver.max.logs】配置的大小
  + **手动刷出，通过shell命令【flush 'table_name'】 或 【flush 'region_name'】**

## HFile

### 逻辑结构

+ ![image-20200519095215534](.\image\HFile逻辑结构.png)

+ Trailer
  + 这部分主要记录了HFile的基本信息、各个部分的偏移量和寻址信息
+ Load-on-open-section
  + 表示在HBase regionServer启动的时候，需要加载到内存中的数据块，包括FileInfo、Bloom Filter Block、Data Block index和Meta block index
+ scanned block section
  + 表示在HFile顺序扫描时数据块会被读取，主要包括Leaf Index Block和Bloom Block
+ non scanned block section
  + 表示在HFile顺序扫描的时候数据不会被读取，主要包括Meta Block和Intermediate Leval Data Index Block

### 物理结构

+ ![image-20200519095318847](.\image\HFile物理结构_2.png)

## 数据存储结构

+ B+
+ B-
+ LSM

## why

> **为什么不将每个region的数据更改都写到一个单独的日志文件中？**
>
> + 如果将不同表的日志提交到不同的日志文件中去的话，就需要向FS并发的写入大量文件，这种操作依赖于每个FS底层的实现，这些写入会导致大量的磁盘寻道来向不同的物理日志文件中写入数据
> + HBase遵循这个原则，同时写入太多的文件，且需要保留滚动的日志会影响系统的扩展性，这种设计最终是由底层文件系统决定的。虽然在HBase中可以替换底层文件系统，但是通常情况下安装还是会选择HDFS
> + 如果系统崩溃，那么HMaster会将日志拆分，将不同region的Log数据进行拆分，分别放到对应的region目录下，然后再将失效的region重新分配，领取到这些region的regionServer在load region的过程中，会发现有历史HLog需要处理，因此会replay HLog中的数据到memstore中，然后flush到StoreFile【也就是HFile】中，完成数据恢复

> **HBase数据时如何写入的？**
>
> + ![image-20200518135612655](.\image\HBase数据写入流程.png)
> + 客户端与Zookeeper交互获取meta元数据表，根据rowKey获取他们归属的regionServer的元数据信息，并缓存到客户端本地
> + 然后根据定位的regionServer信息对rowKey进行分组【如果是批量提交的话】，每个分组对应一次RPC请求。
> + regionServer接收到客户端的写入请求后，首先会反序列化为Put对象，然后执行各种检查，比如region是否只读，memstore大小是否超过blockingMemstoreSize等。检查完成之后就会执行如下核心操作
> + **获取行锁、Region更新共享锁**：HBase中使用行锁保证对同一行数据的更新都是互斥操作，用以保证更新的原子性，要么成功，要么失败
> + **开启写事务**：获取write number，用于实现MVCC【多版本并发控制】，实现数据的非锁定读，在保证读写一致性的前提下提高读取性能
> + **写缓存memstore**：HBase中每个列族都会对应一个store，用来存储该列族数据。每个store都会有个写缓存memstore，用于缓存写入数据。HBase不会直接将数据落盘，而是先写入缓存，等缓存满足一定的大小之后再一起落盘
> + **Append HLog**：HBase使用WAL机制保证数据可靠性，即先写入日志再写缓存，即使发生宕机，也可以通过回放HLog日志还原出原始数据。该步骤就是讲数据构造为WALEdit对象，然后顺序写入HLog日志中，此时不需要执行sync操作，0.98版本采用了新的写线程模式实现HLog日志的写入，可以使得整个数据更新新能得到极大提升
> + **释放行锁以及共享锁**
> + **Sync HLog真正sync到HDFS**：在释放行锁之后执行sync操作是为了尽量减少持锁时间，提升写性能。如果sync失败，执行回滚操作将memstore中已经写入的数据移出
> + **结束写事务**：此时该线程的更新操作才会对其他读请求可见，更新才实际生效。
> + **flush memstore**：当写缓存满64M后，会启动flush线程将数据刷新到磁盘

> **WAL机制**
>
> + WAL是一种高效的日志算法，几乎是所有非内存数据库提升写性能的不二法门，基本原理是在数据写入之前首先顺序写入日志，然后再写入缓存，等到缓存写满后在同一落盘。之所以能够提升写性能，是因为WAL将一次随机写转化为了一次顺序写+一次内存写，提升写性能的同事，WAL可以保证数据的可靠性，即在任何情况下数据不丢失。加入一次写入之后发生了宕机，即使所有缓存中的数据丢失，也可以通过恢复日志还原出丢失的数据
> + 持久化等级
>   + HBase中可以通过设置WAL的持久化等级决定是否开启WAL机制以及HLog的落盘方式。WAL的持久化等级分为如下四个等级
>     + SKIP_WAL：只写缓存，不写HLog日志
>     + ASYNC_WAL：异步将数据写入到HLog日志中
>     + SYNC_WAL：同步将数据写入日志文件，需要注意的是数据只是被写入文件系统中，并没有真正落盘
>     + FSYNC_WAL：同步将数据写入日志文件中并强制落盘。最严格的日志写入等级，可以保证数据不丢失，但是性能相对比较差
>     + USER_DEFAULT：默认如果用户没有指定持久化等级，HBase使用SYNC_WAL持久化等级
>   + put.setDurability(Durability.SYNC_WAL)

> **HFile物理结构**
>
> + ![image-20200518135906717](.\image\HFile物理结构.png)

## HBase Shell

> + VM options
>
>   + ```
>     HBASE_SHELL_OPTS="-verbose:gc -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCDateStamps \
>       -XX:+PrintGCDetails -Xloggc:$HBASE_HOME/logs/gc-hbase.log" ./bin/hbase shell
>     ```

## Compaction

> Minor Compaction

> Major Compaction
>
> + 删除过期时间【TTl】
> + 删除墓碑标记的数据【*tombstone* 】

## 避免数据热点常用技术

> + 【Salt】前缀加盐
> + 【Hashing】
> + 【Reversing the key】

## 二八法则

> **80%的业务请求都集中在20%的热点数据上，因此将这部分数据缓存起来就可以极大的提升系统的性能**

## 优化

> + 尝试最小化rowKey大小长度、colimn Family名称长度、Qualifier名称长度【因为在DataBlock Index中也会保存数据的Key，而Key中包含ColumnFamily】

## TTL

## API

+ Connections
  + 是重量级对象
  + 线程安全
+ Table
  + 轻量级对象
  + 线程不安全