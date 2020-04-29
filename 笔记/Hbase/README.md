# start

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

