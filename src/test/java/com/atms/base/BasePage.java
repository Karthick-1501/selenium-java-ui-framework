package com.atms.base;

import com.atms.config.ConfigManager;
import com.atms.driver.DriverManager;

public class BasePage {
	
    public void launchApplication() {
        String url = ConfigManager.getExecution("base.url");
        DriverManager.getDriver().get(url);
    }

}
