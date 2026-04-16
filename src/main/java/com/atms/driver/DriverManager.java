package com.atms.driver;

import com.atms.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-safe WebDriver factory using ThreadLocal.
 * Supports parallel execution at both class and method level.
 * Headless mode is controlled by execution.properties: headless=true|false
 */
public class DriverManager {

    private static final ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();

    public static void initDriver() {
        if (threadLocalDriver.get() == null) {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--incognito");
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-infobars");
            options.addArguments("--disable-save-password-bubble");

            // CI-safe flags — required when running headless in a container
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--remote-allow-origins=*");

            String headless = ConfigManager.getExecution("headless");
            if ("true".equalsIgnoreCase(headless)) {
                options.addArguments("--headless=new");
                options.addArguments("--window-size=1920,1080");
            }

            Map<String, Object> prefs = new HashMap<>();
            prefs.put("credentials_enable_service", false);
            prefs.put("profile.password_manager_enabled", false);
            options.setExperimentalOption("prefs", prefs);

            threadLocalDriver.set(new ChromeDriver(options));
        }
    }

    public static WebDriver getDriver() {
        return threadLocalDriver.get();
    }

    public static WebDriver getCurrentDriver() {
        WebDriver driver = threadLocalDriver.get();
        if (driver == null) {
            throw new RuntimeException("WebDriver is null in the current thread. Was initDriver() called?");
        }
        return driver;
    }

    public static void quitDriver() {
        WebDriver driver = threadLocalDriver.get();
        if (driver != null) {
            driver.quit();
            threadLocalDriver.remove();
        }
    }
}
