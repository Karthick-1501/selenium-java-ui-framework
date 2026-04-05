# Retry Mechanism

---

## Why Retry Exists

Selenium tests fail transiently for reasons that have nothing to do with the application being broken:

- Element is in the DOM but not yet clickable (CSS animation, overlay fading out)
- Brief DOM re-render between `findElement()` and `click()`
- Network lag causing a delayed response that shifts the layout
- `StaleElementReferenceException` from a partial page reload

Retry absorbs these transient failures without masking real bugs.

---

## Configuration

Retry count is driven by `Environment/execution.properties`:

```properties
retry.count=2
```

Change the value — behaviour changes everywhere. No recompile needed.

`ActionEngine` reads it once at class load time:

```java
private static final int MAX_ATTEMPTS =
    Integer.parseInt(ConfigManager.getExecution("retry.count"));
```

---

## How It Works

```java
public static void click(String locator) {
    By target = getBy(locator);
    int attempts = 0;

    while (attempts < MAX_ATTEMPTS) {
        try {
            WaitUtils.waitForClickable(target);      // explicit wait first
            driver.findElement(target).click();
            return;                                  // success — exit immediately
        } catch (Exception e) {
            attempts++;
            if (attempts == MAX_ATTEMPTS)
                throw new RuntimeException("click() failed after " + MAX_ATTEMPTS + " attempts: " + locator, e);
        }
    }
}
```

Key details:
- `waitForClickable()` runs **before** each attempt — not just the first one
- On the final attempt, the original exception is wrapped and rethrown with locator context
- The method returns immediately on success — no wasted iterations

---

## What Gets Retried vs What Doesn't

| Method | Retried | Reason |
|--------|---------|--------|
| `click()` | ✅ Yes — `retry.count` from config | Most vulnerable to transient DOM state |
| `type()` | ✅ Yes — hardcoded 2 | Fields can briefly be un-interactable |
| `getText()` | ❌ No | Read-only, idempotent — if it fails twice it's a real locator issue |

---

## What Retry Does NOT Do

Retry is not a crutch for bad locators or slow tests. It doesn't:

- Hide application bugs (a real failure still fails after `MAX_ATTEMPTS`)
- Replace proper explicit waits (waits run inside each retry, not instead of it)
- Retry at the test method level (that would mask logic failures — this is action-level only)

If a test consistently hits the retry limit, the locator or wait strategy needs fixing — not the retry count.
