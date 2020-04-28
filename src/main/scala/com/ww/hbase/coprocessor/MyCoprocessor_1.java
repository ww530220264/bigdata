package com.ww.hbase.coprocessor;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.ipc.RpcServer;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;

import java.io.IOException;
import java.util.List;

/**
 * @author wangwei@huixiangtech.cn
 * @version 1.0
 * @className MyCoprocessor_1
 * @description TODO
 * @date 2020/4/28-17:42
 **/
public class MyCoprocessor_1 extends BaseRegionObserver {
    private static Configuration conf = HBaseConfiguration.create();
    private static Connection conn = null;
    private static Table table = null;

    static {
        conf.set("hbase.zookeeper.quorum", "centos7-1:2181,centos7-2:2181,centos7-3:2181");
        try {
            conn = ConnectionFactory.createConnection(conf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void prePut(ObserverContext<RegionCoprocessorEnvironment> e,
                       Put put,
                       WALEdit edit,
                       Durability durability) throws IOException {
        byte[] row = put.getRow();
        List<Cell> cells = put.get("cf".getBytes(), "star".getBytes());
        Cell cell = cells.get(0);

        Put new_put = new Put(row);
        new_put.addColumn("cf".getBytes(),"fensi".getBytes(),cell.getValueArray());
        table.put(new_put);
        conn.close();
    }
}
