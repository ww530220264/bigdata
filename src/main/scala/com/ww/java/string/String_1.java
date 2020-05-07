package com.ww.java.string;

public class String_1 {
    public static void main(String[] args) {
        String s1 = "a" + "b" + "c";
        String s2 = "abc";
        System.err.println(s1 == s2);
        System.err.println(s1.equals(s2));

        String s3 = "abc";
        String tmp = "ab";
        String s4= tmp + "c";
        System.err.println(s3 == s4); // false
        System.err.println(s3.equals(s4));
    }
}
