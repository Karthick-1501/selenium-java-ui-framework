package com.atms.pages;

import com.atms.elements.CheckoutOverviewElements;
import com.atms.utils.action.ActionEngine;

public class CheckoutOverviewPage {



    public String getItemPrice() {
        return ActionEngine.getText(CheckoutOverviewElements.ITEM_PRICE);
    }

    public void clickFinish() {
        ActionEngine.click(CheckoutOverviewElements.FINISH_BTN);
    }
}