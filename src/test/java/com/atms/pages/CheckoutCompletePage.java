package com.atms.pages;

import com.atms.elements.CheckoutCompleteElements;
import com.atms.utils.action.ActionEngine;

public class CheckoutCompletePage {

 

    public String getConfirmationMessage() {
        return ActionEngine.getText(CheckoutCompleteElements.THANK_YOU_MSG);
    }
}