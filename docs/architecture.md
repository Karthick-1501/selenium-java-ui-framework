# Architecture

## Source Layout

```
src/
├── main/java/com/atms/          ← Framework layer (reusable, test-agnostic)
│   ├── base/
│   │   ├── BasePage.java        ← Shared page utilities (launch, etc.)
│   │   └── BaseTest.java        ← TestNG lifecycle + parallel mode detection
│   ├── config/
│   │   └── ConfigManager.java   ← Unified property reader (static init)
│   ├── driver/
│   │   └── DriverManager.java   ← ThreadLocal<WebDriver> factory
│   ├── elements/                ← Locator constants, one file per page
│   ├── pages/                   ← Page Objects, one file per page
│   ├── reporting/
│   │   ├── ReportManager.java   ← ExtentReports singleton (synchronized)
│   │   └── TestListener.java    ← ITestListener → builds class-grouped report
│   └── utils/
│       ├── action/
│       │   └── ActionEngine.java    ← All WebDriver interactions + retry
│       ├── assertion/
│       │   └── AssertEngine.java    ← BigDecimal numeric assertion
│       └── waits/
│           └── WaitUtils.java       ← Explicit wait wrappers
└── test/java/com/atms/
    └── tests/                   ← Business-flow test classes only
        ├── SauceDemoTest.java
        └── SampleTest.java
```

---

## Why `src/main` vs `src/test`?

Placing framework code in `src/test` couples it to test compile scope — it signals to Maven that it's throwaway test code and cannot be reused across modules. The driver factory, action engine, and reporting infrastructure are reusable components, not tests. They belong in `src/main`.

Test classes (`SauceDemoTest`, `SampleTest`) stay in `src/test` because they *are* the tests — they consume the framework, they're not part of it.

---

## Layer Responsibilities

| Layer | Class | Responsibility |
|-------|-------|----------------|
| Config | `ConfigManager` | Single source for all property reads. Fails fast on missing files. |
| Driver | `DriverManager` | ThreadLocal WebDriver. One instance per thread, zero cross-thread leakage. |
| Base | `BaseTest` | TestNG lifecycle hooks. Detects parallel mode at runtime and routes accordingly. |
| Base | `BasePage` | Shared page-level utilities available to all Page Objects. |
| Elements | `*Elements` | Locator constants only — no logic, no imports, no WebDriver. |
| Pages | `*Page` | Business actions composed from `ActionEngine` calls. Pages never touch `WebDriver` directly. |
| Action | `ActionEngine` | The only place `driver.findElement()` is called. Handles waits + retry. |
| Assertion | `AssertEngine` | Custom assertions for cases where TestNG's built-ins produce false failures. |
| Reporting | `TestListener` + `ReportManager` | Wires TestNG events to Extent report nodes. Thread-safe. |

---

## Dependency Flow

```
Tests
  └── Pages
        └── ActionEngine / AssertEngine
              └── DriverManager  ←── BaseTest (lifecycle)
                    ↑
              WaitUtils
                    ↑
              ConfigManager  ←── BaseTest, ActionEngine
```

Nothing in `elements/` or `pages/` imports `WebDriver` directly.
Everything goes through `ActionEngine` — one choke point for all interactions.
