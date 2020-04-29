package com.ww.hbase.coprocessor.sum;

import com.google.common.base.Stopwatch;
import com.google.protobuf.ServiceException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
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
 * @className SumClient
 * @description TODO
 * @date 2020/4/29-13:28
 **/
public class SumClient {
    public static void main(String[] args) throws IOException {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "centos7-1:2181,centos7-2:2181,centos7-3:2181");
        Connection connection = ConnectionFactory.createConnection(conf);
        TableName tableName = TableName.valueOf("stu");
        Table table = connection.getTable(tableName);
        final Sum.SumRequest request = Sum.SumRequest.newBuilder().setFamily("baseInfo").setColumn("age").build();
        try {
            Map<byte[], Long> results = table.coprocessorService(
                    Sum.SumService.class,
                    null,  /* start key */
                    null,  /* end   key */
                    aggregate -> {
                        BlockingRpcCallback<Sum.SumResponse> rpcCallback = new BlockingRpcCallback<>();
                        aggregate.getSum(null, request, rpcCallback);
                        Sum.SumResponse response = rpcCallback.get();

                        return response.hasSum() ? response.getSum() : 0L;
                    }
            );
            long total = 0L;
            for (Long sum : results.values()) {
                total += sum;
            }
            System.err.println(total);
            stopwatch.stop();
            System.err.println(stopwatch.elapsedTime(TimeUnit.SECONDS));
        } catch (ServiceException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
