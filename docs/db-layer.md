# DB Layer

`src/main/java/com/atms/utils/db/DBUtils.java`  
`src/main/java/com/atms/pages/TaxPage.java`

---

## Why DB Validation Exists

SauceDemo's tax amount is calculated server-side and displayed as a single label — no breakdown of rate or basis is shown. Asserting `"Tax: $2.40"` against a hardcoded value tells you nothing about whether the calculation logic is correct. If someone changes the tax rate, the test just updates the hardcode and moves on.

DB-driven validation closes this gap:

1. Fetch the actual configured tax rate from the database
2. Apply the same formula the application uses
3. Assert the computed expected value against what the UI shows

If the rate changes, the test adapts — it's asserting the *relationship* between the config and the UI, not a snapshot.

---

## DBUtils — Connection Factory

```java
public class DBUtils {

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    ConfigManager.getExecution("db.url"),
                    ConfigManager.getExecution("db.username"),
                    ConfigManager.getExecution("db.password")
            );
        } catch (Exception e) {
            throw new RuntimeException("DB connection failed", e);
        }
    }
}
```

Connection credentials are read from `execution.properties` on every call — lazy, not cached as static fields. This avoids a class-load-time failure if the DB isn't reachable when the JVM starts.

`execution.properties`:
```properties
db.url=jdbc:postgresql://localhost:5432/at-db
db.username=postgres
db.password=root
```

Connections are opened with `try-with-resources` in the caller (`TaxPage`) — they close automatically after the query completes.

---

## Database Schema

The tax system spans three schemas, each with a focused responsibility:

```
product.id_itm          → item master (id, name, description)
classification.cls_cd   → maps items to classification codes
tax.tx_cfg              → maps classification codes to tax rates
```

This separation is deliberate — the same tax rate applies to all items in a classification. Adding a new item means inserting a row in `product.id_itm` and mapping it to an existing classification. The tax rate is inherited, not duplicated per item.

---

## TaxPage — Query, Calculate, Read

### fetchTaxPercent(String itemName)

Joins across all three schemas to retrieve the tax rate for a given item name:

```java
String query = """
    SELECT t.tx_rt
    FROM product.id_itm p
    JOIN classification.cls_cd c ON p.id_itm = c.itm_id
    JOIN tax.tx_cfg t ON c.cl_cd = t.cl_cd
    WHERE p.name = ?
""";
```

Uses a `PreparedStatement` with a parameterised `?` — no string concatenation, no SQL injection risk. Returns `0` if no row is found, which will produce a zero tax expectation and surface as a test failure rather than a silent pass.

### calculateTax(BigDecimal subtotal, double percent)

```java
public BigDecimal calculateTax(BigDecimal subtotal, double percent) {
    return subtotal
            .multiply(BigDecimal.valueOf(percent))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
}
```

The formula is exclusive tax — applied on top of the subtotal. `RoundingMode.HALF_UP` matches how SauceDemo rounds displayed values. Result is always 2 decimal places, matching the UI.

**Why not `double` math?**

```java
//  floating-point trap
double tax = 109.94 * 8.0 / 100;  // → 8.795199999999999
Math.round(tax * 100.0) / 100.0;   // → 8.8 — looks right, but fragile
```

`BigDecimal` arithmetic is exact at every step — no rounding artifacts mid-calculation.

### getUITax()

```java
public BigDecimal getUITax() {
    String text = ActionEngine.getText(InventoryElements.TAX_LABEL);
    return new BigDecimal(text.replace("Tax: $", "").trim());
}
```

Reads the `[data-test='tax-label']` element via `ActionEngine`, strips the label prefix, and returns a `BigDecimal`. This stays in `BigDecimal` so the comparison in the test never touches `double`.

---

## Full Assertion Flow

```java
// 1. Navigate to checkout overview
BigDecimal subtotal   = overviewPage.getUISubtotal();     // "Item total: $29.99" → BigDecimal

// 2. Fetch tax rate from DB
double taxPercent     = taxPage.fetchTaxPercent(item1);   // e.g. 8.0

// 3. Compute expected tax using BigDecimal arithmetic
BigDecimal expectedTax = taxPage.calculateTax(subtotal, taxPercent);  // → 2.40

// 4. Read actual tax from UI
BigDecimal uiTax      = taxPage.getUITax();               // "Tax: $2.40" → BigDecimal

// 5. Assert — scale-safe, no double conversion
AssertEngine.assertBigDecimalEquals(uiTax, expectedTax, "Tax mismatch");
```

The entire chain from DB to UI never passes through `double`. No precision is lost at any step.

---

## Tests Using the DB Layer

| Test | Items | What it validates |
|------|-------|-------------------|
| `validateTax` | 1 (Backpack) | Tax on a single-item subtotal matches the DB-configured rate |
| `validateTaxAllItems` | 6 (full cart) | Tax on a full-cart subtotal matches the same DB rate |

Both tests use `item1`'s tax rate fetched from the DB — this is valid because SauceDemo applies a flat rate across all items in the same classification. A future extension could assert per-item rates independently for mixed-classification carts.
