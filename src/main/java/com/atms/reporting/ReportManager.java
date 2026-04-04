package com.atms.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

/**
 * Singleton holder for ExtentReports instance.
 * Thread-safe via synchronized lazy init — safe under parallel execution.
 */
public class ReportManager {

    private static ExtentReports extent;

    public static synchronized ExtentReports getReport() {
        if (extent == null) {
            ExtentSparkReporter spark = new ExtentSparkReporter("reports/ExtentReport.html");
            extent = new ExtentReports();
            extent.attachReporter(spark);
        }
        return extent;
    }
}
