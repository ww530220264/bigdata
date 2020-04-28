package com.ww.data.generator;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DataGeneratorArguments extends DataGeneratoryParamParser {

    private ArrayList<String> extraArgs = new ArrayList<>();
    private String dbType;
    private String table;

    public DataGeneratorArguments(String[] args){
        parse(args);
        checkArgs();
    }
    public void checkArgs(){
        if (StringUtils.isBlank(dbType)){
            throw new RuntimeException("the dbType should be specified");
        }
    }
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

    public ArrayList<String> getExtraArgs() {
        return extraArgs;
    }

    public void setExtraArgs(ArrayList<String> extraArgs) {
        this.extraArgs = extraArgs;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }
}
