package com.ww.hbase.coprocessor;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;

import java.io.IOException;
import java.util.Iterator;
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
            table = conn.getTable(TableName.valueOf("stu_tmp"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void prePut(ObserverContext<RegionCoprocessorEnvironment> e,
                       Put put,
                       WALEdit edit,
                       Durability durability) throws IOException {
        //获取当前表名
        //String currentTableName = e.getEnvironment().getRegionInfo().getRegionNameAsString();
        byte[] row = put.getRow();
        List<Cell> cells = put.get("info".getBytes(), "name".getBytes());
        Cell cell = cells.get(0);

        Put new_put = new Put(row);
        new_put.addColumn("info".getBytes(),"name_copy".getBytes(),new String(cell.getValueArray(),cell.getValueOffset(),cell.getValueLength(),"UTF-8").getBytes());
        table.put(new_put);
        conn.close();
    }

    public static void main(String[] args) throws IOException {
        Scan scan = new Scan();
        ResultScanner scanner = table.getScanner(scan);
        Iterator<Result> iterator = scanner.iterator();
        while (iterator.hasNext()){
            Result next = iterator.next();
            List<Cell> columnCells = next.getColumnCells("info".getBytes(), "name".getBytes());
            Cell cell = columnCells.get(0);
            System.err.println(new String(cell.getValueArray(),cell.getValueOffset(),cell.getValueLength(),"UTF-8"));
        }
        table.close();
        conn.close();

    }
}
