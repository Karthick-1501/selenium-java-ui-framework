# UI Automation Framework

> Selenium · Java · TestNG · Maven · ExtentReports  
> Built to solve real parallel execution, flakiness, and reporting problems — not a tutorial clone.

---

## What this is

A production-structured UI test automation framework built on top of Selenium WebDriver (Java), designed for multi-threaded execution without driver collision, config-driven test data, and grouped HTML reporting with inline failure screenshots.

Target app: [SauceDemo](https://www.saucedemo.com) — used as a realistic e-commerce test surface covering login → inventory → cart → checkout → order confirmation.

---

## Project Structure

```
Ui-Framework/
├── src/
│   ├── main/java/com/atms/         ← Framework layer (not test-coupled)
│   │   ├── base/                   ← BasePage, BaseTest (lifecycle)
│   │   ├── config/                 ← ConfigManager (property loader)
│   │   ├── driver/                 ← DriverManager (ThreadLocal WebDriver)
│   │   ├── elements/               ← Locator constants per page
│   │   ├── pages/                  ← Page Object classes
│   │   ├── reporting/              ← ExtentReports listener + singleton
│   │   └── utils/
│   │       ├── action/             ← ActionEngine (click, type, getText + retry)
│   │       ├── assertion/          ← AssertEngine (BigDecimal numeric assert)
│   │       └── waits/              ← WaitUtils (explicit waits)
│   └── test/java/com/atms/
│       └── tests/                  ← Business-flow test classes
├── Environment/
│   ├── execution.properties        ← browser, base URL, retry count
│   └── testdata.properties         ← credentials, form data
├── reports/                        ← ExtentReport output + screenshots
├── testng-classes.xml              ← Parallel by class (1 browser/class)
├── testng-methods.xml              ← Parallel by method (1 browser/test)
└── pom.xml
```

---

## Key Engineering Decisions

### 1. ThreadLocal WebDriver — parallel-safe by design

Standard `static WebDriver` breaks under parallel execution because all threads share the same instance. `DriverManager` wraps the driver in `ThreadLocal<WebDriver>` so each thread gets its own isolated browser session.

```java
private static final ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();
```

`initDriver()` checks `threadLocalDriver.get() == null` before creating — no double-init even if called multiple times on the same thread. `quitDriver()` calls `threadLocalDriver.remove()` to prevent memory leaks between test runs.

---

### 2. Dual parallel modes — same codebase, two XML files

`BaseTest` detects the active parallel mode at runtime by reading the suite's parallel setting:

```java
private boolean isMethodParallel() {
    return org.testng.Reporter.getCurrentTestResult()
            .getTestContext()
            .getCurrentXmlTest()
            .getSuite()
            .getParallel()
            .toString()
            .equalsIgnoreCase("methods");
}
```

| Mode | XML | Driver scope | Use when |
|------|-----|-------------|----------|
| `parallel="classes"` | `testng-classes.xml` | One browser per class | Tests within a class share state |
| `parallel="methods"` | `testng-methods.xml` | One browser per method | Full isolation per test |

Lifecycle hooks (`@BeforeClass` / `@BeforeMethod` / `@AfterClass` / `@AfterMethod`) switch automatically — no code change needed between modes.

---

### 3. Config-driven — zero hardcoding in test code

All environment-specific values live in `Environment/`:

```properties
# execution.properties
base.url=https://www.saucedemo.com
browser=chrome
retry.count=2

# testdata.properties
valid.username=standard_user
valid.password=secret_sauce
```

`ConfigManager` loads both files in a static block and exposes typed accessors. Tests read values via:

```java
ConfigManager.getExecution("base.url");
ConfigManager.getTestData("valid.username");
```

Swapping environments = editing one `.properties` file.

---

### 4. ActionEngine with configurable retry

UI interactions fail transiently — element not yet clickable, brief DOM re-render, network lag. ActionEngine wraps `click()` with a retry loop driven by `retry.count` from config:

```java
while (attempts < MAX_ATTEMPTS) {
    try {
        WaitUtils.waitForClickable(target);
        driver.findElement(target).click();
        return;
    } catch (Exception e) {
        attempts++;
        if (attempts == MAX_ATTEMPTS) throw new RuntimeException(...);
    }
}
```

Retry kicks in only when the element genuinely fails — not as a lazy fallback for bad locators. Explicit waits (`WebDriverWait`) run first inside every action before any retry.

Locator strategy is prefix-encoded:

```java
"id=login-button"          // → By.id
"css=.shopping_cart_badge" // → By.cssSelector
"xpath=//div[@class='..."] // → By.xpath
```

---

### 5. Custom numeric assertion — BigDecimal precision

TestNG's `assertEquals("1.00", "1.0")` fails. The UI renders prices as `"$29.99"` or `"1.00"` depending on context. Standard string comparison is unreliable here.

`AssertEngine.assertDoubleEquals()` strips currency symbols in the caller, then normalizes both sides via `BigDecimal.compareTo()` — which ignores trailing zeros:

```java
BigDecimal actual   = new BigDecimal("28.990");  // from UI
BigDecimal expected = BigDecimal.valueOf(28.99);
actual.compareTo(expected) == 0                  // → true
```

This is the same precision approach used in financial and tax calculation testing, where `1.00 ≠ 1.0` as strings but must be treated as equal.

---

### 6. Class-grouped Extent reporting with thread safety

Flat Extent reports dump every test as a top-level node — unreadable at scale. `TestListener` groups test methods under their parent class using `ConcurrentHashMap`:

```java
ExtentTest parent = classNodeMap.computeIfAbsent(
    className,
    name -> ReportManager.getReport().createTest(name)
);
ExtentTest child = parent.createNode(methodName);
```

`computeIfAbsent` is atomic — no race condition when multiple threads try to create the same parent node simultaneously. `ThreadLocal<ExtentTest> testNode` keeps each thread's active node isolated.

On failure, a screenshot is captured immediately, saved to `reports/screenshots/`, and embedded inline in the report with the stack trace.

`ReportManager.getReport()` is `synchronized` to prevent double-initialization of `ExtentReports` under parallel startup.

---

## Running Tests

**Prerequisites:** Java 17+, Maven 3.8+, Chrome installed

```bash
# Class-parallel (default — faster)
mvn test -DsuiteXmlFile=testng-classes.xml

# Method-parallel (fully isolated)
mvn test -DsuiteXmlFile=testng-methods.xml
```

Report generated at `reports/ExtentReport.html` after each run.

---

## Test Coverage

| Test | Flow |
|------|------|
| `launchUrl` | Smoke — verifies base URL loads correctly |
| `login` | Credential entry → successful login |
| `cartFlow` | Add item → assert cart badge count → remove item |
| `verifyCheckoutFlow` | Full flow: login → add → cart → checkout → confirm |
| `verifyCheckoutPriceAssert` | Same flow with explicit price assertion via `AssertEngine` |

---

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| selenium-java | 4.21.0 | WebDriver core |
| testng | 7.10.2 | Test runner + parallel execution |
| webdrivermanager | 5.8.0 | Automatic ChromeDriver binary management |
| extentreports | 5.1.1 | HTML reporting |

---

## Author

**Karthick S** — Automation Test Engineer / SDET  
[LinkedIn](https://linkedin.com/in/karthicks1520) · Chennai, India
