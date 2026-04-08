# AssertEngine

`src/main/java/com/atms/utils/assertion/AssertEngine.java`

---

## The Problem

UI text from the browser is always a `String`. When asserting numeric values — prices, quantities, totals — the obvious approach is:

```java
Assert.assertEquals(priceText, "29.99"); // ❌ brittle
```

This breaks the moment the UI renders `"29.990"`, `" 29.99"`, or `"$29.99"` (before the caller strips `$`). Even `Double.parseDouble()` comparison has a trap:

```java
Assert.assertEquals(Double.parseDouble("1.00"), 1.0); // ✅ passes
Assert.assertEquals("1.00", String.valueOf(1.0));      // ❌ "1.00" != "1.0"
```

Standard `assertEquals` on strings treats `"1.00"` and `"1.0"` as different values — causing false failures on fields that are numerically identical.

`Double` itself introduces another problem. `28.99` in IEEE 754 double precision is actually `28.989999999999998...` — which means arithmetic done in `double` can drift away from the exact value the UI shows.

---

## The Fix: BigDecimal.compareTo()

Both assertion methods normalize to `BigDecimal` and use `.compareTo()`, which compares mathematical value independent of scale.

### assertDoubleEquals — for UI string vs known expected value

```java
public static void assertDoubleEquals(String actualUI, double expected) {
    BigDecimal actual     = new BigDecimal(actualUI.trim());
    BigDecimal expectedBD = BigDecimal.valueOf(expected);

    if (actual.compareTo(expectedBD) != 0) {
        Assert.fail("Numeric assertion failed — Expected: " + expectedBD + " | Actual: " + actual);
    }
}
```

`BigDecimal.compareTo()` compares mathematical value, not scale:

```
new BigDecimal("28.990").compareTo(BigDecimal.valueOf(28.99)) == 0  // ✅ true
new BigDecimal("1.00").compareTo(BigDecimal.valueOf(1.0))    == 0  // ✅ true
new BigDecimal("29.00").compareTo(BigDecimal.valueOf(28.99)) == 0  // ❌ false → test fails correctly
```

### assertBigDecimalEquals — for BigDecimal vs BigDecimal (monetary/tax flows)

```java
public static void assertBigDecimalEquals(BigDecimal actual, BigDecimal expected, String message) {
    if (actual.compareTo(expected) != 0) {
        Assert.fail(message + " — Expected: " + expected + " | Actual: " + actual);
    }
}
```

Used when both the expected and actual values are already `BigDecimal` — tax calculations, subtotal comparisons. Avoids any lossy conversion through `double`.

---

## Usage

```java
// Strip currency symbol before passing — assertion receives a clean numeric string
String price = overviewPage.getItemPrice();                     // "$29.99"
AssertEngine.assertDoubleEquals(price.replace("$", ""), 29.99); // ✅

// Cart badge count — "1" vs 1.0
String count = inventoryPage.getCartCount();                    // "1"
AssertEngine.assertDoubleEquals(count, 1.0);                    // ✅

// Tax validation — both sides are BigDecimal, no double involved
BigDecimal expectedTax = taxPage.calculateTax(subtotal, percent);
BigDecimal uiTax       = taxPage.getUITax();
AssertEngine.assertBigDecimalEquals(uiTax, expectedTax, "Tax mismatch"); // ✅

// Subtotal validation — same pattern
BigDecimal calculatedTotal = inventoryPage.sumPrices(prices);
BigDecimal uiSubtotal      = overviewPage.getUISubtotal();
AssertEngine.assertBigDecimalEquals(uiSubtotal, calculatedTotal, "Subtotal mismatch"); // ✅
```

---

## Which Method to Use

| Scenario | Method |
|----------|--------|
| UI string vs known numeric constant | `assertDoubleEquals(String, double)` |
| Two `BigDecimal` values (tax, subtotal, price totals) | `assertBigDecimalEquals(BigDecimal, BigDecimal, String)` |

The rule: if either value passed through `double` at any point, it may have lost precision. Use `assertBigDecimalEquals` whenever both sides of the comparison are or can be `BigDecimal`.
