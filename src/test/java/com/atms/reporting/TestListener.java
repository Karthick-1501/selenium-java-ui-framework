package com.atms.reporting;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.atms.driver.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestListener implements ITestListener {
	
	private static Map<String, ExtentTest> classNodeMap = new ConcurrentHashMap<>();
	private static ThreadLocal<ExtentTest> testNode = new ThreadLocal<>();

    
    @Override
    public void onTestStart(ITestResult result) {

        String className = result.getTestClass().getRealClass().getSimpleName();
        String methodName = result.getMethod().getMethodName();

        ExtentTest parent = classNodeMap.computeIfAbsent(
                className,
                name -> ReportManager.getReport().createTest(name)
        );

        ExtentTest child = parent.createNode(methodName);

        testNode.set(child);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        testNode.get().log(Status.PASS, "Test Passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {

        testNode.get().fail("Test Failed");

        Throwable throwable = result.getThrowable();
        testNode.get().fail(throwable);

        try {
            String path = takeScreenshot(result.getMethod().getMethodName());

            testNode.get().fail("Screenshot below:",
                    MediaEntityBuilder.createScreenCaptureFromPath(path).build());

        } catch (Exception e) {
            testNode.get().fail("Screenshot failed: " + e.getMessage());
        }
    }
    
    @Override
    public void onFinish(org.testng.ITestContext context) {
        ReportManager.getReport().flush();
    }

    private String takeScreenshot(String testName) throws Exception {

        String timestamp = String.valueOf(System.currentTimeMillis());

        String folderPath = "reports/screenshots/";
        Files.createDirectories(Paths.get(folderPath));

        String fileName = testName + "_" + timestamp + ".png";

        // Full path (for saving file)
        String fullPath = folderPath + fileName;

        File src = ((TakesScreenshot) DriverManager.getDriver())
                .getScreenshotAs(OutputType.FILE);

        Files.copy(src.toPath(), Paths.get(fullPath));

        // 🔥 Return relative path for report
        return "screenshots/" + fileName;
    }
}