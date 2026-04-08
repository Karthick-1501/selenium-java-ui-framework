# AddToCartTest

`src/test/java/com/atms/tests/AddToCartTest.java`

---

## What This Test Class Covers

`AddToCartTest` validates multi-item cart behaviour, subtotal accuracy, and DB-driven tax assertions. It exercises flows that `SauceDemoTest` doesn't: adding all 6 inventory items, verifying the displayed subtotal against a calculated sum, and asserting the tax against a rate fetched from PostgreSQL.

---

## Test 1 — addItemsToCart

```java
@Test
public void addItemsToCart() {
    basePage.launchApplication();
    loginPage.login(username, password);

    inventoryPage.addItem(item1);  // Sauce Labs Backpack
    inventoryPage.addItem(item2);  // Sauce Labs Bike
    inventoryPage.addItem(item3);  // Sauce Labs T-Shirt
    inventoryPage.addItem(item4);  // Sauce Labs Jacket
    inventoryPage.addItem(item5);  // Sauce Labs Onesie
    inventoryPage.addItem(item6);  // Test all
}
```

**What it proves:** All 6 `addItem()` calls succeed without error. Each item name resolves through `InventoryPage.ITEM_MAP` to its locator, and `ActionEngine.click()` reaches the element. If any item name is wrong in `testdata.properties`, the test fails immediately with a clear `"Unknown item name in ITEM_MAP: ..."` message.

---

## Test 2 — subtotalValidation

Doesn't hardcode an expected total — calculates it from the UI itself and compares against the checkout summary.

### Flow

```
Login → Add 6 items → Read all prices from inventory page
     → Navigate to cart → Checkout → Enter details → Continue
     → Read "Item total:" from Checkout Overview
     → Assert: calculated sum == displayed subtotal
```

### The assertion

```java
List<BigDecimal> prices        = inventoryPage.getAllItemPrices();
BigDecimal       calculatedTotal = inventoryPage.sumPrices(prices);

// ... navigate to checkout overview ...

BigDecimal uiSubtotal = overviewPage.getUISubtotal();
AssertEngine.assertBigDecimalEquals(uiSubtotal, calculatedTotal, "Subtotal mismatch");
```

`getUISubtotal()` reads `"Item total: $109.94"` from `[data-test='subtotal-label']`, strips the prefix, and returns a `BigDecimal`. `sumPrices()` uses `BigDecimal.add()` and `setScale(2, HALF_UP)`. `AssertEngine.assertBigDecimalEquals()` compares via `.compareTo()` — scale-safe.

### Why no hardcoded expected value?

If SauceDemo changes a product price, a hardcoded assertion immediately becomes stale and causes a false failure. The dynamic approach:

- Still catches real pricing bugs (checkout total doesn't match sum of displayed prices)
- Survives product price updates without touching test code
- Validates the *relationship* between inventory prices and checkout total, which is the actual business requirement

---

## Test 3 — validateTax

Fetches the tax rate from PostgreSQL and asserts the UI tax label against the computed expected value.

### Flow

```
Login → Add item1 → Cart → Checkout → Enter details → Continue
     → Read UI subtotal (BigDecimal)
     → Fetch tax rate from DB for item1
     → Compute expected tax: subtotal × rate / 100 (BigDecimal arithmetic)
     → Read UI tax label (BigDecimal)
     → Assert expected == actual
```

### The assertion

```java
BigDecimal subtotal    = overviewPage.getUISubtotal();
double     taxPercent  = taxPage.fetchTaxPercent(item1);
BigDecimal expectedTax = taxPage.calculateTax(subtotal, taxPercent);
BigDecimal uiTax       = taxPage.getUITax();

AssertEngine.assertBigDecimalEquals(uiTax, expectedTax, "Tax mismatch for single item");
```

The entire chain stays in `BigDecimal` — no conversion through `double` at any point.

### What this catches

- UI tax label not matching the configured tax rate
- Backend rounding that differs from `HALF_UP` — the assertion uses `setScale(2, HALF_UP)` to match how SauceDemo displays values
- DB misconfiguration — wrong rate stored for an item's classification

---

## Test 4 — validateTaxAllItems

Same flow and assertion as `validateTax` but with all 6 items in the cart, exercising the tax calculation against a larger subtotal.

```java
// All 6 items added before navigating to checkout
BigDecimal subtotal    = overviewPage.getUISubtotal();
double     taxPercent  = taxPage.fetchTaxPercent(item1);  // flat rate applies to all
BigDecimal expectedTax = taxPage.calculateTax(subtotal, taxPercent);
BigDecimal uiTax       = taxPage.getUITax();

AssertEngine.assertBigDecimalEquals(uiTax, expectedTax, "Tax mismatch for all items");
```

Uses `item1`'s tax rate — valid because SauceDemo applies a flat rate across all items in the same classification. A future extension could assert independently per item for mixed-rate carts.

---

## Shared Setup

All test data fields are read from `testdata.properties` in field initializers:

```java
private final String username  = ConfigManager.getTestData("valid.username");
private final String firstname = ConfigManager.getTestData("firstname");
private final String item1     = ConfigManager.getTestData("item1");
```

Page objects are instantiated as fields — stateless, safe to share across methods within the class under class-parallel mode.
