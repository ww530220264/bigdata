package com.ww.data.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) {
        DataGeneratorArguments dataGeneratorArguments = parseArguments(args);
        Client client = null;
        try {
            long start = System.currentTimeMillis();
            ExecutorService executorService = Executors.newCachedThreadPool();
            ExecutorCompletionService<Object> completionService = new ExecutorCompletionService<>(executorService);
            List<Callable> callables = new ArrayList<>();
            int numThread = 12;
            for (int i = 0; i < numThread; i++) {
                client = ClientFactory.getClient(dataGeneratorArguments);
                callables.add((Callable) client);
            }
            for (int i = 0; i < numThread; i++) {
                completionService.submit(callables.get(i));
            }
            for (int i = 0; i < numThread; i++) {
                System.err.println(completionService.take().get());
            }
            System.err.println((System.currentTimeMillis() - start) / 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            if (client != null) {
                client.close();
            }
        }


    }

    public static DataGeneratorArguments parseArguments(String[] args) {
        return new DataGeneratorArguments(args);
    }

}
