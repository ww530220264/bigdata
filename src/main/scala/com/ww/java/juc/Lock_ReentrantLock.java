package com.ww.java.juc;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author wangwei@huixiangtech.cn
 * @version 1.0
 * @className Lock_ReentrantLock
 * @description TODO
 * @date 2020/4/30-11:41
 **/
public class Lock_ReentrantLock {

    private static Unsafe unsafe;
    static {
        Field theUnsafe;
        try {
            theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
            //  获取静态字段偏移量
            //  获取实例字段偏移量
            System.err.println(unsafe);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private ReentrantLock lock = new ReentrantLock();

    public void a() {
        System.err.println(Thread.currentThread().getName() + "准备获取锁。。。" + System.currentTimeMillis());
        lock.lock();
        try {
            System.err.println(Thread.currentThread().getName() + "获取到锁。。。" + System.currentTimeMillis());
//            Thread.sleep(Integer.MAX_VALUE);
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
    public void b() {
        System.err.println(Thread.currentThread().getName() + "准备获取锁。。。" + System.currentTimeMillis());
        try {
            if (lock.tryLock(5, TimeUnit.SECONDS)) {
                try {
                    System.err.println(Thread.currentThread().getName() + "获取到锁。。。" + System.currentTimeMillis());
                    Thread.sleep(Integer.MAX_VALUE);
                }finally {
                    lock.unlock();
                }
            }else {
                System.err.println(Thread.currentThread().getName() + "没有获取到锁。。。" + System.currentTimeMillis());
            }
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void c() {
        System.err.println(Thread.currentThread().getName() + "准备获取锁。。。" + System.currentTimeMillis());
        lock.lock();
        try {
            System.err.println(Thread.currentThread().getName() + "获取到锁。。。" + System.currentTimeMillis());
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Lock_ReentrantLock t = new Lock_ReentrantLock();
        Thread t1 = new Thread(() -> t.a());
        t1.start();
//        new Thread(() -> t.b()).start();
        Thread t2 = new Thread(() -> t.c());
        t2.start();
        while (true){
            TimeUnit.SECONDS.sleep(10);
            System.err.println(111111111);
//            unsafe.unpark(t2);
        }
    }
}
