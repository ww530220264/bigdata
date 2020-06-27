package com.ww.juc;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadCase {
    /**
     * 两个线程交替输出1A2B3C4D5E6F...
     */
    public static void main(String[] args) throws InterruptedException {
//        func_1();
//        func_2();
//        func_3();
        func_4();
    }

    public static void func_4(){
        TransferQueue queue = new LinkedTransferQueue();

        new Thread(()->{
            for (int i = 1; i <= 26; i++){
                try {
                    System.out.print(queue.take());
                    queue.transfer(i);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(()->{
            for (char i = 'A'; i <= 'Z'; i++) {
                try {
                    queue.transfer(i);
                    System.out.print(queue.take());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    static Thread t1 = null;
    static Thread t2 = null;

    public static void func_3() {
        t1 = new Thread(() -> {
            for (int i = 1; i <= 26; i++) {
                System.out.print(i);
                LockSupport.unpark(t2);
                if (i < 26) {
                    LockSupport.park();
                }
            }
        });
        t2 = new Thread(() -> {
            for (char i = 'A'; i <= 'Z'; i++) {
                System.out.print(i);
                LockSupport.unpark(t1);
                if (i < 'Z') {
                    LockSupport.park();
                }
            }
        });
        t1.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        t2.start();
    }

    public static void func_2() {
        ReentrantLock lock = new ReentrantLock();
        Condition nums = lock.newCondition();
        Condition chars = lock.newCondition();

        new Thread(() -> {
            for (int i = 1; i <= 26; i++) {
                try {
                    lock.lock();
                    System.out.print(i);
                    chars.signalAll();
                    if (i < 26) {
                        nums.await();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }).start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            for (char i = 'A'; i <= 'Z'; i++) {
                try {
                    lock.lock();
                    System.out.print(i);
                    nums.signalAll();
                    if (i < 'Z') {
                        chars.await();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }).start();
    }

    public static void func_1() {
        Object lock = new Object();
        new Thread(() -> {
            synchronized (lock) {
                for (int i = 1; i <= 26; i++) {
                    System.out.print(i);
                    try {
                        lock.notifyAll();
                        if (i < 26) {
                            lock.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }).start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            synchronized (lock) {
                for (char i = 'A'; i <= 'Z'; i++) {
                    System.out.print(i);
                    lock.notifyAll();
                    try {
                        if (i < 'Z') {
                            lock.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }).start();
    }
}
