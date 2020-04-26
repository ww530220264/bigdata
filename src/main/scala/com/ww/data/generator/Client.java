package com.ww.data.generator;

import java.sql.DatabaseMetaData;
import java.util.List;

public interface Client {

    public void close();

    public void send(List<Object> data);

    public DatabaseMetaData getMetadata();

}
