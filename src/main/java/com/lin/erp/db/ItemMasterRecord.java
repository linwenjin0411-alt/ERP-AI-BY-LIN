package com.lin.erp.db;

public class ItemMasterRecord {
    private final String itemCode;
    private final String itemName;
    private final String itemType;
    private final String uom;
    private final String plant;
    private final String status;
    private final String safetyStock;
    private final String leadTimeDays;

    public ItemMasterRecord(String itemCode, String itemName, String itemType, String uom, String plant,
                            String status, String safetyStock, String leadTimeDays) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.itemType = itemType;
        this.uom = uom;
        this.plant = plant;
        this.status = status;
        this.safetyStock = safetyStock;
        this.leadTimeDays = leadTimeDays;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemType() {
        return itemType;
    }

    public String getUom() {
        return uom;
    }

    public String getPlant() {
        return plant;
    }

    public String getStatus() {
        return status;
    }

    public String getSafetyStock() {
        return safetyStock;
    }

    public String getLeadTimeDays() {
        return leadTimeDays;
    }

    public String[] toValues() {
        return new String[]{itemCode, itemName, itemType, uom, plant, status, safetyStock, leadTimeDays};
    }

    public static ItemMasterRecord fromValues(String[] values) {
        return new ItemMasterRecord(
                valueAt(values, 0),
                valueAt(values, 1),
                valueAt(values, 2),
                valueAt(values, 3),
                valueAt(values, 4),
                valueAt(values, 5),
                valueAt(values, 6),
                valueAt(values, 7)
        );
    }

    private static String valueAt(String[] values, int index) {
        if (values == null || index >= values.length || values[index] == null) {
            return "";
        }
        return values[index].trim();
    }
}
