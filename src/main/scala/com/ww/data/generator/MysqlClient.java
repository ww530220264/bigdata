package com.ww.data.generator;

import com.alibaba.druid.pool.DruidDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MysqlClient implements Client {

    private DruidDataSource dataSource;
    private Map<Thread, Connection> connectionMap = new HashMap();

    public MysqlClient() {
        dataSource = new DruidDataSource();
        InputStream inputStream = MysqlClient.class.getClassLoader().getResourceAsStream("db.properties");
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("加载db.properties配置文件失败!");
        }
        dataSource.configFromPropety(properties);
    }

    @Override
    public void close() {
        try {
            connectionMap.get(Thread.currentThread()).close();
            connectionMap.remove(Thread.currentThread());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(Thread.currentThread().getName() + " MySQL连接关闭失败...");
        }
    }

    @Override
    public void send(List<Object> data) {
        Connection connection = getConnection();
        //...send data
    }

    @Override
    public DatabaseMetaData getMetadata() {
        Connection connection = getConnection();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet rs = metaData.getTables(null, null, "msg_center", new String[]{"TABLE"});
            while (rs.next()) {
                System.err.println(rs.getString("TABLE_NAME"));
                ResultSet rsColimns = metaData.getColumns(null, "%", rs.getString("TABLE_NAME"), "%");
                while (rsColimns.next()) {
                    System.out.println("字段名：" + rsColimns.getString("COLUMN_NAME") + "------" + "类型："
                            + rsColimns.getString("TYPE_NAME") + "------" + "长度：" + rsColimns.getString("COLUMN_SIZE")
                            + "------" + "备注：" + rsColimns.getString("REMARKS"));
                }
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(Thread.currentThread().getName() + " 获取数据库元数据失败...");
        }
    }

    public Connection getConnection() {
        Connection connection = connectionMap.get(Thread.currentThread());
        if (null != connection) {
            return connection;
        }
        try {
            connection = dataSource.getConnection();
            connectionMap.put(Thread.currentThread(), connection);
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(Thread.currentThread().getName() + " 获取MySQL连接失败...");
        }
    }
}
