package com.atms.utils.action;

import com.atms.driver.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import com.atms.utils.waits.*;
import com.atms.config.*;

/**
 * Centralized action handler for WebDriver interactions.
 */
public class ActionEngine {
	
	private static final int MAX_ATTEMPTS =    Integer.parseInt(ConfigManager.getExecution("retry.count"));

	public static void click(String locator) {

	    By targetElement = getBy(locator);
	    int attempts = 0;
	   

	    while (attempts < MAX_ATTEMPTS) {
	        try {
	            WaitUtils.waitForClickable(targetElement);

	            WebElement element = DriverManager.getDriver().findElement(targetElement);
	            element.click();

	            return; // success

	        } catch (Exception e) {
	            attempts++;

	            if (attempts == MAX_ATTEMPTS) {
	                throw new RuntimeException("Failed to click element after retries: " + locator, e);
	            }
	        }
	    }
	}

	public static void type(String locator, String value) {

	    By targetElement = getBy(locator);
	    int attempts = 0;
	    int maxAttempts = 2;

	    while (attempts < maxAttempts) {
	        try {
	            WaitUtils.waitForVisible(targetElement);

	            WebElement element = DriverManager.getDriver().findElement(targetElement);
	            element.clear();
	            element.sendKeys(value);

	            return;

	        } catch (Exception e) {
	            attempts++;

	            if (attempts == maxAttempts) {
	                throw new RuntimeException("Failed to type into element: " + locator, e);
	            }
	        }
	    }
	}

	public static String getText(String locator) {
	    By targetElement = getBy(locator);

	    WaitUtils.waitForVisible(targetElement);

	    return DriverManager.getDriver().findElement(targetElement).getText().trim();
	}
	
    private static WebElement getElement(String locator) {
        By targetElement = getBy(locator);
        return DriverManager.getDriver().findElement(targetElement);
    }

    private static By getBy(String locator) {

        if (locator.startsWith("id=")) {
            return By.id(locator.replace("id=", ""));
        } else if (locator.startsWith("xpath=")) {
            return By.xpath(locator.replace("xpath=", ""));
        } else if (locator.startsWith("css=")) {
            return By.cssSelector(locator.replace("css=", ""));
        }

        throw new RuntimeException("Invalid locator: " + locator);
    }
}