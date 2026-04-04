package com.atms.utils.action;

import com.atms.config.ConfigManager;
import com.atms.driver.DriverManager;
import com.atms.utils.waits.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Centralized action handler for all WebDriver interactions.
 *
 * Retry count is driven by execution.properties (retry.count).
 * Supports locator prefixes: id=, xpath=, css=
 */
public class ActionEngine {

    private static final int MAX_ATTEMPTS =
            Integer.parseInt(ConfigManager.getExecution("retry.count"));

    public static void click(String locator) {
        By target = getBy(locator);
        int attempts = 0;

        while (attempts < MAX_ATTEMPTS) {
            try {
                WaitUtils.waitForClickable(target);
                DriverManager.getDriver().findElement(target).click();
                return;
            } catch (Exception e) {
                attempts++;
                if (attempts == MAX_ATTEMPTS) {
                    throw new RuntimeException("click() failed after " + MAX_ATTEMPTS + " attempts: " + locator, e);
                }
            }
        }
    }

    public static void type(String locator, String value) {
        By target = getBy(locator);
        int attempts = 0;
        int maxAttempts = 2;

        while (attempts < maxAttempts) {
            try {
                WaitUtils.waitForVisible(target);
                WebElement element = DriverManager.getDriver().findElement(target);
                element.clear();
                element.sendKeys(value);
                return;
            } catch (Exception e) {
                attempts++;
                if (attempts == maxAttempts) {
                    throw new RuntimeException("type() failed after " + maxAttempts + " attempts: " + locator, e);
                }
            }
        }
    }

    public static String getText(String locator) {
        By target = getBy(locator);
        WaitUtils.waitForVisible(target);
        return DriverManager.getDriver().findElement(target).getText().trim();
    }

    private static By getBy(String locator) {
        if (locator.startsWith("id=")) {
            return By.id(locator.replace("id=", ""));
        } else if (locator.startsWith("xpath=")) {
            return By.xpath(locator.replace("xpath=", ""));
        } else if (locator.startsWith("css=")) {
            return By.cssSelector(locator.replace("css=", ""));
        }
        throw new RuntimeException("Unsupported locator strategy. Use id=, xpath=, or css= prefix. Got: " + locator);
    }
}
