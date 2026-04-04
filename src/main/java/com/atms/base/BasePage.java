package com.atms.base;

import com.atms.config.ConfigManager;
import com.atms.driver.DriverManager;

/**
 * Base class for all Page Objects.
 * Provides common page-level utilities like application launch.
 */
public class BasePage {

    public void launchApplication() {
        String url = ConfigManager.getExecution("base.url");
        DriverManager.getDriver().get(url);
    }
}
