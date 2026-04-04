package com.atms.pages;

import com.atms.elements.LoginElements;
import com.atms.utils.action.ActionEngine;

public class LoginPage {

    public void login(String username, String password) {
        ActionEngine.type(LoginElements.USERNAME, username);
        ActionEngine.type(LoginElements.PASSWORD, password);
        ActionEngine.click(LoginElements.LOGIN_BTN);
    }
}
