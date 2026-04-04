package com.atms.reporting;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.atms.driver.DriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TestNG listener that builds a class-grouped Extent report.
 *
 * Report structure:
 *   ClassName (parent node)
 *     └── methodName (child node) → PASS / FAIL + screenshot on failure
 *
 * ConcurrentHashMap ensures parent nodes are created once and shared safely
 * across threads during parallel execution.
 */
public class TestListener implements ITestListener {

    private static final Map<String, ExtentTest> classNodeMap = new ConcurrentHashMap<>();
    private static final ThreadLocal<ExtentTest> testNode = new ThreadLocal<>();

    @Override
    public void onTestStart(ITestResult result) {
        String className  = result.getTestClass().getRealClass().getSimpleName();
        String methodName = result.getMethod().getMethodName();

        ExtentTest parent = classNodeMap.computeIfAbsent(
                className,
                name -> ReportManager.getReport().createTest(name)
        );

        testNode.set(parent.createNode(methodName));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        testNode.get().log(Status.PASS, "Test Passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest node = testNode.get();
        node.fail("Test Failed");
        node.fail(result.getThrowable());

        try {
            String screenshotPath = takeScreenshot(result.getMethod().getMethodName());
            node.fail("Screenshot:", MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
        } catch (Exception e) {
            node.fail("Screenshot capture failed: " + e.getMessage());
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        testNode.get().log(Status.SKIP, "Test Skipped");
    }

    @Override
    public void onFinish(ITestContext context) {
        ReportManager.getReport().flush();
    }

    private String takeScreenshot(String testName) throws Exception {
        String folderPath = "reports/screenshots/";
        Files.createDirectories(Paths.get(folderPath));

        String fileName = testName + "_" + System.currentTimeMillis() + ".png";
        String fullPath = folderPath + fileName;

        File src = ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.FILE);
        Files.copy(src.toPath(), Paths.get(fullPath));

        // Return relative path so Extent can resolve the screenshot correctly
        return "screenshots/" + fileName;
    }
}
