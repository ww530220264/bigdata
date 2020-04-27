package com.ww.data.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wangwei@huixiangtech.cn
 * @version 1.0
 * @className Row
 * @description TODO
 * @date 2020/4/27-10:02
 **/
public class RowMeta {

    private String tableName;
    private String sql;

    List<ColumnMeta> columnList = new ArrayList<>();

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<ColumnMeta> getColumnList() {
        return columnList;
    }

    public void setColumnList(List<ColumnMeta> columnList) {
        this.columnList = columnList;
    }

    @Override
    public String toString() {
        String s = "Row{" + "\n";
        for (ColumnMeta col : columnList) {
            s += col.toString() + "\n";
        }
        s += "}";
        return s;
    }

}
