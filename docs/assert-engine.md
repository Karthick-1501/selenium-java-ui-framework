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

This is a real issue in checkout and tax validation flows where the DB stores `28.990000` and the UI renders `$28.99`.

---

## The Fix: BigDecimal.compareTo()

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

---

## Usage

```java
// Strip currency symbol in the caller — assertion receives a clean numeric string
String price = overviewPage.getItemPrice();                     // "$29.99"
AssertEngine.assertDoubleEquals(price.replace("$", ""), 29.99); // ✅

// Cart badge count — "1" vs 1.0
String count = inventoryPage.getCartCount();                    // "1"
AssertEngine.assertDoubleEquals(count, 1.0);                    // ✅
```

---

## Why Not Just `Double.parseDouble()`?

`Double.parseDouble("28.99")` introduces floating-point representation error.
`28.99` in IEEE 754 double precision is actually `28.989999999999998436805981327779591083526611328125`.
`BigDecimal` stores exact decimal values — no precision loss, no rounding artifacts.

This matters in financial and tax assertion flows where `28.99 != 28.990000000001` must not be a fluke.
