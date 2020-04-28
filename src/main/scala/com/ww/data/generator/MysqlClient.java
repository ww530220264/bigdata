package com.ww.data.generator;

import com.alibaba.druid.pool.DruidDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.Callable;

public class MysqlClient implements Client, Callable {

    private volatile static DruidDataSource dataSource;
    private DataGeneratorArguments arguments;
    private Map<Thread, Connection> connectionMap = new HashMap();

    public MysqlClient(){}

    public MysqlClient(DataGeneratorArguments arguments) {
        this.arguments = arguments;
        if (dataSource == null) {
            synchronized (this) {
                if (dataSource == null) {
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
            }
        }
    }

    @Override
    public void close() {
        try {
            Connection connection = connectionMap.get(Thread.currentThread());
            if (null != connection) {
                connection.close();
            }
            connectionMap.remove(Thread.currentThread());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(Thread.currentThread().getName() + " MySQL连接关闭失败...");
        }
    }

    @Override
    public void send(List<List<Object>> dataList, RowMeta rowMeta) {
        Connection connection = getConnection();
        //...send data
        PreparedStatement pst = null;
        try {
            pst = connection.prepareStatement(rowMeta.getSql());
            int fileds = dataList.size();
            int dataSize = dataList.get(0).size();
            for (int i = 0; i < dataSize; i++) {
                for (int j = 0; j < fileds; j++) {
                    pst.setObject(j + 1, dataList.get(j).get(i));
                }
                pst.addBatch();
            }
            pst.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                pst.close();
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(Thread.currentThread().getName() + " PreparedStatement 关闭失败...");
            }
        }
    }

    /**
     * @return java.sql.DatabaseMetaData
     * @author wangwei@huixiangtech.cn
     * @description TODO
     * @params []
     * @date 2020/4/27 9:54
     * @version 1.0
     * TABLE_CAT String => 表类别（可为 null）
     * TABLE_SCHEM String => 表模式（可为 null）
     * TABLE_NAME String => 表名称
     * COLUMN_NAME String => 列名称
     * DATA_TYPE int => 来自 java.sql.Types 的 SQL 类型
     * TYPE_NAME String => 数据源依赖的类型名称，对于 UDT，该类型名称是完全限定的
     * COLUMN_SIZE int => 列的大小。
     * BUFFER_LENGTH 未被使用。
     * DECIMAL_DIGITS int => 小数部分的位数。对于 DECIMAL_DIGITS 不适用的数据类型，则返回 Null。
     * NUM_PREC_RADIX int => 基数（通常为 10 或 2）
     * NULLABLE int => 是否允许使用 NULL。
     * columnNoNulls - 可能不允许使用 NULL 值
     * columnNullable - 明确允许使用 NULL 值
     * columnNullableUnknown - 不知道是否可使用 null
     * REMARKS String => 描述列的注释（可为 null）
     * COLUMN_DEF String => 该列的默认值，当值在单引号内时应被解释为一个字符串（可为 null）
     * SQL_DATA_TYPE int => 未使用
     * SQL_DATETIME_SUB int => 未使用
     * CHAR_OCTET_LENGTH int => 对于 char 类型，该长度是列中的最大字节数
     * ORDINAL_POSITION int => 表中的列的索引（从 1 开始）
     * IS_NULLABLE String => ISO 规则用于确定列是否包括 null。
     * YES --- 如果参数可以包括 NULL
     * NO --- 如果参数不可以包括 NULL
     * 空字符串 --- 如果不知道参数是否可以包括 null
     * SCOPE_CATLOG String => 表的类别，它是引用属性的作用域（如果 DATA_TYPE 不是 REF，则为 null）
     * SCOPE_SCHEMA String => 表的模式，它是引用属性的作用域（如果 DATA_TYPE 不是 REF，则为 null）
     * SCOPE_TABLE String => 表名称，它是引用属性的作用域（如果 DATA_TYPE 不是 REF，则为 null）
     * SOURCE_DATA_TYPE short => 不同类型或用户生成 Ref 类型、来自 java.sql.Types 的 SQL 类型的源类型（如果 DATA_TYPE 不是 DISTINCT 或用户生成的 REF，则为 null）
     * IS_AUTOINCREMENT String => 指示此列是否自动增加
     * YES --- 如果该列自动增加
     * NO --- 如果该列不自动增加
     * 空字符串 --- 如果不能确定该列是否是自动增加参数
     * COLUMN_SIZE 列表示给定列的指定列大小。对于数值数据，这是最大精度。对于字符数据，这是字符长度。对于日期时间数据类型，这是 String 表示形式的字符长度（假定允许的最大小数秒组件的精度）。对于二进制数据，这是字节长度。对于 ROWID 数据类型，这是字节长度。对于列大小不适用的数据类型，则返回 Null。
     * <p>
     * <p>
     * 参数：
     * catalog - 类别名称；它必须与存储在数据库中的类别名称匹配；该参数为 "" 表示获取没有类别的那些描述；为 null 则表示该类别名称不应该用于缩小搜索范围
     * schemaPattern - 模式名称的模式；它必须与存储在数据库中的模式名称匹配；该参数为 "" 表示获取没有模式的那些描述；为 null 则表示该模式名称不应该用于缩小搜索范围
     * tableNamePattern - 表名称模式；它必须与存储在数据库中的表名称匹配
     * columnNamePattern - 列名称模式；它必须与存储在数据库中的列名称匹配
     * String catalog = conn.getCatalog(); //catalog 其实也就是数据库名
     * ResultSet tablesResultSet = dbMetaData.getTables(catalog,null,null,new String[]{"TABLE"});
     * while(tablesResultSet.next()){
     * String tableName = tablesResultSet.getString("TABLE_NAME");
     * }
     * tablesResultSet 中有以下列：
     * <p>
     * 复制代码
     * TABLE_CAT String => 表类别（可为 null）
     * TABLE_SCHEM String => 表模式（可为 null）
     * TABLE_NAME String => 表名称
     * TABLE_TYPE String => 表类型。典型的类型是 "TABLE"、"VIEW"、"SYSTEM TABLE"、"GLOBAL TEMPORARY"、"LOCAL TEMPORARY"、"ALIAS" 和 "SYNONYM"。
     * REMARKS String => 表的解释性注释
     * TYPE_CAT String => 类型的类别（可为 null）
     * TYPE_SCHEM String => 类型模式（可为 null）
     * TYPE_NAME String => 类型名称（可为 null）
     * SELF_REFERENCING_COL_NAME String => 有类型表的指定 "identifier" 列的名称（可为 null）
     * REF_GENERATION String => 指定在 SELF_REFERENCING_COL_NAME 中创建值的方式。这些值为 "SYSTEM"、"USER" 和 "DERIVED"。（可能为 null）
     **/
    @Override
    public RowMeta getMetadata() {
        Connection connection = getConnection();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            ResultSet rs = metaData.getTables(catalog, null, arguments.getTable(), new String[]{"TABLE"});
            System.err.println("数据库名: " + catalog);
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                ResultSet cols = metaData.getColumns(catalog, "%", tableName, "%");
                System.err.println("表名: " + tableName);
                RowMeta rowMeta = new RowMeta();
                rowMeta.setTableName(tableName);
                while (cols.next()) {
                    ColumnMeta columnMeta = new ColumnMeta();
                    columnMeta.setColumnName(cols.getString("COLUMN_NAME"));
                    columnMeta.setColumnTypeName(cols.getString("TYPE_NAME"));
                    columnMeta.setColumnSize(cols.getInt("COLUMN_SIZE"));
                    columnMeta.setColumnRemark(cols.getString("REMARKS"));
                    if ("DECIMAL".equals(columnMeta.getColumnTypeName())) {
                        columnMeta.setDecimalDigits(cols.getInt("DECIMAL_DIGITS"));
                    }
                    columnMeta.setAutoIncrement(cols.getBoolean("IS_AUTOINCREMENT"));
                    rowMeta.getColumnList().add(columnMeta);
                }
                return rowMeta;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(Thread.currentThread().getName() + " 获取数据库元数据失败...");
        }
        throw new RuntimeException(Thread.currentThread().getName() + " 获取数据库元数据失败...");
    }

    @Override
    public List<List<Object>> generateData(RowMeta rowMeta, int epoch) {
        List<List<Object>> dataList = new ArrayList<>();
        List<ColumnMeta> columnList = rowMeta.getColumnList();
        String sql1 = "INSERT INTO " + rowMeta.getTableName() + "(";
        String sql2 = "VALUES(";
        for (ColumnMeta col : columnList) {
            List<Object> list = new ArrayList<>(256);
            if (!col.isAutoIncrement()) {
                sql1 += col.getColumnName() + ",";
                sql2 += "?,";
                dataList.add(list);
                if (TypeUtils.isInt(col.getColumnTypeName())) {
                    if (TypeUtils.isPhone(col.getColumnRemark())) {
                        for (int i = 0; i < epoch; i++) {
                            String telephone = Utils.getTelephone();
                            list.add(Long.parseLong(telephone));
                        }
                        continue;
                    } else if (TypeUtils.isAge(col.getColumnRemark())) {
                        for (int i = 0; i < epoch; i++) {
                            int age = Utils.getAge();
                            list.add(age);
                        }
                        continue;
                    }
                    for (int i = 0; i < epoch; i++) {
                        long num = Utils.getNum(0, (int) Math.pow(10, col.getColumnSize()));
                        list.add(num);
                    }
                    continue;
                }
                if (TypeUtils.isString(col.getColumnTypeName())) {
                    if (TypeUtils.isName(col.getColumnRemark())) {
                        for (int i = 0; i < epoch; i++) {
                            String name = Utils.getChineseName();
                            list.add(name);
                        }
                        continue;
                    } else if (TypeUtils.isEmail(col.getColumnRemark())) {
                        for (int i = 0; i < epoch; i++) {
                            String email = Utils.getEmail(1, 8);
                            list.add(email);
                        }
                        continue;
                    } else if (TypeUtils.isIP(col.getColumnRemark())) {
                        for (int i = 0; i < epoch; i++) {
                            String ip = Utils.getIp();
                            list.add(ip);
                        }
                        continue;
                    } else if (TypeUtils.isAddress(col.getColumnRemark())) {
                        for (int i = 0; i < epoch; i++) {
                            String address = Utils.getAddress();
                            list.add(address);
                        }
                        continue;
                    } else if (TypeUtils.isHighSchool(col.getColumnRemark())) {
                        for (int i = 0; i < epoch; i++) {
                            String school = Utils.getHighSchool();
                            list.add(school);
                        }
                        continue;
                    } else if (TypeUtils.isNative(col.getColumnRemark())) {
                        for (int i = 0; i < epoch; i++) {
                            String nativeAddr = Utils.getNative();
                            list.add(nativeAddr);
                        }
                        continue;
                    } else if (TypeUtils.isProvince(col.getColumnRemark())) {
                        for (int i = 0; i < epoch; i++) {
                            String province = Utils.getProvince();
                            list.add(province);
                        }
                        continue;
                    } else if (TypeUtils.isCity(col.getColumnRemark())) {
                        int index = -1;
                        int tmpSize = rowMeta.getColumnList().size();
                        int autoIncrCount = 0;
                        List<ColumnMeta> tmpList = rowMeta.getColumnList();
                        for (int j = 0; j < tmpSize; j++) {
                            if (tmpList.get(j).isAutoIncrement()) {
                                autoIncrCount++;
                                continue;
                            }
                            if (TypeUtils.isProvince(tmpList.get(j).getColumnRemark())) {
                                index = j;
                                break;
                            }
                        }
                        index -= autoIncrCount;
                        for (int i = 0; i < epoch; i++) {
                            String city;
                            if (index < 0) {
                                city = Utils.getCity(null);
                            } else {
                                city = Utils.getCity(dataList.get(index).get(i).toString());
                            }
                            list.add(city);
                        }
                        continue;
                    }
                    for (int i = 0; i < epoch; i++) {
                        String text = Utils.getText(col.getColumnSize());
                        list.add(text);
                    }
                    continue;
                }
                if (TypeUtils.isDate(col.getColumnTypeName())) {
                    for (int i = 0; i < epoch; i++) {
                        Date date = Utils.getDate();
                        list.add(date);
                    }
                    continue;
                }
                if (TypeUtils.isDecimal(col.getColumnTypeName())) {
                    int columnSize = col.getColumnSize();
                    int decimalDigits = col.getDecimalDigits();
                    for (int i = 0; i < epoch; i++) {
                        BigDecimal decimal = Utils.getDecimal(columnSize, decimalDigits);
                        list.add(decimal);
                    }
                    continue;
                }
            }
        }
        sql1 += ")";
        sql2 += ")";
        sql1 = sql1.replace(",)", ")");
        sql2 = sql2.replace(",)", ")");
        rowMeta.setSql(sql1 + " " + sql2);
        System.err.println(Thread.currentThread().getName() + "--" + rowMeta.getSql());
        return dataList;
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

    @Override
    public Object call() {
        try {
            int i = 0;
            RowMeta metadata = getMetadata();
            while (i < 1000) {
                i++;
                List<List<Object>> lists = generateData(metadata, 500);
                send(lists, metadata);
            }
            return "OK";
        } finally {
            close();
        }

    }
}
