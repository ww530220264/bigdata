package com.ww.hbase.coprocessor;

import org.apache.commons.lang.time.StopWatch;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.client.coprocessor.AggregationClient;
import org.apache.hadoop.hbase.client.coprocessor.DoubleColumnInterpreter;
import org.apache.hadoop.hbase.client.coprocessor.LongColumnInterpreter;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.*;

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
        //获取当前表名
        String currentTableName = e.getEnvironment().getRegionInfo().getRegionNameAsString();
        table = conn.getTable(TableName.valueOf(currentTableName));
        byte[] row = put.getRow();
        List<Cell> cells = put.get("info".getBytes(), "name".getBytes());
        Cell cell = cells.get(0);
        Put new_put = new Put(row);
        byte[] destArr = new byte[cell.getValueLength()];
        System.arraycopy(cell.getValueArray(), cell.getValueOffset(), destArr, 0, cell.getValueLength());
        new_put.addColumn("info".getBytes(), "name_copy".getBytes(), destArr);
        table.put(new_put);
        conn.close();
    }

    public static void main(String[] args) throws Throwable {
//        table = conn.getTable(TableName.valueOf("stu"));
//        Get get = new Get("1827781".getBytes());
//        get.addColumn("baseInfo".getBytes(), "age".getBytes());
//        Result result = table.get(get);
//        List<Cell> cellList = result.listCells();
//        for (Cell cell : cellList) {
//            System.err.println(Long.parseLong(Bytes.toString(CellUtil.cloneValue(cell))));
//        }
//        insert();
        countRow();
    }
    /**
     * @author wangwei@huixiangtech.cn
     * @description TODO
     * @params []
     * @date 2020/4/29 15:21
     * @return void
     * @version 1.0
     **/
    public static void countRow() throws Throwable {
        Admin admin = conn.getAdmin();
        TableName stu = TableName.valueOf("stu");
        /**
         * 注册协处理器
         * 1.禁用表
         * 2.（判断是否已经存在该协处理器）修改表描述符
         * 3.启用表
         **/
        admin.disableTable(stu);
        HTableDescriptor tableDescriptor = admin.getTableDescriptor(stu);
        String classStr = "org.apache.hadoop.hbase.coprocessor.AggregateImplementation";
        if (!tableDescriptor.hasCoprocessor(classStr)){
            tableDescriptor.addCoprocessor(classStr);
        }
        admin.modifyTable(stu,tableDescriptor);
        admin.enableTable(stu);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Scan scan = new Scan();
//        Map<byte[], NavigableSet<byte[]>> familyMap = new TreeMap<>((o1, o2) -> Bytes.compareTo(o1,o2));
//        NavigableSet<byte[]> columnSet = new TreeSet<>((o1, o2) -> Bytes.compareTo(o1,o2));
//        columnSet.add("age".getBytes());
//        familyMap.put("baseInfo".getBytes(),columnSet);
//        scan.setFamilyMap(familyMap);
        // 指定扫描的列族和列
        scan.addColumn("baseInfo".getBytes(),"age".getBytes());
        AggregationClient aggregationClient = new AggregationClient(conf);
        double l = aggregationClient.avg(stu, new DoubleColumnInterpreter(), scan);
        System.err.println(l);
        stopWatch.stop();
        System.err.println(stopWatch.getTime()/1000);
    }
    public static void insert() throws IOException {
        try {
            Put put = new Put("11".getBytes());
            put.addColumn("info".getBytes(), "name".getBytes(), "wangwei_1".getBytes());
            List<Cell> cells = put.get("info".getBytes(), "name".getBytes());
            Cell cell = cells.get(0);
            byte[] row = put.getRow();
            Put newPut = new Put(row);
            byte[] destArr = new byte[cell.getValueLength()];
            System.arraycopy(cell.getValueArray(), cell.getValueOffset(), destArr, 0, cell.getValueLength());
            newPut.addColumn("info".getBytes(), "name_copy".getBytes(), destArr);
            table.put(newPut);
        } finally {
            table.close();
            conn.close();
        }
    }
}
