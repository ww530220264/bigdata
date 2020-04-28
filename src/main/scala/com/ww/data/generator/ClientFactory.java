package com.ww.data.generator;

public class ClientFactory {

    public static Client getClient(DataGeneratorArguments arguments) {
        switch (arguments.getDbType().toLowerCase()) {
            case "mysql":
                return new MysqlClient(arguments);
            default:
                throw new IllegalArgumentException("no data-type matches, please check your --db-type's value");
        }
    }

}
