package com.ww.data.generator;

import java.util.List;

public interface Client {

    void close();

    void send(List<List<Object>> data, RowMeta rowMeta);

    RowMeta getMetadata();

    List<List<Object>> generateData(RowMeta rowMeta, int epoch);

}
