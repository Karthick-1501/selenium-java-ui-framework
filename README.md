# UI Automation Framework

> Selenium · Java 17 · TestNG · Maven · ExtentReports

---

## What This Is

A UI test automation framework built on SauceDemo — an e-commerce app covering login, inventory, cart, checkout, and order confirmation. The target was to solve actual parallel execution and flakiness problems, not just wrap Selenium in a Page Object pattern and call it a framework.

The things that make this worth looking at:

- `ThreadLocal<WebDriver>` — each thread gets its own browser, zero collision under parallel runs
- Two parallel modes (class-level and method-level) from the same codebase — the active mode is detected at runtime, no code changes needed
- `ActionEngine` is the only place `driver.findElement()` is called — retry logic and explicit waits live there, not scattered across page classes
- `AssertEngine` uses `BigDecimal.compareTo()` instead of string equality — no false failures from UI rendering `"1.00"` vs `"1.0"`
- `TestListener` uses `ConcurrentHashMap.computeIfAbsent()` for thread-safe class-grouped reporting
- Everything environment-specific lives in two `.properties` files

---

## Project Structure

```
selenium-java-ui-framework/
├── src/
│   ├── main/java/com/atms/              ← Framework layer (reusable, test-agnostic)
│   │   ├── base/
│   │   │   ├── BasePage.java            ← launchApplication(), shared page utilities
│   │   │   └── BaseTest.java            ← TestNG lifecycle + parallel mode detection
│   │   ├── config/
│   │   │   └── ConfigManager.java       ← Property loader, fails fast on missing files
│   │   ├── driver/
│   │   │   └── DriverManager.java       ← ThreadLocal<WebDriver> factory
│   │   ├── elements/                    ← Locator constants only — no logic, no imports
│   │   │   ├── LoginElements.java
│   │   │   ├── InventoryElements.java
│   │   │   ├── CartElements.java
│   │   │   ├── CheckoutElements.java
│   │   │   ├── CheckoutOverviewElements.java
│   │   │   └── CheckoutCompleteElements.java
│   │   ├── pages/                       ← Business actions, no direct WebDriver calls
│   │   │   ├── LoginPage.java
│   │   │   ├── InventoryPage.java       ← Item map, price collection, sum
│   │   │   ├── CartPage.java
│   │   │   ├── CheckoutPage.java
│   │   │   ├── CheckoutOverviewPage.java ← UI subtotal reader + BigDecimal parsing
│   │   │   └── CheckoutCompletePage.java
│   │   ├── reporting/
│   │   │   ├── ReportManager.java       ← ExtentReports singleton (synchronized init)
│   │   │   └── TestListener.java        ← ITestListener, class-grouped report, thread-safe
│   │   └── utils/
│   │       ├── action/
│   │       │   └── ActionEngine.java    ← Every WebDriver interaction + configurable retry
│   │       ├── assertion/
│   │       │   └── AssertEngine.java    ← BigDecimal numeric assertions
│   │       └── waits/
│   │           └── WaitUtils.java       ← Explicit wait wrappers
│   └── test/java/com/atms/
│       └── tests/
│           ├── SauceDemoTest.java        ← Core checkout and cart tests
│           ├── AddToCartTest.java        ← Multi-item cart + subtotal validation
│           └── SampleTest.java          ← Smoke test
├── Environment/
│   ├── execution.properties             ← browser, base URL, retry count
│   └── testdata.properties             ← credentials, item names, form data
├── docs/
│   ├── architecture.md                  ← Layer responsibilities + dependency flow
│   ├── action-engine.md                 ← Locator strategy, retry internals, why not PageFactory
│   ├── assert-engine.md                 ← BigDecimal vs Double, AssertEngine usage
│   ├── parallel-execution.md            ← ThreadLocal, dual modes, memory safety
│   ├── extent-report.md                 ← Class-grouped hierarchy, thread-safe parent creation
│   ├── retry-mechanism.md               ← What gets retried, what doesn't, and why
│   ├── page-inventory.md                ← Item map pattern, price collection, sumPrices()
│   ├── page-checkout-overview.md        ← getUISubtotal() parsing + BigDecimal rationale
│   └── test-add-to-cart.md              ← subtotalvalidation flow explained
├── reports/                             ← ExtentReport.html + screenshots/ (gitignored)
├── testng-classes.xml                   ← parallel="classes" — one browser per class
├── testng-methods.xml                   ← parallel="methods" — one browser per test
└── pom.xml
```

---

## Quick Start

**Prerequisites:** Java 17+, Maven 3.8+, Chrome installed

```bash
git clone https://github.com/Karthick-1501/selenium-java-ui-framework.git
cd selenium-java-ui-framework

# one browser per class — faster
mvn test -DsuiteXmlFile=testng-classes.xml

# one browser per test method — fully isolated
mvn test -DsuiteXmlFile=testng-methods.xml
```

Report lands at `reports/ExtentReport.html` after each run.

---

## Configuration

`Environment/execution.properties`
```properties
browser=chrome
base.url=https://www.saucedemo.com
retry.count=2
```

`Environment/testdata.properties`
```properties
valid.username=standard_user
valid.password=secret_sauce
firstname=Karthick
lastname=S
postcode=608001

item1=Sauce Labs Backpack
item2=Sauce Labs Bike
item3=Sauce Labs T-Shirt
item4=Sauce Labs Jacket
item5=Sauce Labs Onesie
item6=Test all
```

Switching environments means editing these two files. Nothing else.

---

## Test Coverage

| Class | Test | What it covers |
|-------|------|----------------|
| `SampleTest` | `launchUrl` | Smoke — URL loads and contains `saucedemo` |
| `SauceDemoTest` | `login` | Credential entry → successful login |
| `SauceDemoTest` | `cartFlow` | Add item → assert cart badge count → remove |
| `SauceDemoTest` | `verifyCheckoutPriceAssert` | Full flow with explicit `$29.99` price assertion |
| `SauceDemoTest` | `verifyCheckoutFlow` | Full flow: login → add → cart → checkout → confirm |
| `AddToCartTest` | `addItemsToCart` | Add all 6 inventory items via name-driven `addItem()` |
| `AddToCartTest` | `subtotalvalidation` | Collect prices from inventory DOM → assert against checkout overview subtotal |

### How subtotalvalidation works

This test doesn't hardcode an expected total. It calculates what the subtotal should be from the UI itself:

1. Adds all 6 items to cart
2. Reads every `[data-test='inventory-item-price']` element from the inventory DOM
3. Strips `$`, parses each to `BigDecimal`, sums with `RoundingMode.HALF_UP`
4. Goes through cart → checkout → enter details → continue
5. Reads the `Item total:` label on the Checkout Overview page
6. Asserts calculated sum == displayed subtotal via `BigDecimal.compareTo()`

If SauceDemo changes a price, the test still passes. If their backend subtotal doesn't match the sum of the displayed prices, it fails — which is the actual bug worth catching.

---

## Engineering Decisions

### ThreadLocal WebDriver

A shared `static WebDriver` causes thread collision under parallel execution — two threads writing to the same driver instance produces non-deterministic failures. `DriverManager` wraps it in `ThreadLocal<WebDriver>`:

```java
private static final ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();
```

`quitDriver()` calls `threadLocalDriver.remove()` after quitting — without this, the reference lingers in the thread pool and leaks memory across runs.

→ [`docs/parallel-execution.md`](docs/parallel-execution.md)

---

### Dual parallel modes

`BaseTest` reads the active parallel setting from the TestNG suite at runtime:

```java
private boolean isMethodParallel() {
    return org.testng.Reporter.getCurrentTestResult()
            .getTestContext().getCurrentXmlTest()
            .getSuite().getParallel()
            .toString().equalsIgnoreCase("methods");
}
```

| Mode | File | Driver scope |
|------|------|--------------|
| `parallel="classes"` | `testng-classes.xml` | One browser per class |
| `parallel="methods"` | `testng-methods.xml` | One browser per test method |

`@BeforeClass`/`@AfterClass` and `@BeforeMethod`/`@AfterMethod` route automatically. Swapping modes = swapping XML files.

→ [`docs/parallel-execution.md`](docs/parallel-execution.md)

---

### ActionEngine

Nothing outside `ActionEngine` calls `driver.findElement()`. Every interaction goes through it. Locators use a prefix convention:

```
"id=login-button"           → By.id("login-button")
"css=.shopping_cart_badge"  → By.cssSelector(".shopping_cart_badge")
"xpath=//div[@class='x']"   → By.xpath("//div[@class='x']")
```

`click()` retries up to `retry.count` (from config). `waitForClickable()` runs before each attempt — not just the first. An unsupported prefix throws immediately with the full locator string in the message.

→ [`docs/action-engine.md`](docs/action-engine.md) · [`docs/retry-mechanism.md`](docs/retry-mechanism.md)

---

### AssertEngine

`TestNG.assertEquals("1.00", "1.0")` fails. `Double.parseDouble()` has IEEE 754 precision issues on values like `28.99`. `AssertEngine.assertDoubleEquals()` normalizes both sides through `BigDecimal.compareTo()`:

```java
new BigDecimal("28.990").compareTo(BigDecimal.valueOf(28.99)) == 0  // ✅ true
```

This is the same approach used in financial system testing where trailing-zero differences between stored and displayed values are expected and shouldn't cause failures.

→ [`docs/assert-engine.md`](docs/assert-engine.md)

---

### Extent Reporting

Flat Extent reports dump every test as a top-level node — unreadable at any scale. `TestListener` groups methods under their parent class using `ConcurrentHashMap.computeIfAbsent()`, which is atomic — no duplicate parent nodes even when multiple threads try to create the same entry simultaneously:

```
📁 SauceDemoTest
   ✅ login
   ✅ cartFlow
   ❌ verifyCheckoutFlow  → stack trace + screenshot embedded
📁 AddToCartTest
   ✅ addItemsToCart
   ✅ subtotalvalidation
```

Each thread's active node is kept in `ThreadLocal<ExtentTest>` so `onTestSuccess` and `onTestFailure` always write to the correct node.

→ [`docs/extent-report.md`](docs/extent-report.md)

---

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| `selenium-java` | 4.21.0 | WebDriver core |
| `testng` | 7.10.2 | Test runner + parallel execution |
| `webdrivermanager` | 5.8.0 | Automatic ChromeDriver binary management |
| `extentreports` | 5.1.1 | HTML reporting |

---

## Docs

| File | What it covers |
|------|----------------|
| [`docs/architecture.md`](docs/architecture.md) | Layer responsibilities, dependency flow, why `src/main` vs `src/test` |
| [`docs/action-engine.md`](docs/action-engine.md) | Locator strategy, retry internals, why not PageFactory |
| [`docs/assert-engine.md`](docs/assert-engine.md) | BigDecimal vs Double, usage examples |
| [`docs/parallel-execution.md`](docs/parallel-execution.md) | ThreadLocal deep dive, dual modes, memory safety |
| [`docs/extent-report.md`](docs/extent-report.md) | Class-grouped hierarchy, thread-safe node creation, failure handling |
| [`docs/retry-mechanism.md`](docs/retry-mechanism.md) | What gets retried, what doesn't, and why |
| [`docs/page-inventory.md`](docs/page-inventory.md) | Item map pattern, price collection, sumPrices() |
| [`docs/page-checkout-overview.md`](docs/page-checkout-overview.md) | getUISubtotal() parsing, BigDecimal rationale |
| [`docs/test-add-to-cart.md`](docs/test-add-to-cart.md) | subtotalvalidation flow explained end to end |

---

## Author

**Karthick S** — Automation Test Engineer / SDET  
[GitHub](https://github.com/Karthick-1501) · [LinkedIn](https://linkedin.com/in/karthicks1520) · Chennai, India
