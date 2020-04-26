package com.ww.data.generator;

import java.util.ArrayList;
import java.util.List;

public class DataGeneratorArguments extends DataGeneratoryParamParser {

    private String[] args;

    private String dbType;

    public DataGeneratorArguments(String[] args){
        this.args = args;
        parse(args);
    }

    private ArrayList<String> extraArgs = new ArrayList<>();


    @Override
    boolean handle(String opt, String value) {
        switch (opt) {
            case DBTYPE:
                dbType = value;
                return true;
        }
        return false;
    }


    @Override
    void handleExtraArgs(List<String> subArgs) {
        extraArgs.addAll(extraArgs);
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }
}
