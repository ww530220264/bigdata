package com.ww.hbase.coprocessor.avg;

import com.google.common.base.Stopwatch;
import com.google.protobuf.ServiceException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author wangwei@huixiangtech.cn
 * @version 1.0
 * @className AvgClient
 * @description TODO
 * @date 2020/4/29-17:08
 **/
public class AvgClient {
    public static void main(String[] args) throws IOException {

        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "centos7-1:2181,centos7-2:2181,centos7-3:2181");
        Connection connection = ConnectionFactory.createConnection(conf);
        TableName stu = TableName.valueOf("stu");
        Table table = connection.getTable(stu);
        Admin admin = connection.getAdmin();
        admin.disableTable(stu);
        HTableDescriptor tableDescriptor = admin.getTableDescriptor(stu);
        String classStr = "com.ww.hbase.coprocessor.avg.AvgEndPoint";
        if (!tableDescriptor.hasCoprocessor(classStr)){
            tableDescriptor.addCoprocessor(classStr);
        }
        admin.modifyTable(stu,tableDescriptor);
        admin.enableTable(stu);
        final AVG.AvgRequest request = AVG.AvgRequest.newBuilder().setFamily("baseInfo").setColumn("age").build();
        try {
            Map<byte[], Long[]> results = table.coprocessorService(
                    AVG.AvgService.class,
                    null,  /* start key */
                    null,  /* end   key */
                    aggregate -> {
                        BlockingRpcCallback<AVG.AvgResponse> rpcCallback = new BlockingRpcCallback<>();
                        aggregate.getAvg(null, request, rpcCallback);
                        AVG.AvgResponse response = rpcCallback.get();
                        Long sum = response.hasSum() ? response.getSum() : 0L;
                        Long count = response.hasCount() ? response.getCount() : 0L;
                        Long[] r = new Long[2];
                        r[0] = count;
                        r[1] = sum;
                        return r;
                    }
            );
            long count = 0L;
            long sum = 0;
            for (Long[] r : results.values()) {
                count += r[0];
                sum += r[1];
            }
            System.err.println(((double)sum) / count);
            stopwatch.stop();
            System.err.println(stopwatch.elapsedTime(TimeUnit.SECONDS));
        } catch (
                ServiceException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
