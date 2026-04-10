# UI Automation Framework

> Selenium · Java 17 · TestNG · Maven · ExtentReports · PostgreSQL

---

## What This Is

A UI test automation framework built on SauceDemo — an e-commerce app covering login, inventory, cart, checkout, and order confirmation. The target was to solve actual parallel execution, flakiness, and financial assertion problems, not just wrap Selenium in a Page Object pattern and call it a framework.

The things that make this worth looking at:

- `ThreadLocal<WebDriver>` — each thread gets its own browser, zero collision under parallel runs
- Two parallel modes (class-level and method-level) from the same codebase — the active mode is detected at runtime, no code changes needed
- `ActionEngine` is the only place `driver.findElement()` is called — retry logic and explicit waits live there, not scattered across page classes
- `AssertEngine` uses `BigDecimal.compareTo()` instead of string equality — no false failures from UI rendering `"1.00"` vs `"1.0"`
- DB-driven tax validation — tax rate fetched from PostgreSQL, computed in Java, asserted against the live UI with `BigDecimal` precision
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
│   │   │   ├── CheckoutCompletePage.java
│   │   │   └── TaxPage.java             ← DB query, tax calculation, UI tax reader
│   │   ├── reporting/
│   │   │   ├── ReportManager.java       ← ExtentReports singleton (synchronized init)
│   │   │   └── TestListener.java        ← ITestListener, class-grouped report, thread-safe
│   │   └── utils/
│   │       ├── action/
│   │       │   └── ActionEngine.java    ← Every WebDriver interaction + configurable retry
│   │       ├── assertion/
│   │       │   └── AssertEngine.java    ← BigDecimal numeric assertions
│   │       ├── db/
│   │       │   └── DBUtils.java         ← PostgreSQL connection factory
│   │       └── waits/
│   │           └── WaitUtils.java       ← Explicit wait wrappers
│   └── test/java/com/atms/
│       └── tests/
│           ├── SauceDemoTest.java        ← Core checkout and cart tests
│           ├── AddToCartTest.java        ← Multi-item cart, subtotal + tax validation
│           └── SampleTest.java          ← Smoke test
├── .env                                 ← secrets (gitignored — never committed)
├── .env.example                         ← safe template committed to source control
├── Environment/
│   ├── execution.properties             ← browser, base URL, retry count; DB creds via ${VAR}
│   └── testdata.properties             ← item names, form data; app creds via ${VAR}
├── docs/
│   ├── architecture.md                  ← Layer responsibilities + dependency flow
│   ├── action-engine.md                 ← Locator strategy, retry internals, why not PageFactory
│   ├── assert-engine.md                 ← BigDecimal vs Double, AssertEngine usage
│   ├── parallel-execution.md            ← ThreadLocal, dual modes, memory safety
│   ├── extent-report.md                 ← Class-grouped hierarchy, thread-safe parent creation
│   ├── retry-mechanism.md               ← What gets retried, what doesn't, and why
│   ├── db-layer.md                      ← DBUtils, TaxPage, schema design, tax calculation
│   ├── page-inventory.md                ← Item map pattern, price collection, sumPrices()
│   ├── page-checkout-overview.md        ← getUISubtotal() parsing + BigDecimal rationale
│   └── test-add-to-cart.md              ← subtotalValidation and validateTax flows explained
├── reports/                             ← ExtentReport.html + screenshots/ (gitignored)
├── testng-classes.xml                   ← parallel="classes" — one browser per class
├── testng-methods.xml                   ← parallel="methods" — one browser per test
└── pom.xml
```

---

## Quick Start

**Prerequisites:** Java 17+, Maven 3.8+, Chrome installed, PostgreSQL running with `at-db` database

```bash
git clone https://github.com/Karthick-1501/selenium-java-ui-framework.git
cd selenium-java-ui-framework

# copy secrets template and fill in your values
cp .env.example .env

# one browser per class — faster
mvn test -DsuiteXmlFile=testng-classes.xml

# one browser per test method — fully isolated
mvn test -DsuiteXmlFile=testng-methods.xml
```

Report lands at `reports/ExtentReport.html` after each run.

---

## Secrets Setup

Credentials and DB connection details are never stored in version-controlled files.
They live in a `.env` file at the project root, which is gitignored.

**Step 1 — copy the template:**
```bash
cp .env.example .env
```

**Step 2 — fill in your values:**
```env
# .env
DB_URL=jdbc:postgresql://localhost:5432/at-db
DB_USERNAME=postgres
DB_PASSWORD=your_actual_password

APP_USERNAME=standard_user
APP_PASSWORD=your_actual_password

DB_TABLE_PRODUCT=your_product_schema.your_product_table
DB_TABLE_CLASSIFICATION=your_classification_schema.your_classification_table
DB_TABLE_TAX=your_tax_schema.your_tax_table

DB_COL_TAX_RATE=your_tax_rate_column
DB_COL_PRODUCT_ID=your_product_id_column
DB_COL_CLASSIFICATION_ITEM_ID=your_classification_item_id_column
DB_COL_CLASSIFICATION_CODE=your_classification_code_column
DB_COL_TAX_CODE=your_tax_code_column
DB_COL_PRODUCT_NAME=your_product_name_column
```

**Step 3 — run as normal.** `ConfigManager` loads `.env` automatically at startup via `java-dotenv`.

> In CI/CD, set these as environment variables in your pipeline instead of using a `.env` file. `ConfigManager` falls back to `System.getenv()` automatically.

---

## Configuration

`Environment/execution.properties`
```properties
browser=chrome
base.url=https://www.saucedemo.com
retry.count=2

# DB connection — resolved from .env at runtime
db.url=${DB_URL}
db.username=${DB_USERNAME}
db.password=${DB_PASSWORD}

# DB table names — resolved from .env at runtime
db.table.product=${DB_TABLE_PRODUCT}
db.table.classification=${DB_TABLE_CLASSIFICATION}
db.table.tax=${DB_TABLE_TAX}

# DB column names — resolved from .env at runtime
db.col.tax.rate=${DB_COL_TAX_RATE}
db.col.product.id=${DB_COL_PRODUCT_ID}
db.col.classification.item.id=${DB_COL_CLASSIFICATION_ITEM_ID}
db.col.classification.code=${DB_COL_CLASSIFICATION_CODE}
db.col.tax.code=${DB_COL_TAX_CODE}
db.col.product.name=${DB_COL_PRODUCT_NAME}
```

`Environment/testdata.properties`
```properties
# App credentials resolved from .env at runtime
valid.username=${APP_USERNAME}
valid.password=${APP_PASSWORD}

firstname=Karthick
lastname=S
postcode=608001

item1=Sauce Labs Backpack
item2=Sauce Labs Bike
item3=Sauce Labs T-Shirt
item4=Sauce Labs Jacket
item5=Sauce Labs Onesie
item6=Test all

backpack.price=29.99
```

Switching environments means updating your `.env` file (or CI env vars). The property files themselves require no edits.

---

## Test Coverage

| Class | Test | What it covers |
|-------|------|----------------|
| `SampleTest` | `launchUrl` | Smoke — URL loads and contains `saucedemo` |
| `SauceDemoTest` | `login` | Credential entry → successful login |
| `SauceDemoTest` | `cartFlow` | Add backpack → assert cart badge count → remove |
| `SauceDemoTest` | `verifyCheckoutFlow` | Full flow: login → add → cart → checkout → confirm + price assertion |
| `AddToCartTest` | `addItemsToCart` | Add all 6 inventory items via name-driven `addItem()` |
| `AddToCartTest` | `subtotalValidation` | Collect prices from inventory DOM → sum → assert against checkout overview subtotal |
| `AddToCartTest` | `validateTax` | Single item: fetch tax rate from DB → compute expected → assert against UI tax label |
| `AddToCartTest` | `validateTaxAllItems` | All 6 items: same DB-driven tax validation against the full cart subtotal |

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

`getTexts(locator)` supports multi-element collection without bypassing ActionEngine — used by `InventoryPage.getAllItemPrices()` to read all price labels in one call.

→ [`docs/action-engine.md`](docs/action-engine.md) · [`docs/retry-mechanism.md`](docs/retry-mechanism.md)

---

### DB-Driven Tax Validation

Tax rates are not hardcoded. `TaxPage.fetchTaxPercent()` queries PostgreSQL using a JOIN across three schemas. Table names, schema names, and column names are **never stored in source code** — they are loaded at runtime from `.env` via `ConfigManager`, so no schema details appear in commits.

```java
String productTable        = ConfigManager.getExecution("db.table.product");
String classificationTable = ConfigManager.getExecution("db.table.classification");
String taxTable            = ConfigManager.getExecution("db.table.tax");
// column names loaded the same way ...

String query = String.format("""
    SELECT t.%s
    FROM %s p
    JOIN %s c ON p.%s = c.%s
    JOIN %s t ON c.%s = t.%s
    WHERE p.%s = ?
""", colTaxRate, productTable, classificationTable,
     colProductId, colClassItemId, taxTable,
     colClassCode, colTaxCode, colProductName);
```

The rate is returned and passed to `calculateTax()`, which uses `BigDecimal` arithmetic:

```java
public BigDecimal calculateTax(BigDecimal subtotal, double percent) {
    return subtotal
        .multiply(BigDecimal.valueOf(percent))
        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
}
```

The test then asserts the computed expected tax against what the UI displays — all in `BigDecimal` via `AssertEngine.assertBigDecimalEquals()`.

→ [`docs/db-layer.md`](docs/db-layer.md)

---

### AssertEngine

`TestNG.assertEquals("1.00", "1.0")` fails. `Double.parseDouble()` has IEEE 754 precision issues on values like `28.99`. `AssertEngine.assertDoubleEquals()` normalizes both sides through `BigDecimal.compareTo()`:

```java
new BigDecimal("28.990").compareTo(BigDecimal.valueOf(28.99)) == 0  // ✅ true
```

For monetary comparisons where both sides are already `BigDecimal` (tax, subtotal), `AssertEngine.assertBigDecimalEquals()` uses `.compareTo()` directly, avoiding any lossy conversion through `double`.

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
   ✅ subtotalValidation
   ✅ validateTax
   ✅ validateTaxAllItems
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
| `postgresql` | 42.7.3 | JDBC driver for DB-driven validation |
| `dotenv-java` | 3.0.0 | Loads `.env` secrets at runtime |

---

## Docs

| File | What it covers |
|------|----------------|
| [`docs/architecture.md`](docs/architecture.md) | Layer responsibilities, dependency flow, why `src/main` vs `src/test` |
| [`docs/action-engine.md`](docs/action-engine.md) | Locator strategy, retry internals, `getTexts()`, why not PageFactory |
| [`docs/assert-engine.md`](docs/assert-engine.md) | BigDecimal vs Double, `assertDoubleEquals` vs `assertBigDecimalEquals` |
| [`docs/parallel-execution.md`](docs/parallel-execution.md) | ThreadLocal deep dive, dual modes, memory safety |
| [`docs/extent-report.md`](docs/extent-report.md) | Class-grouped hierarchy, thread-safe node creation, failure handling |
| [`docs/retry-mechanism.md`](docs/retry-mechanism.md) | What gets retried, what doesn't, and why |
| [`docs/db-layer.md`](docs/db-layer.md) | DBUtils, TaxPage, schema design, tax calculation, full assertion flow |
| [`docs/page-inventory.md`](docs/page-inventory.md) | Item map pattern, price collection, sumPrices() |
| [`docs/page-checkout-overview.md`](docs/page-checkout-overview.md) | getUISubtotal() parsing, BigDecimal rationale |
| [`docs/test-add-to-cart.md`](docs/test-add-to-cart.md) | subtotalValidation and validateTax flows explained end to end |

---

## Author

**Karthick S** — Automation Test Engineer / SDET  
[GitHub](https://github.com/Karthick-1501) · [LinkedIn](https://linkedin.com/in/karthicks1520) · Chennai, India
