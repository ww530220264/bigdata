# Start

## base

```
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
```



## conf

```
# sqoop.env.sh
export HADOOP_HOME=/usr/local/bigdata/hadoop-2.9.2
export HIVE_HOME=/usr/local/bigdata/apache-hive-1.2.2-bin
export HBASE_HOME=/usr/local/bigdata/hbase-1.3.1
```

## lib

> 复制mysql驱动到Sqoop/lib目录下

## command

#### HDFS

```
【--direct】参数可以使用mysqldump功能，使任务运行的更快速
sqoop import \
-Dorg.apache.sqoop.splitter.allow_text_splitter=true \
--direct \
--connect jdbc:mysql://centos7-3/test --username root --password Ww@123456 --table stu \
--target-dir /sqoop/test/stu
【--where】
sqoop import -Dorg.apache.sqoop.splitter.allow_text_splitter=true \
--direct \
--connect jdbc:mysql://centos7-3/test --username root --password Ww@123456 \
--table stu --where "province='湖北省'" \
--target-dir /sqoop/test/stu
【--append】:导入数据追加到目标目录
【--delete-target-dir】:如果目标目录存在，删除该目录（类似overwrite）
```

#### HIVE

```
【--hive-import】:指定数据导入到hive中的表
【--target-dir】：指定该参数，数据首先写入到该目录
【--hive-drop-import-delims】：删除string字段内的特殊字符，内部是使用正则表达式将特殊字符替换成“”
【--null-string/--null-non-string】:指定空字段的值；Sqoop默认空数据存的是“NULL”字符串，但是hive会把空解析成\N,因此当文件存储的空是默认的“NULL”字符串的话，hive就不能正常读取文件中的空值了
```

#### HBASE

```
sqoop import \
--connect jdbc:mysql://centos7-3:3306/test --username root --password Ww@123456 \
--table stu \
--hbase-create-table \
--hbase-table stu \
--column-family baseInfo --hbase-row-key id
```



### Problems

**Cannot run program "mysqldump": error=2, 没有那个文件或目录**

```
# 在安装mysql的机器上查找mysqldump文件，然后拷贝到其他机器的相同路径下：
[root@centos7-3 ~]# find / -name mysqldump
/usr/bin/mysqldump
[root@centos7-3 ~]# scp /usr/bin/mysqldump root@centos7-1:/usr/bin/
[root@centos7-3 ~]# scp /usr/bin/mysqldump root@centos7-2:/usr/bin/
```

