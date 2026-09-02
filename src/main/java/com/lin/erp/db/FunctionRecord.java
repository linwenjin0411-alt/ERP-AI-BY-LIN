package com.lin.erp.db;

public class FunctionRecord {
    private final long id;
    private final String functionCode;
    private final String[] values;

    public FunctionRecord(long id, String functionCode, String[] values) {
        this.id = id;
        this.functionCode = functionCode;
        this.values = values == null ? new String[0] : values;
    }

    public long getId() {
        return id;
    }

    public String getFunctionCode() {
        return functionCode;
    }

    public String[] getValues() {
        String[] copy = new String[values.length];
        System.arraycopy(values, 0, copy, 0, values.length);
        return copy;
    }
}
