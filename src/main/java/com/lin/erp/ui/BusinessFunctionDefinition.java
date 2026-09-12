package com.lin.erp.ui;

public class BusinessFunctionDefinition {
    private final String[] fieldKeys;
    private final String[] defaultValues;
    private final String[] tableColumnKeys;
    private final String[][] tableRows;
    private final String flow;
    private final String upstream;
    private final String downstream;
    private final String numberingRule;
    private final String organizationPeriod;
    private final boolean readOnly;

    public BusinessFunctionDefinition(String[] fieldKeys, String[] defaultValues,
                                      String[] tableColumnKeys, String[][] tableRows,
                                      String flow, String upstream, String downstream) {
        this(fieldKeys, defaultValues, tableColumnKeys, tableRows, flow, upstream, downstream,
                "DOC-{module}-{yyMMdd}-{sequence}", "LINOVA / FY2026-08", false);
    }

    public BusinessFunctionDefinition(String[] fieldKeys, String[] defaultValues,
                                      String[] tableColumnKeys, String[][] tableRows,
                                      String flow, String upstream, String downstream,
                                      String numberingRule, String organizationPeriod, boolean readOnly) {
        this.fieldKeys = fieldKeys;
        this.defaultValues = defaultValues;
        this.tableColumnKeys = tableColumnKeys;
        this.tableRows = tableRows;
        this.flow = flow;
        this.upstream = upstream;
        this.downstream = downstream;
        this.numberingRule = numberingRule;
        this.organizationPeriod = organizationPeriod;
        this.readOnly = readOnly;
    }

    public String[] getFieldKeys() {
        return fieldKeys;
    }

    public String[] getDefaultValues() {
        return defaultValues;
    }

    public String[] getTableColumnKeys() {
        return tableColumnKeys;
    }

    public String[][] getTableRows() {
        return tableRows;
    }

    public String getFlow() {
        return flow;
    }

    public String getUpstream() {
        return upstream;
    }

    public String getDownstream() {
        return downstream;
    }

    public String getNumberingRule() {
        return numberingRule;
    }

    public String getOrganizationPeriod() {
        return organizationPeriod;
    }

    public boolean isReadOnly() {
        return readOnly;
    }
}
