package com.ww.data.generator;

/**
 * @author wangwei@huixiangtech.cn
 * @version 1.0
 * @className TypeUtils
 * @description TODO
 * @date 2020/4/27-10:49
 **/
public class TypeUtils {

    public static boolean isInt(String typeName) {
        if (typeName.contains("INT") ||
                typeName.contains("BIGINT")) {
            return true;
        }
        return false;
    }

    public static boolean isDecimal(String typeName) {
        if (typeName.contains("DECIMAL")) {
            return true;
        }
        return false;
    }

    public static boolean isString(String typeName) {
        if (typeName.contains("VARCHAR") ||
                typeName.contains("CHAR") ||
                typeName.contains("TEXT")) {
            return true;
        }
        return false;
    }

    public static boolean isDate(String typeName) {
        if (typeName.contains("DATE") ||
                typeName.contains("DATETIME") ||
                typeName.contains("TIMESTAMP")) {
            return true;
        }
        return false;
    }

    public static boolean isEmail(String remark) {
        if (remark.contains("邮箱") ||
                remark.toLowerCase().contains("email")) {
            return true;
        }
        return false;
    }

    public static boolean isPhone(String remark) {
        if (remark.contains("手机") ||
                remark.contains("电话") ||
                remark.toLowerCase().contains("phone") ||
                remark.toLowerCase().contains("telephone")) {
            return true;
        }
        return false;
    }

    public static boolean isName(String remark) {
        if (remark.contains("姓名") ||
                remark.toLowerCase().contains("name")) {
            return true;
        }
        return false;
    }

    public static boolean isAge(String remark) {
        if (remark.contains("年龄") ||
                remark.toLowerCase().contains("age")) {
            return true;
        }
        return false;
    }

    public static boolean isIP(String remark) {
        if (remark.toLowerCase().contains("ip")) {
            return true;
        }
        return false;
    }

    public static boolean isAddress(String remark) {
        if (remark.contains("住址") ||
                remark.contains("地址") ||
                remark.contains("地点") ||
                remark.contains("方位") ||
                remark.toLowerCase().contains("address")) {
            return true;
        }
        return false;
    }

    public static boolean isHighSchool(String remark) {
        if (remark.contains("高校") ||
                remark.contains("大学") ||
                remark.contains("大专") ||
                remark.contains("学院") ||
                remark.toLowerCase().contains("school") ||
                remark.toLowerCase().contains("highschool")) {
            return true;
        }
        return false;
    }

    public static boolean isNative(String remark) {
        if (remark.contains("籍贯") ||
                remark.toLowerCase().contains("native")) {
            return true;
        }
        return false;
    }
    public static boolean isProvince(String remark) {
        if (remark.contains("省份") ||
                remark.contains("省") ||
                remark.toLowerCase().contains("province")) {
            return true;
        }
        return false;
    }
    public static boolean isCity(String remark) {
        if (remark.contains("城市") ||
                remark.contains("市") ||
                remark.toLowerCase().contains("city")) {
            return true;
        }
        return false;
    }
}
