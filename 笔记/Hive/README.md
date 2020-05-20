# Start

## base

```
-- MYSQL
CREATE TABLE `stu` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增id',
  `name` varchar(16) NOT NULL COMMENT '姓名',
  `province` varchar(16) DEFAULT NULL COMMENT '省份',
  `city` varchar(16) DEFAULT NULL COMMENT '城市',
  `school` varchar(20) NOT NULL COMMENT '大学',
  `age` int(2) NOT NULL COMMENT '年龄',
  `score` decimal(4,2) NOT NULL COMMENT 'score',
  `phone` bigint(16) NOT NULL COMMENT 'phone number',
  `email` varchar(32) DEFAULT NULL COMMENT 'email',
  `ip` varchar(32) DEFAULT NULL COMMENT 'IP',
  `address` text COMMENT 'home address',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '时间戳',
  PRIMARY KEY (`id`),
  KEY `province` (`province`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9870359 DEFAULT CHARSET=utf8;


-- HIVE
create external table if not exists stu
(
id int comment "数据ID",
name string comment "姓名",
province string comment "省份",
city string comment "城市",
school string comment "学校",
age int comment "年龄",
score float comment "分数",
phone string comment "手机号码",
email string comment "邮箱",
ip string comment "IP",
address string comment "地址",
operate_time TIMESTAMP comment "操作时间"
)
comment "学生表"
partitioned by (year string comment "年份")
row format delimited
fields terminated by ","
stored as textfile
location "/sqoop/hive/stu/";
```

## concept

变量和属性

![image-20200515165904634](.\image\变量和属性.png)

```
$ hive --define name=wangwei
hive> set name; //wangwei
hive> set hivevar:name; // wangwei

$ hive --hiveconf hive.cli.print.header=true
hive> set hive.cli.print.header; //true
检索属性名
$ hive -S -e "set" | grep warehouse
$ hive -f /xx/xx.hql

数据库
hive> hive.cli.print.current.db=true;
hive> describe database extended test_db;
表
hive> show tables in db_name;
hive> alter table table_name set tblproperties("name"="wangwei");
hive> show tblproperties table_name;
hive> create table if not exists table_name_new [like table_name]; //只复制表结构
hive> describe formatted db_name.table_name;
hive> describe [extended | formatted] db_name.table_name.col_name;
hvie> create table table_name as select * from table_name;
分区表
hive> set hive.mapred.mode=strict; //如果查询分区表，没有加上分区过滤器时，将会禁止提交这个任务
hive> show partitions table_name;
hive> show partitions table_name [partition(key1='v1',key2='v2')];
hive> describe extended table_name partition(key1='v1',key2='v2');
hive> load data [local] inpath 'path' [overwrite] into table table_name [partition(key1='v1',key2='v2')];
hive> alter table table_name add partition(key1='v1',key2='v2') [location 'path']
hive> alter table table_name partition(key1='v1',key2='v2') set location 'path';
hive> alter table line_3 add if not exists partition(year='1') location '/test-for-		       hive/par/year=1'  partition(year='2') location '/test-for-hive/par/year=2';
hive> alter table table_name drop if exists partition(year='1');
hive> alter table table_name partition(key='value') [enable|disable] no_drop; //不允许删除
hive> alter table table_name partition(key='value') [enable|disable] offline; //不允许查询

列
hive> alter table table_name change column col_name col_name_new DataType [comment 'comment'] after col_name_other;
hive> alter table table_name add columns (col_1 DataType comment '',col_2 DataType comment '');
hive> alter table table_name replace columns(
					col_1 DataType comment '',
					col_2 DataType comment '',
					col_3 DataType comment ''); //replace语句只能用于如下内置SerDe模块的表DynamicSerDe、MetadataTypedColumnsetSerDe
hive> alter table table_name set tblproperties('key'='v'); //能增加、修改属性，但无法删除已有的属性					

```

## multi insert 多路输出

```
from table table_name t
insert [overwrite | into] table table_name
partition (key='v1',key2='v2')
select * where t.col_1 = 'v1' and col_2 = 'v2'
insert [overwrite | into] table table_name
partition (key='v1',key2='v2')
select * where t.col_1 = 'v1' and col_2 = 'v2'
insert [overwrite | into] table table_name
partition (key='v1',key2='v2')
select * where t.col_1 = 'v1' and col_2 = 'v2'
.....
;
```

## dynamic partition insert 动态分区插入

```
-- 动态分区插入
insert [overwrite|into] table table_name
partition(key1,key2)
select ...,col_key_1,col_key_2
from table_name;
-- 动静结合[静态分区需要放在动态分区之前]
insert overwrite|into table table_name
partition(key1='v1',key2)
select ...,col_key_1,col_key_2
from table_name
where col_key_1 = 'v1'
```

![image-20200516100059444](.\image\动态分区插入.png)

## export 数据导出

```
-- 如果数据文件格式刚好是用户需要的
hadoop fs -cp src dest;
-- else
insert overwrite local directory 'path'
select ... from table_name;
```

## function

```
cast (col as int)
```

## aggregate function 【聚合函数】

```
-- 开启map段聚合，提升性能，但是会更消耗内存
hive> set hive.map.aggr=true;
```

![image-20200516113214398](.\image\聚合函数_1.png)

![image-20200516113308473](.\image\聚合函数_2.png)

## 表生成函数

![image-20200516114228739](E:\workspace\bigdata\笔记\Hive\image\表生成函数.png)

+ ### eg

  + ![image-20200516222132158](.\image\error_1.png)
  + ![image-20200516222229324](.\image\lateral_view.png)

## UDF

+ ADD JAR

  + ```
    添加指定jar到类路径 add jar XX.jar;
    创建临时函数 create temporary function tmp_fun as 'com.org.....';
    ```

  + ```
    describe function tmp_fun;
    ```

## GenericUDF

## UDAF

## UDTF

## 宏命令

## data type

```
struct  //struct<street:string,city:string>
map		//map<string,float>
array	//array<string
```



## command

### create

```
CREATE [TEMPORARY] [EXTERNAL] TABLE [IF NOT EXISTS] [db_name.]table_name    
  [(col_name data_type [COMMENT col_comment], ... [constraint_specification])]
  [COMMENT table_comment]
  [PARTITIONED BY (col_name data_type [COMMENT col_comment], ...)]
  [CLUSTERED BY (col_name, col_name, ...) [SORTED BY (col_name [ASC|DESC], ...)] INTO num_buckets BUCKETS]
  [SKEWED BY (col_name, col_name, ...)                  
     ON ((col_value, col_value, ...), (col_value, col_value, ...), ...)
     [STORED AS DIRECTORIES]
  [
   [ROW FORMAT row_format] 
   [STORED AS file_format]
     | STORED BY 'storage.handler.class.name' [WITH SERDEPROPERTIES (...)]  -- (Note: Available in Hive 0.6.0 and later)
  ]
  [LOCATION hdfs_path]
  [TBLPROPERTIES (property_name=property_value, ...)]   -- (Note: Available in Hive 0.6.0 and later)
  [AS select_statement];   -- (Note: Available in Hive 0.5.0 and later; not supported for external tables)
 
CREATE [TEMPORARY] [EXTERNAL] TABLE [IF NOT EXISTS] [db_name.]table_name
  LIKE existing_table_or_view_name
  [LOCATION hdfs_path];
 
data_type
  : primitive_type
  | array_type
  | map_type
  | struct_type
  | union_type  -- (Note: Available in Hive 0.7.0 and later)
 
primitive_type
  : TINYINT
  | SMALLINT
  | INT
  | BIGINT
  | BOOLEAN
  | FLOAT
  | DOUBLE
  | DOUBLE PRECISION -- (Note: Available in Hive 2.2.0 and later)
  | STRING
  | BINARY      -- (Note: Available in Hive 0.8.0 and later)
  | TIMESTAMP   -- (Note: Available in Hive 0.8.0 and later)
  | DECIMAL     -- (Note: Available in Hive 0.11.0 and later)
  | DECIMAL(precision, scale)  -- (Note: Available in Hive 0.13.0 and later)
  | DATE        -- (Note: Available in Hive 0.12.0 and later)
  | VARCHAR     -- (Note: Available in Hive 0.12.0 and later)
  | CHAR        -- (Note: Available in Hive 0.13.0 and later)
 
array_type
  : ARRAY < data_type >
 
map_type
  : MAP < primitive_type, data_type >
 
struct_type
  : STRUCT < col_name : data_type [COMMENT col_comment], ...>
 
union_type
   : UNIONTYPE < data_type, data_type, ... >  -- (Note: Available in Hive 0.7.0 and later)
 
row_format
  : DELIMITED [FIELDS TERMINATED BY char [ESCAPED BY char]] [COLLECTION ITEMS TERMINATED BY char]
        [MAP KEYS TERMINATED BY char] [LINES TERMINATED BY char]
        [NULL DEFINED AS char]   -- (Note: Available in Hive 0.13 and later)
  | SERDE serde_name [WITH SERDEPROPERTIES (property_name=property_value, property_name=property_value, ...)]
 
file_format:
  : SEQUENCEFILE
  | TEXTFILE    -- (Default, depending on hive.default.fileformat configuration)
  | RCFILE      -- (Note: Available in Hive 0.6.0 and later)
  | ORC         -- (Note: Available in Hive 0.11.0 and later)
  | PARQUET     -- (Note: Available in Hive 0.13.0 and later)
  | AVRO        -- (Note: Available in Hive 0.14.0 and later)
  | JSONFILE    -- (Note: Available in Hive 4.0.0 and later)
  | INPUTFORMAT input_format_classname OUTPUTFORMAT output_format_classname
 
constraint_specification:
  : [, PRIMARY KEY (col_name, ...) DISABLE NOVALIDATE ]
    [, CONSTRAINT constraint_name FOREIGN KEY (col_name, ...) REFERENCES table_name(col_name, ...) DISABLE NOVALIDATE 
```

## optimize 【调优】

+ ### join 优化

  + 当对3个或3个以上的表进行join连接时，如果每个on子句都使用相同的连接键的话，那么只会产生一个MR job
  + hive假定查询中最后一个表示最大的表。在对每行记录进行连接操作时，他会尝试将其他表缓存起来，然后扫描最后的那个表进行计算，因此需要保证查询中的表的大小从左到右是依次增加的【或者使用标记----select /*+STREAMTABLE(s) \*/ s.ymd from stocks s ....,此时hive将尝试将表stocks作为驱动表，即使其在查询中不是位与最后面的】
  + map-side join
    + 如果所有的表中只有一张表是小表，那么可以在最大的表通过mapper的时候将小表完全放到内存中。hive可以在map端执行连接过程（成为map-side Join），可以使用【select /*+ MAPJOIN(d)  */】标记来指示哪个表是小表
    + 也可以通过设置属性【hive.auto.convert.join=true】启用优化
    + 也可以配置能够使用这个优化的表的大小【hive.mapjoin.smalltable.filesize=25000000】
    + 不支持RIGHT OUT JOIN 和 FULL OUTER JOIN
  + 分桶表
    + 如果所有的表中的数据都是分桶的，且是按照ON语句中的键进行分桶的，而且其中一张表的分桶个数必须是 另一张表的分桶个数的若干倍。当满足这个条件是，hive可以在map阶段按照分桶数据进行连接。
    + 配置
      + set hive.input.format=org.apache.hadoop.hive.ql.ip.bucketizedHiveInputFormat;
      + set hive.optimize.bucketmapjoin=true;
      + set hive.optimize.bucketmapjoin.sortedmerge=true;

+ ### 开启本地模式

  + ```
    set hive.exec.mode.local.auto=true
    (Cannot run job locally: Number of Input Files (= 20) is larger than hive.exec.mode.local.auto.input.files.max(= 4))
    ```

+ ### 并行执行

  + ```
    hive会将一个查询转换为一个或多个阶段，默认情况下一次执行一个阶段，如果这些阶段并非完全依赖的，那么可以开启并行执行，使更多的阶段可以并行执行，job就可能更快完成
    ```

  + ```
    set hive.exec.paralle=true
    ```

+ ### 严格模式

  + #### 限制三种类型查询

    + 分区表需要查询需要加上分区过滤条件【不允许扫描所有分区】
    + order by语句必须加上limit语句。
    + 限制笛卡尔积的查询

  + ```
    set hive.mapred.mode=strict
    ```

+ ### 调整mapper和task个数

  + ```
    hadoop df -count /path/*  查看某个目录下各个文件大小
    ```

  + ```
    set hive.exec.reducers.bytes.per.reducer=1GB 根据输入数据大小确定reducer个数
    ```

  + ```
    set mapred.reduce.tasks=3  设置使用的reducer的个数
    ```

  + ```
    set hive.exec.reducers.max=10 设置reducers的最大个数，以阻止某个查询消耗太多的reducers资源
    ```

+ ### JVM重用

  + ```
    对于很多小文件或者很多task的场景时，开启JVM重用，配置一个JVM实例在同一个job中重新使用N次
    ```

  + ```
    mapred.job.reuse.jvm.num.tasks=10
    ```

  + ```
    这个功能的缺点是，开启JVM重用将会一直占用使用到的集群资源，以便进行重用，直到任务完成后才能释放。
    ```

+ ### 索引

  + bitmap索引
    + 一般在指定的列排重后的值比较少的时候进行使用

+ ### 动态分区调整

  + 设置动态分区为严格模式：在动态分区插入时至少保证有一个分区是静态的
    + set hive.exec.dynamic.partition.mode=strict
  + 限制查询可以创建的最大分区个数
    + set hive.exec.max.dynamic.partitions=300000
    + set hive.exec.max.dynamic.partitions.pernode=10000

+ ### 推测执行

+ ### 虚拟列

```
# 动态分区模式（严格/非严格）
set hive.exec.dynamic.partition.mode=nostrict
```

## bucket table 分桶表

+ ### 命令		

  + ```
    create table table_name(col_1 String,col_2 Int) partitioned by (key String) clustered by (col_1) into 96 buckets;
    ```

  + ```
    【set hive.enforce.bucketiing=true】
    如果没有设置这个属性，那么需要我们自己设置和分桶个数相匹配的reducer个数【set mapred.reduce.tasks=96】。
    ```

+ ### 优势

  + ```
    因为桶的数量是固定的，所以他没有数据波动，桶对于数据抽样再合适不过。如果两个表都是按照user_id分桶的话，hive可以创建一个逻辑上正确的抽样。
    ```

  + ```
    分桶同时有利于执行高效的map-side join
    ```

    

## sort 排序

```
order by		全局排序
sort by			reducer内排序，局部排序
distribute by	控制根据某些字段分发到reducer
group by		控制根据某些字段分发到reducer
cluster by 		相当于cluster by same_col == distribute by same_col sort by same_col【是全排序】
```

## sample 抽样

+ ### 抽样查询

  + ```
    hive> select * from table_name tablesample(bucket x out of y on rand()) s;
    hive> select * from table_name tablesample(bucket x out of y on col_name) s;
    ```

    

+ 数据块抽样

  + ```
    select * from table_name tablesample(0.1 percent) s;
    ```

  + ```
    这种抽样方式不一定能够适合所有的文件格式。这种抽样的最小抽样单元是一个hdfs数据块，因此，如果数据大小小于普通的块大小128M的话，那么将会返回所有的行
    ```

+ 分桶表的输入裁剪

  + 如果tablesample语句中指定的列和clustered by语句中的指定的列相同，那么tablesample查询只会扫描涉及到的表的hash分区下的数据

## index 索引

## streaming

## file type 【文件格式】

### sequenceFile

### RCFile

## difference with traditional DB

+ 传统数据库
  + 是写时模式
    + 即数据库在数据写入数据库时对模式进行检查
+ Hive
  + 读时模式
    + 读取数据时，才根据模式对数据进行解析

