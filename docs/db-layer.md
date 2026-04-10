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

Connection credentials are read from `execution.properties` at runtime — the actual values come from `.env` (gitignored). This avoids committing any environment-specific config to source control.

`execution.properties` (committed — no secrets):
```properties
db.url=${DB_URL}
db.username=${DB_USERNAME}
db.password=${DB_PASSWORD}
```

`.env` (gitignored — real values live here):
```env
DB_URL=jdbc:postgresql://localhost:5432/your-db
DB_USERNAME=your_user
DB_PASSWORD=your_password
```

Connections are opened with `try-with-resources` in the caller (`TaxPage`) — they close automatically after the query completes.

---

## Database Schema

The tax system spans three schemas, each with a focused responsibility. The actual schema and table names are not stored in source code — they are configured via `.env` and resolved at runtime through `ConfigManager`.

```
product schema      → item master (id, name, description)
classification schema → maps items to classification codes
tax schema          → maps classification codes to tax rates
```

This separation is deliberate — the same tax rate applies to all items in a classification. Adding a new item means inserting into the product table and mapping it to an existing classification. The tax rate is inherited, not duplicated per item.

Configure your schema and table names in `.env`:
```env
DB_TABLE_PRODUCT=your_product_schema.your_product_table
DB_TABLE_CLASSIFICATION=your_classification_schema.your_classification_table
DB_TABLE_TAX=your_tax_schema.your_tax_table
```

Configure your column names in `.env`:
```env
DB_COL_TAX_RATE=your_tax_rate_column
DB_COL_PRODUCT_ID=your_product_id_column
DB_COL_CLASSIFICATION_ITEM_ID=your_classification_item_id_column
DB_COL_CLASSIFICATION_CODE=your_classification_code_column
DB_COL_TAX_CODE=your_tax_code_column
DB_COL_PRODUCT_NAME=your_product_name_column
```

See `.env.example` for the full list of keys.

---

## TaxPage — Query, Calculate, Read

### fetchTaxPercent(String itemName)

Loads table and column names from `ConfigManager` at runtime, then builds and executes a JOIN across all three schemas to retrieve the tax rate for a given item:

```java
String productTable        = ConfigManager.getExecution("db.table.product");
String classificationTable = ConfigManager.getExecution("db.table.classification");
String taxTable            = ConfigManager.getExecution("db.table.tax");

String colTaxRate     = ConfigManager.getExecution("db.col.tax.rate");
String colProductId   = ConfigManager.getExecution("db.col.product.id");
String colClassItemId = ConfigManager.getExecution("db.col.classification.item.id");
String colClassCode   = ConfigManager.getExecution("db.col.classification.code");
String colTaxCode     = ConfigManager.getExecution("db.col.tax.code");
String colProductName = ConfigManager.getExecution("db.col.product.name");

String query = String.format("""
    SELECT t.%s
    FROM %s p
    JOIN %s c ON p.%s = c.%s
    JOIN %s t ON c.%s = t.%s
    WHERE p.%s = ?
""", colTaxRate,
     productTable,
     classificationTable, colProductId, colClassItemId,
     taxTable, colClassCode, colTaxCode,
     colProductName);
```

No table names, column names, or schema identifiers appear as string literals in the committed code. The query structure is visible; the identifiers are not.

Uses a `PreparedStatement` with a parameterised `?` for the item name — no string concatenation, no SQL injection risk. Returns `0` if no row is found, which produces a zero tax expectation and surfaces as a test failure rather than a silent pass.

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
// floating-point trap
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
BigDecimal subtotal    = overviewPage.getUISubtotal();     // "Item total: $29.99" → BigDecimal

// 2. Fetch tax rate from DB (table/column names come from .env at runtime)
double taxPercent      = taxPage.fetchTaxPercent(item1);   // e.g. 8.0

// 3. Compute expected tax using BigDecimal arithmetic
BigDecimal expectedTax = taxPage.calculateTax(subtotal, taxPercent);  // → 2.40

// 4. Read actual tax from UI
BigDecimal uiTax       = taxPage.getUITax();               // "Tax: $2.40" → BigDecimal

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
