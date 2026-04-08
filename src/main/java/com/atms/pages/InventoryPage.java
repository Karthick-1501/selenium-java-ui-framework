package com.atms.pages;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import com.atms.elements.InventoryElements;
import com.atms.utils.action.ActionEngine;

public class InventoryPage extends ActionEngine {

    public void addBackpackToCart() {
        ActionEngine.click(InventoryElements.ADD_TO_CART_BACKPACK);
    }

    public void openCart() {
        ActionEngine.click(InventoryElements.CART_ICON);
    }

    public String getCartCount() {
        return ActionEngine.getText(InventoryElements.CART_BADGE);
    }

    private static final Map<String, String> ITEM_MAP = new HashMap<>();

    static {
        ITEM_MAP.put("Sauce Labs Backpack", InventoryElements.ADD_TO_CART_BACKPACK);
        ITEM_MAP.put("Sauce Labs Bike",     InventoryElements.ADD_TO_CART_BIKE);
        ITEM_MAP.put("Sauce Labs T-Shirt",  InventoryElements.ADD_TO_CART_TSHIRT);
        ITEM_MAP.put("Sauce Labs Jacket",   InventoryElements.ADD_TO_CART_JACKET);
        ITEM_MAP.put("Sauce Labs Onesie",   InventoryElements.ADD_TO_CART_ONESIE);
        ITEM_MAP.put("Test all",            InventoryElements.ADD_TO_CART_TESTALL);
    }

    public void addItem(String itemName) {
        String locator = ITEM_MAP.get(itemName);
        if (locator == null) {
            throw new RuntimeException("Unknown item name in ITEM_MAP: " + itemName);
        }
        click(locator);
    }

    public List<BigDecimal> getAllItemPrices() {
        List<String> rawPrices = ActionEngine.getTexts(InventoryElements.PRICE_LIST);
        List<BigDecimal> prices = new ArrayList<>();
        for (String raw : rawPrices) {
            prices.add(new BigDecimal(raw.replace("$", "")));
        }
        return prices;
    }

    public BigDecimal sumPrices(List<BigDecimal> prices) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal price : prices) {
            total = total.add(price);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
