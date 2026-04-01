package com.atms.pages;

import com.atms.elements.CheckoutElements;
import com.atms.utils.action.ActionEngine;

public class CheckoutPage {



    public void enterDetails(String first, String last, String zip) {
        ActionEngine.type(CheckoutElements.FIRST_NAME, first);
        ActionEngine.type(CheckoutElements.LAST_NAME, last);
        ActionEngine.type(CheckoutElements.ZIP, zip);
    }

    public void clickContinue() {
        ActionEngine.click(CheckoutElements.CONTINUE_BTN);
    }
}