package com.atms.utils.waits;

import com.atms.driver.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Explicit wait utilities used by ActionEngine before any interaction.
 */
public class WaitUtils {

    private static final int TIMEOUT_SECONDS = 10;

    public static void waitForVisible(By locator) {
        WebDriver driver = DriverManager.getDriver();
        new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SECONDS))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static void waitForClickable(By locator) {
        WebDriver driver = DriverManager.getDriver();
        new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SECONDS))
                .until(ExpectedConditions.elementToBeClickable(locator));
    }
}
