package com.atms.pages;

import com.atms.elements.CartElements;
import com.atms.utils.action.ActionEngine;

public class CartPage {

    public boolean isRemoveButtonPresent() {
        return ActionEngine.isDisplayed(CartElements.REMOVE_BUTTON);
    }

    public void clickRemoveButton() {
        ActionEngine.click(CartElements.REMOVE_BUTTON);
    }

    public void clickCheckout() {
        ActionEngine.click(CartElements.CHECKOUT_BTN);
    }
}
