# start

## count

```
$HBASE_HOME/bin/hbase org.apache.hadoop.hbase.mapreduce.RowCounter 'tablename'
```

### coprocessor

```
alter 'stu_tmp', METHOD => 'table_att', 'coprocessor' => \
'hdfs://mycluster/app-jars/bigdata.jar|com.ww.hbase.coprocessor.MyCoprocessor_1|1001|'
```

