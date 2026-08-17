package com.lin.erp.db;

import java.util.ArrayList;
import java.util.List;

public class ModulePageData {
    private final String code;
    private final String titleKey;
    private final String subtitleKey;
    private final String pageType;
    private final String tableTitleKey;
    private final String processTitleKey;
    private final String focusTitleKey;
    private final String promptValue;

    private final List<String> actions = new ArrayList<String>();
    private final List<String> processSteps = new ArrayList<String>();
    private final List<Metric> metrics = new ArrayList<Metric>();
    private final List<String> tableColumns = new ArrayList<String>();
    private final List<String[]> tableRows = new ArrayList<String[]>();
    private final List<Integer> tableRowSortOrders = new ArrayList<Integer>();
    private final List<String> focusItems = new ArrayList<String>();

    public ModulePageData(String code, String titleKey, String subtitleKey, String pageType, String tableTitleKey,
                          String processTitleKey, String focusTitleKey, String promptValue) {
        this.code = code;
        this.titleKey = titleKey;
        this.subtitleKey = subtitleKey;
        this.pageType = pageType;
        this.tableTitleKey = tableTitleKey;
        this.processTitleKey = processTitleKey;
        this.focusTitleKey = focusTitleKey;
        this.promptValue = promptValue;
    }

    public static ModulePageData error(String message) {
        ModulePageData data = new ModulePageData("ERROR", "Database unavailable", message, "ERROR",
                "table.records", "panel.process", "panel.todo", null);
        data.getFocusItems().add(message);
        return data;
    }

    public String getCode() {
        return code;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getSubtitleKey() {
        return subtitleKey;
    }

    public String getPageType() {
        return pageType;
    }

    public String getTableTitleKey() {
        return tableTitleKey;
    }

    public String getProcessTitleKey() {
        return processTitleKey;
    }

    public String getFocusTitleKey() {
        return focusTitleKey;
    }

    public String getPromptValue() {
        return promptValue;
    }

    public List<String> getActions() {
        return actions;
    }

    public List<String> getProcessSteps() {
        return processSteps;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public List<Metric> getMetrics(String location) {
        List<Metric> filtered = new ArrayList<Metric>();
        for (Metric metric : metrics) {
            if (location.equals(metric.getLocation())) {
                filtered.add(metric);
            }
        }
        return filtered;
    }

    public List<String> getTableColumns() {
        return tableColumns;
    }

    public List<String[]> getTableRows() {
        return tableRows;
    }

    public List<Integer> getTableRowSortOrders() {
        return tableRowSortOrders;
    }

    public void addTableRow(int sortOrder, String[] values) {
        tableRows.add(values);
        tableRowSortOrders.add(Integer.valueOf(sortOrder));
    }

    public List<String> getFocusItems() {
        return focusItems;
    }

    public static class Metric {
        private final String location;
        private final String labelValue;
        private final String metricValue;
        private final String noteValue;
        private final String accentCode;

        public Metric(String location, String labelValue, String metricValue, String noteValue, String accentCode) {
            this.location = location;
            this.labelValue = labelValue;
            this.metricValue = metricValue;
            this.noteValue = noteValue;
            this.accentCode = accentCode;
        }

        public String getLocation() {
            return location;
        }

        public String getLabelValue() {
            return labelValue;
        }

        public String getMetricValue() {
            return metricValue;
        }

        public String getNoteValue() {
            return noteValue;
        }

        public String getAccentCode() {
            return accentCode;
        }
    }
}
