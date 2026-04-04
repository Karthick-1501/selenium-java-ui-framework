package com.atms.pages;

import com.atms.elements.InventoryElements;
import com.atms.utils.action.ActionEngine;

public class InventoryPage {

    public void addBackpackToCart() {
        ActionEngine.click(InventoryElements.ADD_TO_CART_BACKPACK);
    }

    public void openCart() {
        ActionEngine.click(InventoryElements.CART_ICON);
    }

    public String getCartCount() {
        return ActionEngine.getText(InventoryElements.CART_BADGE);
    }
}
