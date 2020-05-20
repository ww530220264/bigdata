package com.ww.hbase.main;

import org.apache.hadoop.hbase.util.Bytes;

/**
 * @author wangwei@huixiangtech.cn
 * @version 1.0
 * @className Test
 * @description TODO
 * @date 2020/5/19-21:14
 **/
public class Test {
    public static void main(String[] args) {
        long l = 1234567890L;
        byte[] l_bytes = Bytes.toBytes(l);
        String l_s = String.valueOf(l);
        byte[] bytes = Bytes.toBytes(l_s);
        System.err.println("long bytes length：" + l_bytes.length);
        System.err.println("string bytes length：" + bytes.length);
    }
}
