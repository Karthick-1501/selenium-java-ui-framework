package com.atms.pages;

import com.atms.elements.CheckoutElements;
import com.atms.utils.action.ActionEngine;

public class CheckoutPage {

    public void enterDetails(String firstName, String lastName, String zip) {
        ActionEngine.type(CheckoutElements.FIRST_NAME, firstName);
        ActionEngine.type(CheckoutElements.LAST_NAME, lastName);
        ActionEngine.type(CheckoutElements.ZIP, zip);
    }

    public void clickContinue() {
        ActionEngine.click(CheckoutElements.CONTINUE_BTN);
    }
}
