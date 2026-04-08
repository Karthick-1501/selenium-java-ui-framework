package com.atms.pages;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;

import com.atms.elements.InventoryElements;
import com.atms.utils.action.ActionEngine;
import com.atms.utils.db.DBUtils;

public class TaxPage {

    public double fetchTaxPercent(String itemName) {

        String query = """
            SELECT t.tx_rt
            FROM product.id_itm p
            JOIN classification.cls_cd c ON p.id_itm = c.itm_id
            JOIN tax.tx_cfg t ON c.cl_cd = t.cl_cd
            WHERE p.name = ?
        """;

        try (Connection conn = DBUtils.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, itemName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("tx_rt");
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