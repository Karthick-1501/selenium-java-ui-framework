# CheckoutOverviewPage

`src/main/java/com/atms/pages/CheckoutOverviewPage.java`

---

## Responsibilities

`CheckoutOverviewPage` handles interactions on the SauceDemo order summary screen — the step between shipping details and order confirmation.

| Method | What it does |
|--------|-------------|
| `getItemPrice()` | Returns the displayed item price as a raw string (e.g. `"$29.99"`) |
| `getUISubtotal()` | Parses the `"Item total: $X.XX"` label into a `BigDecimal` |
| `clickFinish()` | Clicks the Finish button to place the order |

---

## getUISubtotal() — parsing the subtotal label

The SauceDemo subtotal label renders as:

```
Item total: $109.94
```

`getUISubtotal()` reads this text, strips the non-numeric prefix, and returns a `BigDecimal` suitable for exact comparison:

```java
public BigDecimal getUISubtotal() {
    String subtotalText = ActionEngine.getText(CheckoutOverviewElements.ITEM_SUBTOTAL);
    String value = subtotalText.replace("Item total: $", "");
    return new BigDecimal(value);
}
```

**Why `BigDecimal` and not `double`?**

`Double.parseDouble("109.94")` introduces IEEE 754 representation error. `BigDecimal("109.94")` stores the exact decimal value. When this result is compared against `InventoryPage.sumPrices()` — which also produces a `BigDecimal` — the comparison is numerically exact.

**Locator:**

```java
// CheckoutOverviewElements.java
public static final String ITEM_SUBTOTAL = "css=[data-test='subtotal-label']";
```

`data-test` attributes are the most stable locator type in SauceDemo — they're set explicitly for test tooling and won't shift with styling changes.

---

## getItemPrice() — raw price string

```java
public String getItemPrice() {
    return ActionEngine.getText(CheckoutOverviewElements.ITEM_PRICE);
}
```

Returns the price with the `$` symbol included (e.g. `"$29.99"`). The caller is responsible for stripping the currency symbol before passing to `AssertEngine`:

```java
String price = overviewPage.getItemPrice();
AssertEngine.assertDoubleEquals(price.replace("$", ""), 29.99);
```

This keeps the page object focused on retrieval — transformation and assertion stay in the test or engine layer.

---

## Locators

```java
// CheckoutOverviewElements.java
public static final String ITEM_PRICE    = "css=.inventory_item_price";
public static final String ITEM_SUBTOTAL = "css=[data-test='subtotal-label']";
public static final String FINISH_BTN    = "id=finish";
```
