package com.ww.data.generator;

/**
 * @author wangwei@huixiangtech.cn
 * @version 1.0
 * @className Filed
 * @description TODO
 * @date 2020/4/27-10:02
 **/
public class ColumnMeta {

    private String columnName;
    private String columnTypeName;
    private int columnSize;
    private String columnRemark;
    private int decimalDigits;
    private boolean autoIncrement;

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnTypeName() {
        return columnTypeName;
    }

    public void setColumnTypeName(String columnTypeName) {
        this.columnTypeName = columnTypeName;
    }

    public int getColumnSize() {
        return columnSize;
    }

    public void setColumnSize(int columnSize) {
        this.columnSize = columnSize;
    }

    public String getColumnRemark() {
        return columnRemark;
    }

    public void setColumnRemark(String columnRemark) {
        this.columnRemark = columnRemark;
    }

    public int getDecimalDigits() {
        return decimalDigits;
    }

    public void setDecimalDigits(int decimalDigits) {
        this.decimalDigits = decimalDigits;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    @Override
    public String toString() {
        return "ColumnMeta{" +
                "columnName='" + columnName + '\'' +
                ", columnTypeName='" + columnTypeName + '\'' +
                ", columnSize=" + columnSize +
                ", columnRemark='" + columnRemark + '\'' +
                ", decimalDigits=" + decimalDigits +
                ", autoIncrement=" + autoIncrement +
                '}';
    }
}
