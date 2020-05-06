package com.ww.juc;


import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author wangwei@huixiangtech.cn
 * @version 1.0
 * @className JUC_Unsafe
 * @description TODO
 * @date 2020/4/30-13:49
 **/
public class JUC_Unsafe {
    private static Unsafe unsafe;
    private static long valueOffset1;
    private volatile static int value1;
    private int value2;
    private static int value3;
    private static long valueOffset2;
    private static int threadSize = 100;

    static {
        Field theUnsafe;
        try {
            theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
            //  获取静态字段偏移量
            valueOffset1 = unsafe.staticFieldOffset(JUC_Unsafe.class.getDeclaredField("value1"));
            System.err.println(valueOffset1);
            //  获取实例字段偏移量
            long valueOffset2 = unsafe.objectFieldOffset(JUC_Unsafe.class.getDeclaredField("value2"));
            System.err.println(valueOffset2);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        tryLock();
    }

    public static void tryLock() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1000);
        for (int i = 0; i < 1000; i++) {
            new Thread(() -> {
                lock();
                countDownLatch.countDown();
            }).start();
        }
        countDownLatch.await();
        System.err.println(value3);
    }

    //  模拟synchronized 的monitorEntere、monitorExit指令
    public static void lock() {
        unsafe.monitorEnter(JUC_Unsafe.class);
        try {
            value3++;
        } finally {
            unsafe.monitorExit(JUC_Unsafe.class);
        }
    }
    //  线程park、unpark
    public static void park_unpark() throws InterruptedException {
        Thread t1 = new Thread(() -> {
            System.err.println(currentThreadName() + "--begin--" + System.currentTimeMillis());
            unsafe.park(false, 0);
            System.err.println(currentThreadName() + "----end--" + System.currentTimeMillis());
        }, "Thread-1111");

        Thread t2 = new Thread(() -> {
            System.err.println(currentThreadName() + "--begin--" + System.currentTimeMillis());
            //  在park之前调用unpark（多次调用效果同一次），再调用park时不会阻塞
            unsafe.unpark(Thread.currentThread());
            unsafe.unpark(Thread.currentThread());
            unsafe.unpark(Thread.currentThread());
            unsafe.park(false, TimeUnit.SECONDS.toNanos(3));
            System.err.println(currentThreadName() + "----end--" + System.currentTimeMillis());
        }, "Thread-2222");
        t1.start();
        t2.start();
        TimeUnit.SECONDS.sleep(5);
        unsafe.unpark(t1);
    }

    public static void test_add() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(threadSize);
        for (int i = 0; i < threadSize; i++) {
            new Thread(() -> {
                add_1();
                countDownLatch.countDown();
            }).start();
        }
        countDownLatch.await();
        System.err.println(Thread.currentThread().getName() + "--" + value1);
    }

    public static void add_1() {
        int andAddInt = unsafe.getAndAddInt(JUC_Unsafe.class, valueOffset1, 1);
        System.err.println(Thread.currentThread().getName() + "--" + andAddInt);
    }

    public static String currentThreadName() {
        return Thread.currentThread().getName();
    }
}
