package com.ww.data.generator;

import java.sql.DatabaseMetaData;

public class Main {

    public static void main(String[] args) {
        DataGeneratorArguments dataGeneratorArguments = parseArguments(args);
        System.err.println(dataGeneratorArguments.getDbType());
        Client client = null;
        try {
            client = ClientFactory.getClient(dataGeneratorArguments);
            DatabaseMetaData metadata = client.getMetadata();
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
