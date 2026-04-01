package com.atms.base;


import com.atms.config.ConfigManager;
import com.atms.driver.DriverManager;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import com.atms.reporting.TestListener;
import org.testng.ITestResult;


@Listeners(TestListener.class)
public class BaseTest {

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod() {

	    if (isMethodParallel()) {
	        DriverManager.initDriver();
	    }

	    // ✅ Always clear cookies before each test
	    if (DriverManager.getDriver() != null) {
	        DriverManager.getDriver().manage().deleteAllCookies();
	    }
	}

	@BeforeClass(alwaysRun = true)
	public void beforeClass() {

	    if (!isMethodParallel()) {
	        DriverManager.initDriver();
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
	
	private boolean isMethodParallel() {
	    return org.testng.Reporter.getCurrentTestResult()
	            .getTestContext()
	            .getCurrentXmlTest()
	            .getSuite()
	            .getParallel()
	            .toString()
	            .equalsIgnoreCase("methods");
	}
    
    public void launchApplication() {
        String url = ConfigManager.getExecution("base.url");
        DriverManager.getDriver().get(url);
    }
}