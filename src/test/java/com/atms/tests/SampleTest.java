package com.atms.tests;

import com.atms.base.BaseTest;
import com.atms.driver.DriverManager;
import org.testng.annotations.Test;
import com.atms.config.ConfigManager;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import com.atms.reporting.TestListener;

@Listeners(TestListener.class)
public class SampleTest extends BaseTest {
	@Test
	public void LaunchUrl() {
		String url = ConfigManager.getExecution("base.url");

	    DriverManager.getDriver().get(url);
	   
	}
}