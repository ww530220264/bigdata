package com.ww.guava;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author wangwei@huixiangtech.cn
 * @version 1.0
 * @className Test_Futures
 * @description TODO
 * @date 2020/7/5-10:19
 **/
public class Test_Futures {

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        Future<String> future = executorService.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                System.err.println(System.currentTimeMillis());
                Thread.sleep(10000);
                return "OK";
            }
        });
        ListenableFuture<String> listenableFuture = JdkFutureAdapters.listenInPoolThread(future);
        Futures.addCallback(listenableFuture, new FutureCallback<String>() {
            @Override
            public void onSuccess(String s) {
                System.err.println(System.currentTimeMillis());
                System.err.println("成功接收到返回结果------>" + s);
            }

            @Override
            public void onFailure(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }
}
