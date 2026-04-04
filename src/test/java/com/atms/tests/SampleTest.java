package com.atms.tests;

import com.atms.base.BaseTest;
import com.atms.config.ConfigManager;
import com.atms.driver.DriverManager;
import com.atms.reporting.TestListener;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(TestListener.class)
public class SampleTest extends BaseTest {

    @Test
    public void launchUrl() {
        String url = ConfigManager.getExecution("base.url");
        DriverManager.getDriver().get(url);
        String currentUrl = DriverManager.getDriver().getCurrentUrl();
        Assert.assertTrue(currentUrl.contains("saucedemo"), "Launch URL verification failed. Got: " + currentUrl);
    }
}
