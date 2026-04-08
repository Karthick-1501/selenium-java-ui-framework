# ActionEngine

`src/main/java/com/atms/utils/action/ActionEngine.java`

---

## What it is

The single point of contact between test code and the WebDriver API.
No class outside `ActionEngine` calls `driver.findElement()` directly.

This means retry logic, wait strategy, and locator resolution are defined once — not scattered across 20 page classes.

---

## Locator Strategy

Locators are passed as prefixed strings:

```java
"id=login-button"           // → By.id("login-button")
"css=.shopping_cart_badge"  // → By.cssSelector(".shopping_cart_badge")
"xpath=//div[@class='x']"   // → By.xpath("//div[@class='x']")
```

`getBy()` parses the prefix and returns the appropriate `By` object. Unsupported prefixes throw immediately with a clear message — no silent fallback.

Constants live in `elements/` classes:

```java
// elements/LoginElements.java
public static final String LOGIN_BTN = "id=login-button";

// pages/LoginPage.java
ActionEngine.click(LoginElements.LOGIN_BTN);
```

---

## Retry Logic

`click()` retries up to `retry.count` times (from `execution.properties`).
An explicit `waitForClickable()` runs before each attempt.

```java
while (attempts < MAX_ATTEMPTS) {
    try {
        WaitUtils.waitForClickable(target);
        driver.findElement(target).click();
        return; // done
    } catch (Exception e) {
        attempts++;
        if (attempts == MAX_ATTEMPTS)
            throw new RuntimeException("click() failed after retries: " + locator, e);
    }
}
```

Why retry on `click()` but not blindly on `type()`?
- `click()` is most vulnerable to transient DOM state (overlays, animations, re-renders).
- `type()` caps at 2 — a field that can't be typed into after 2 tries is a locator bug, not a timing issue.

---

## Methods

| Method | Wait applied | Retry | Notes |
|--------|-------------|-------|-------|
| `click(locator)` | `waitForClickable` | Yes — `retry.count` from config | Primary interaction method |
| `type(locator, value)` | `waitForVisible` | Yes — hardcoded 2 | Clears before typing |
| `getText(locator)` | `waitForVisible` | No | Read-only, idempotent |
| `getTexts(locator)` | `waitForVisible` | No | Multi-element — returns `List<String>` |
| `isDisplayed(locator)` | `waitForVisible` | No | Returns `false` on exception, not throw |

### getTexts(locator)

Added to support multi-element reads without bypassing `ActionEngine`. Used by `InventoryPage.getAllItemPrices()` to collect all price labels on the page:

```java
public static List<String> getTexts(String locator) {
    By target = getBy(locator);
    WaitUtils.waitForVisible(target);
    return DriverManager.getDriver().findElements(target).stream()
            .map(el -> el.getText().trim())
            .collect(Collectors.toList());
}
```

This keeps `InventoryPage` free of any direct `WebElement` or `DriverManager` imports.

### isDisplayed(locator)

Returns `false` cleanly when an element is absent, rather than throwing after a full wait timeout:

```java
public static boolean isDisplayed(String locator) {
    try {
        By target = getBy(locator);
        WaitUtils.waitForVisible(target);
        return DriverManager.getDriver().findElement(target).isDisplayed();
    } catch (Exception e) {
        return false;
    }
}
```

Used by `CartPage.isRemoveButtonPresent()` so presence checks fail fast rather than waiting out the full explicit wait timeout.

---

## Why not use `@FindBy` / PageFactory?

PageFactory initializes elements at object construction time. Under `ThreadLocal` parallel execution, elements initialized on thread A can be stale or null when accessed on thread B. `driver.findElement()` on demand (lazy lookup) is the safer pattern for multi-threaded frameworks.
