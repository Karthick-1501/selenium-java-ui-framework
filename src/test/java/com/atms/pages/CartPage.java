package com.atms.pages;

import com.atms.elements.CartElements;
import com.atms.utils.action.ActionEngine;

public class CartPage {



    public boolean isRemoveButtonPresent() {
        try {
            ActionEngine.getText(CartElements.REMOVE_BUTTON);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void clickCheckout() {
        ActionEngine.click(CartElements.CHECKOUT_BTN);
    }
    
    public void clickRemovebutton() {
    	ActionEngine.click(CartElements.REMOVE_BUTTON);
    	
    }
}