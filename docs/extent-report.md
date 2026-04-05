# Reporting

---

## Stack

- **ExtentReports 5.1.1** — HTML report engine
- **ExtentSparkReporter** — renders the actual HTML file
- **TestListener** — TestNG `ITestListener` implementation that feeds events into Extent
- **ReportManager** — synchronized singleton that holds the `ExtentReports` instance

Report output: `reports/ExtentReport.html`
Screenshots: `reports/screenshots/<methodName>_<timestamp>.png`

---

## The Flat Report Problem

Default Extent usage creates one top-level node per test method:

```
✅ login
✅ cartFlow
❌ verifyCheckoutFlow
✅ verifyCheckoutPriceAssert
✅ launchUrl
```

At scale this is unreadable — 50 tests means 50 unrelated top-level entries with no grouping, no context.

---

## The Fix: Class-Grouped Hierarchy

`TestListener` creates a parent node per class and a child node per method:

```
📁 SauceDemoTest
   ✅ login
   ✅ cartFlow
   ❌ verifyCheckoutFlow  → stack trace + screenshot inline
   ✅ verifyCheckoutPriceAssert
📁 SampleTest
   ✅ launchUrl
```

---

## Thread-Safe Parent Node Creation

Under parallel execution, multiple threads may try to create the parent node for the same class simultaneously. A naive `if (map.get(className) == null)` check creates a race condition — two threads both see null and both create a parent, resulting in duplicate nodes.

`ConcurrentHashMap.computeIfAbsent()` is atomic:

```java
ExtentTest parent = classNodeMap.computeIfAbsent(
    className,
    name -> ReportManager.getReport().createTest(name)
);
```

Only one thread executes the lambda. All other threads that arrive simultaneously block and then receive the already-created node.

Each thread's active child node is stored in `ThreadLocal<ExtentTest> testNode` — so `onTestSuccess()` and `onTestFailure()` always write to the correct node for that thread.

---

## Failure Handling

On failure, three things are logged to the child node in sequence:

1. `"Test Failed"` — status marker
2. The full `Throwable` — stack trace rendered inline
3. Screenshot — captured, saved to disk, embedded as a relative path

```java
String path = takeScreenshot(methodName);
node.fail("Screenshot:", MediaEntityBuilder.createScreenCaptureFromPath(path).build());
```

The screenshot path returned is relative (`screenshots/filename.png`) so Extent resolves it correctly from the report's location in `reports/`.

---

## ReportManager Singleton

`getReport()` is `synchronized` to prevent double-initialization under parallel startup:

```java
public static synchronized ExtentReports getReport() {
    if (extent == null) {
        ExtentSparkReporter spark = new ExtentSparkReporter("reports/ExtentReport.html");
        extent = new ExtentReports();
        extent.attachReporter(spark);
    }
    return extent;
}
```

Without `synchronized`, two threads starting simultaneously could both see `extent == null` and create two separate `ExtentReports` instances — resulting in a corrupted or incomplete report.

`flush()` is called in `onFinish()` which fires once after the entire suite completes.
