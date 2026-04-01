package com.atms.driver;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;


public class DriverManager {

    private static ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();

 
    public static void initDriver() {

        if (threadLocalDriver.get() == null) {

            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            
            options.addArguments("--incognito");
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-infobars");
            options.addArguments("--disable-save-password-bubble");

            options.setExperimentalOption("prefs", new java.util.HashMap<String, Object>() {{
                put("credentials_enable_service", false);
                put("profile.password_manager_enabled", false);
            }});

            threadLocalDriver.set(new ChromeDriver(options));
        }
    }
    

    public static WebDriver getDriver() {
        return threadLocalDriver.get();
    }
    
    public static WebDriver getCurrentDriver() {
        WebDriver driver = threadLocalDriver.get();

        if (driver == null) {
            throw new RuntimeException("Driver is NULL in current thread");
        }

        return driver;
    }

    public static void quitDriver() {
        WebDriver webDriver = threadLocalDriver.get();

        if (webDriver != null) {
            webDriver.quit();
            threadLocalDriver.remove();
        }
    }
}