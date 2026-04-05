# Parallel Execution

---

## The Problem with Static WebDriver

```java
// ❌ breaks under parallel execution
public static WebDriver driver;
```

When two test methods run simultaneously on different threads, both share the same `driver` reference. Thread A's `driver.get(url)` overwrites Thread B's session mid-test. Results are non-deterministic — sometimes pass, sometimes NPE, sometimes wrong page.

---

## The Fix: ThreadLocal

```java
private static final ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();
```

`ThreadLocal` gives each thread its own independent copy of the variable. Thread A's driver and Thread B's driver are completely separate objects stored in the same `ThreadLocal` — each thread reads and writes only its own copy.

```
Thread-1 → threadLocalDriver.get() → ChromeDriver instance A
Thread-2 → threadLocalDriver.get() → ChromeDriver instance B
Thread-3 → threadLocalDriver.get() → ChromeDriver instance C
```

No shared state. No collision.

---

## Two Execution Modes

The framework supports both modes from the same codebase — no code changes needed, just swap the XML.

### `testng-classes.xml` — parallel by class

```xml
<suite parallel="classes" thread-count="3">
```

- One browser is created per test class (`@BeforeClass`)
- All methods in that class share the same browser session
- Browser is destroyed after the class finishes (`@AfterClass`)
- Cookies are wiped before each method (`@BeforeMethod`) to prevent state bleed between tests

**Best for:** Faster runs where tests within a class share a logical session.

---

### `testng-methods.xml` — parallel by method

```xml
<suite parallel="methods" thread-count="4">
```

- A fresh browser is created for every single test method (`@BeforeMethod`)
- Browser is destroyed immediately after each method (`@AfterMethod`)
- Zero shared state between any two tests

**Best for:** Full isolation — every test is completely independent.

---

## Runtime Mode Detection

`BaseTest` detects which mode is active at runtime so the same lifecycle code handles both:

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

`@BeforeClass` / `@AfterClass` only init/quit the driver in class-parallel mode.
`@BeforeMethod` / `@AfterMethod` only init/quit in method-parallel mode.
Cookie cleanup always runs regardless of mode.

---

## Memory Safety

`quitDriver()` calls `threadLocalDriver.remove()` after quitting:

```java
public static void quitDriver() {
    WebDriver driver = threadLocalDriver.get();
    if (driver != null) {
        driver.quit();
        threadLocalDriver.remove(); // ← critical
    }
}
```

Without `remove()`, the `ThreadLocal` entry persists in the thread pool even after the browser is closed. In long-running test suites using thread pools, this causes memory leaks and potential stale driver reuse.
