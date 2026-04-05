package com.atms.pages;

import java.math.BigDecimal;

import org.openqa.selenium.By;

import com.atms.driver.DriverManager;
import com.atms.elements.CheckoutOverviewElements;
import com.atms.utils.action.ActionEngine;

public class CheckoutOverviewPage extends ActionEngine{

    public String getItemPrice() {
        return ActionEngine.getText(CheckoutOverviewElements.ITEM_PRICE);
    }

    public void clickFinish() {
        click(CheckoutOverviewElements.FINISH_BTN);
    }
    
    public BigDecimal getUISubtotal() {
        String subtotalText = ActionEngine.getText(CheckoutOverviewElements.ITEM_SUBTOTAL);
        String value = subtotalText.replace("Item total: $", "");
        return new BigDecimal(value);
    }
}
