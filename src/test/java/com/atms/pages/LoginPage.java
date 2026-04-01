package com.atms.pages;

import com.atms.elements.LoginElements;
import com.atms.utils.action.ActionEngine;

public class LoginPage {

    public void login(String username, String password) {
        ActionEngine.type(LoginElements.SDUSERNAME, username);
        ActionEngine.type(LoginElements.SDPASSWORD, password);
        ActionEngine.click(LoginElements.SDLOGIN_BTN);
    }
}