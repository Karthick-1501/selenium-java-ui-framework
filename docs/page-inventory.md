# InventoryPage

`src/main/java/com/atms/pages/InventoryPage.java`

---

## Responsibilities

`InventoryPage` handles everything on the SauceDemo product listing page:

- Adding specific items to cart by name (via `addItem`)
- Adding the backpack specifically (legacy shorthand via `addBackpackToCart`)
- Reading cart badge count
- Collecting all visible prices as `BigDecimal`
- Summing a price list to a precise 2-decimal total

---

## Item Map — name-driven cart actions

Rather than scattering item locators through test code, `InventoryPage` owns a static lookup table that maps human-readable product names to their `ActionEngine`-compatible locators:

```java
private static final Map<String, String> ITEM_MAP = new HashMap<>();

static {
    ITEM_MAP.put("Sauce Labs Backpack", InventoryElements.ADD_TO_CART_BACKPACK);
    ITEM_MAP.put("Sauce Labs Bike",     InventoryElements.ADD_TO_CART_BIKE);
    ITEM_MAP.put("Sauce Labs T-Shirt",  InventoryElements.ADD_TO_CART_TSHIRT);
    ITEM_MAP.put("Sauce Labs Jacket",   InventoryElements.ADD_TO_CART_JACKET);
    ITEM_MAP.put("Sauce Labs Onesie",   InventoryElements.ADD_TO_CART_ONESIE);
    ITEM_MAP.put("Test all",            InventoryElements.ADD_TO_CART_TESTALL);
}
```

Test code reads item names from `testdata.properties` and passes them to `addItem()`:

```java
inventoryPage.addItem(ConfigManager.getTestData("item1"));  // "Sauce Labs Backpack"
inventoryPage.addItem(ConfigManager.getTestData("item2"));  // "Sauce Labs Bike"
```

If an unknown item name is passed, `addItem()` throws immediately:

```java
public void addItem(String itemName) {
    String locator = ITEM_MAP.get(itemName);
    if (locator == null) {
        throw new RuntimeException("Unknown item name in ITEM_MAP: " + itemName);
    }
    click(locator);
}
```

**Why this matters:** Adding new items to test = add one line to `testdata.properties` + one entry in the map. Test methods don't touch locators.

---

## Price Collection — getAllItemPrices()

Reads every price element from the inventory page and returns them as a `List<BigDecimal>`. Uses `ActionEngine.getTexts()` so the page never calls `driver.findElements()` directly:

```java
public List<BigDecimal> getAllItemPrices() {
    List<String> rawPrices = ActionEngine.getTexts(InventoryElements.PRICE_LIST);
    List<BigDecimal> prices = new ArrayList<>();
    for (String raw : rawPrices) {
        prices.add(new BigDecimal(raw.replace("$", "")));
    }
    return prices;
}
```

The locator is a standard string-prefixed constant in `InventoryElements`:

```java
// InventoryElements.java
public static final String PRICE_LIST = "css=[data-test='inventory-item-price']";
```

`ActionEngine.getTexts()` handles multi-element collection internally — all WebDriver interaction stays inside the engine.

---

## Price Summation — sumPrices()

```java
public BigDecimal sumPrices(List<BigDecimal> prices) {
    BigDecimal total = BigDecimal.ZERO;
    for (BigDecimal price : prices) {
        total = total.add(price);
    }
    return total.setScale(2, RoundingMode.HALF_UP);
}
```

`setScale(2, HALF_UP)` ensures the result is always 2 decimal places regardless of how many items are summed. This matches how the SauceDemo UI displays the subtotal and prevents a `BigDecimal.compareTo()` scale mismatch on the assertion side.

---

## Used In

| Test | Usage |
|------|-------|
| `AddToCartTest.addItemsToCart` | `addItem()` × 6 to verify all items can be added |
| `AddToCartTest.subtotalValidation` | `getAllItemPrices()` + `sumPrices()` to compute expected subtotal |
| `AddToCartTest.validateTax` / `validateTaxAllItems` | `addItem()` for cart setup before DB-driven tax assertion |
| `SauceDemoTest.*` | `addBackpackToCart()`, `openCart()`, `getCartCount()` |
