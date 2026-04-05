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
        throw new RuntimeException("Invalid item: " + itemName);
    }
    click(locator);
}
```

**Why this matters:** Adding new items to test = add one line to `testdata.properties` + one entry in the map. Test methods don't touch locators.

---

## Price Collection — getAllItemPrices()

Reads every price element from the inventory page and returns them as a `List<BigDecimal>`:

```java
public List<BigDecimal> getAllItemPrices() {
    List<WebElement> priceElements =
        DriverManager.getDriver().findElements(InventoryElements.PRICE_LIST);

    List<BigDecimal> prices = new ArrayList<>();
    for (WebElement el : priceElements) {
        String clean = el.getText().replace("$", "");  // "$29.99" → "29.99"
        prices.add(new BigDecimal(clean));
    }
    return prices;
}
```

The locator `[data-test='inventory-item-price']` is a `By` object (not a string prefix), stored directly in `InventoryElements` for multi-element collection:

```java
// InventoryElements.java
public static By PRICE_LIST = By.cssSelector("[data-test='inventory-item-price']");
```

This is intentional — `ActionEngine.getText()` handles single elements via string locators. Multi-element collection needs `findElements()` directly, which takes `By`.

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
| `AddToCartTest.subtotalvalidation` | `getAllItemPrices()` + `sumPrices()` to compute expected subtotal |
| `SauceDemoTest.*` | `addBackpackToCart()`, `openCart()`, `getCartCount()` |
