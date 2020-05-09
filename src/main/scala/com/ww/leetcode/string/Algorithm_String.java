package com.ww.leetcode.string;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wangwei@huixiangtech.cn
 * @version 1.0
 * @className Algorithm_String
 * @description TODO
 * @date 2020/5/9-9:03
 **/
public class Algorithm_String {
    public static void main(String[] args) {
        //  无重复字符的最长子串
//        System.err.println(getMaxLengthNoRepeat("242343432`64364"));
        //  最长公共前缀
//        System.err.println(getMaxCommonPrefix(new String[]{"flower", "flow", "flight"}));
        checkInclusion("abc", null);
    }

    //  给定两个字符串 s1 和 s2，写一个函数来判断 s2 是否包含 s1 的排列。
    public static boolean checkInclusion(String s1, String s2) {
        // abcd
        String s = "";
        int length = s1.length();
        int m = length - 1;
        s = Character.toString(s1.charAt(0));
        while (m > 0) {
            int n = 1;
            String sub_s = "";
            for (int i = 0; i < s1.length(); i++) {
                if (i != 0) {
                    if (s.length() < length) {
                        s += Character.toString(s1.charAt(i));
                    }
                    if (s.length() == length) {
                        System.err.println(s);
                        sub_s = s.substring(length-1-n);
//                        int curIndex = length -
//                        while ()
                    }
                }
            }
        }
        return false;
    }

    //  最长公共前缀
    public static String getMaxCommonPrefix(String[] strs) {
        String result = "";
        if (strs.length == 0) {
            return result;
        }
        int minLength = strs[0].length();

        for (String s : strs) {
            minLength = Math.min(minLength, s.length());
        }
        String first = strs[0];
        for (int i = 0; i < minLength; i++) {
            for (int j = 1; j < strs.length; j++) {
                if (first.charAt(i) != strs[j].charAt(i)) {
                    return result;
                }
            }
            result += first.charAt(i);
        }
        return result;
    }

    //  无重复字符的最长子串
    public static int getMaxLengthNoRepeat(String s) {
        if (s.length() == 1) {
            return 1;
        }
        int max = 0;
        int count = 0;
        for (int i = 0; i < s.length() - 1; i++) {
            for (int j = i + 1; j < s.length(); j++) {
                count++;
                int existIndex = s.substring(i, j).indexOf(s.charAt(j));
                if (existIndex >= 0) {
                    max = Math.max(max, j - i);
                    if (existIndex < s.length() - 2) {
                        i = i + existIndex;
                        break;
                    } else {
                        return max;
                    }
                }
                max = Math.max(max, j - i + 1);
            }
        }
        System.err.println("运算次数：" + count);
        return max;
    }
}

