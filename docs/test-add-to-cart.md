# AddToCartTest

`src/test/java/com/atms/tests/AddToCartTest.java`

---

## What This Test Class Covers

`AddToCartTest` validates multi-item cart behaviour and end-to-end subtotal accuracy. It exercises a code path that `SauceDemoTest` doesn't: adding all 6 inventory items, then verifying that the checkout overview page displays the correct sum.

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

**What it proves:** All 6 `addItem()` calls succeed without error — each item name resolves correctly through `InventoryPage.ITEM_MAP` to its locator, and `ActionEngine.click()` reaches the element. If any item name is wrong in `testdata.properties`, the test fails immediately with a clear `"Invalid item: ..."` message.

**Item names come from config:**

```properties
# testdata.properties
item1=Sauce Labs Backpack
item2=Sauce Labs Bike
item3=Sauce Labs T-Shirt
item4=Sauce Labs Jacket
item5=Sauce Labs Onesie
item6=Test all
```

---

## Test 2 — subtotalvalidation

This is the most thorough price test in the suite. It doesn't hardcode an expected total — it calculates the expected value from the UI itself and compares it against the checkout summary.

### Flow

```
Login → Add 6 items → Read all prices from inventory page
     → Navigate to cart → Checkout → Enter details → Continue
     → Read "Item total:" from Checkout Overview
     → Assert: calculated sum == displayed subtotal
```

### The assertion

```java
List<BigDecimal> prices = inventoryPage.getAllItemPrices();
BigDecimal calculatedTotal = inventoryPage.sumPrices(prices);

// ... navigate to checkout overview ...

BigDecimal uiSubtotal = overviewPage.getUISubtotal();
Assert.assertEquals(uiSubtotal, calculatedTotal);
```

`getUISubtotal()` reads `"Item total: $109.94"` from `[data-test='subtotal-label']`, strips the prefix, and returns a `BigDecimal`:

```java
public BigDecimal getUISubtotal() {
    String subtotalText = ActionEngine.getText(CheckoutOverviewElements.ITEM_SUBTOTAL);
    String value = subtotalText.replace("Item total: $", "");
    return new BigDecimal(value);
}
```

`sumPrices()` uses `BigDecimal.add()` and `setScale(2, HALF_UP)` — same scale as the UI output. `Assert.assertEquals(BigDecimal, BigDecimal)` compares by value, not reference.

### Why no hardcoded expected value?

If SauceDemo changes a product price, a hardcoded assertion immediately becomes stale and causes a false failure. The dynamic approach:

- Still catches real pricing bugs (UI subtotal doesn't match the sum of displayed prices)
- Survives product price updates without touching test code
- Validates the *relationship* between inventory prices and the checkout total, which is the actual business requirement

### What this test can catch

- Checkout overview showing a different subtotal than the sum of the inventory prices
- Backend rounding error producing a subtotal that doesn't match the displayed item prices
- A missing or double-counted item in the subtotal calculation
- Currency formatting differences causing a parse failure (which surfaces as a test error, not a false pass)

---

## Shared Setup

All test data fields are read from `testdata.properties` in field initializers — not in `@BeforeMethod`. This means `ConfigManager` is called once at class construction:

```java
private final String username  = ConfigManager.getTestData("valid.username");
private final String firstname = ConfigManager.getTestData("firstname");
// ...
String item1 = ConfigManager.getTestData("item1");
```

Page objects are instantiated as fields — stateless, so safe to share across methods within the class even under class-parallel mode.
