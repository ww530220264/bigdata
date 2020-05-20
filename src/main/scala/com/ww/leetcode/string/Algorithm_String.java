package com.ww.leetcode.string;

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
        System.err.println(checkInclusion("ab", "eidbaooo"));
    }

    //  给定两个字符串 s1 和 s2，写一个函数来判断 s2 是否包含 s1 的排列。
    public static boolean checkInclusion(String s, String s2) {
        boolean flag = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (s2.indexOf(c) != -1) {
                flag = getResult(getSubStr(s, i), Character.toString(c), s2);
                if (flag) {
                    return flag;
                }
            }
        }
        return flag;
    }

    public static String getSubStr(String s, int index) {
        return s.substring(0, index) + s.substring(index + 1);
    }

    public static boolean getResult(String s, String result, String s2) {
        if (s.length() == 0) {
            return true;
        }
        int baseLength = result.length() + s.length();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            result += c;
            if (s2.indexOf(result) != -1) {
                boolean b = getResult(getSubStr(s, i), result, s2);
                if (b) {
                    return b;
                }
            }
            if (result.length() == baseLength) {
                System.err.println(result);
            }
            result = result.substring(0, result.length() - 1);
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

