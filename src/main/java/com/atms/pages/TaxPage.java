package com.atms.pages;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;

import com.atms.config.ConfigManager;
import com.atms.elements.InventoryElements;
import com.atms.utils.action.ActionEngine;
import com.atms.utils.db.DBUtils;

public class TaxPage {

    public double fetchTaxPercent(String itemName) {

        String productTable        = ConfigManager.getExecution("db.table.product");
        String classificationTable = ConfigManager.getExecution("db.table.classification");
        String taxTable            = ConfigManager.getExecution("db.table.tax");

        String colTaxRate          = ConfigManager.getExecution("db.col.tax.rate");
        String colProductId        = ConfigManager.getExecution("db.col.product.id");
        String colClassItemId      = ConfigManager.getExecution("db.col.classification.item.id");
        String colClassCode        = ConfigManager.getExecution("db.col.classification.code");
        String colTaxCode          = ConfigManager.getExecution("db.col.tax.code");
        String colProductName      = ConfigManager.getExecution("db.col.product.name");

        String query = String.format("""
            SELECT t.%s
            FROM %s p
            JOIN %s c ON p.%s = c.%s
            JOIN %s t ON c.%s = t.%s
            WHERE p.%s = ?
        """, colTaxRate,
             productTable,
             classificationTable, colProductId, colClassItemId,
             taxTable, colClassCode, colTaxCode,
             colProductName);

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, itemName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble(colTaxRate);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch tax percent for item: " + itemName, e);
        }

        return 0;
    }

    public BigDecimal calculateTax(BigDecimal subtotal, double percent) {
        return subtotal
                .multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getUITax() {
        String text = ActionEngine.getText(InventoryElements.TAX_LABEL);
        return new BigDecimal(text.replace("Tax: $", "").trim());
    }
}
