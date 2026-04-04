package com.atms.base;

import com.atms.config.ConfigManager;
import com.atms.driver.DriverManager;
import com.atms.reporting.TestListener;
import org.testng.annotations.*;

/**
 * Base test class providing WebDriver lifecycle management.
 *
 * Supports two parallel modes driven by testng.xml:
 *   - parallel="classes"  → one browser per class (faster, shared state per class)
 *   - parallel="methods"  → one browser per test method (fully isolated, slower)
 *
 * Cookie cleanup runs before every method regardless of mode.
 */
@Listeners(TestListener.class)
public class BaseTest {

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        if (!isMethodParallel()) {
            DriverManager.initDriver();
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod() {
        if (isMethodParallel()) {
            DriverManager.initDriver();
        }
        if (DriverManager.getDriver() != null) {
            DriverManager.getDriver().manage().deleteAllCookies();
        }
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() {
        if (isMethodParallel()) {
            DriverManager.quitDriver();
        }
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() {
        if (!isMethodParallel()) {
            DriverManager.quitDriver();
        }
    }

    public void launchApplication() {
        String url = ConfigManager.getExecution("base.url");
        DriverManager.getDriver().get(url);
    }

    private boolean isMethodParallel() {
        return org.testng.Reporter.getCurrentTestResult()
                .getTestContext()
                .getCurrentXmlTest()
                .getSuite()
                .getParallel()
                .toString()
                .equalsIgnoreCase("methods");
    }
}
