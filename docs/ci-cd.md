# CI/CD — GitHub Actions

`.github/workflows/ci.yml`

---

## What it does

On every push or pull request the workflow:

1. Spins up a fresh `ubuntu-latest` runner
2. Installs Java 17 (Temurin) with Maven dependency cache
3. Injects all secrets as environment variables — `ConfigManager` resolves them automatically, no `.env` file is needed
4. Runs `mvn test -DsuiteXmlFile=testng-classes.xml -Dheadless=true`
5. Uploads `reports/ExtentReport.html` and `reports/screenshots/` as a downloadable artifact — always, even when tests fail

---

## Triggers

```yaml
on:
  push:
    branches: ["**"]
  pull_request:
    branches: ["**"]
  workflow_dispatch:         # manual trigger from the Actions tab
    inputs:
      run_tests:             # true (default) or false — skip execution without removing the workflow
      parallel_mode:         # classes (default) or methods
```

| Event | Suite runs? | Parallel mode |
|-------|-------------|---------------|
| `push` | Always | `classes` |
| `pull_request` | Always | `classes` |
| Manual — `run_tests: true` | Yes | Your choice |
| Manual — `run_tests: false` | No (job skipped) | — |

---

## The `run_tests` control

The `run_tests` input is a `choice` field (`true` / `false`, default `true`). The entire `test` job has:

```yaml
if: ${{ github.event.inputs.run_tests != 'false' }}
```

When triggered by a push or PR, `github.event.inputs.run_tests` is empty (not `'false'`), so the condition passes and the suite always runs. When triggered manually with `run_tests: false`, the job is skipped cleanly — no failure, no tests executed.

---

## Headless mode

The workflow passes `-Dheadless=true` to Maven. `ConfigManager.getExecution("headless")` checks JVM system properties first, so this override takes effect without touching `execution.properties`. Locally, `execution.properties` keeps `headless=false` for headed runs.

`DriverManager` picks up the flag:

```java
String headless = ConfigManager.getExecution("headless");
if ("true".equalsIgnoreCase(headless)) {
    options.addArguments("--headless=new");
    options.addArguments("--window-size=1920,1080");
}
```

The CI-safe flags (`--no-sandbox`, `--disable-dev-shm-usage`) are always applied — they are harmless locally and required when Chrome runs inside a container.

---

## Secrets — how they flow

No `.env` file exists in CI. Instead, GitHub Actions secrets are injected as environment variables in the `Run test suite` step:

```yaml
env:
  DB_URL:       ${{ secrets.DB_URL }}
  DB_PASSWORD:  ${{ secrets.DB_PASSWORD }}
  # ... all other secrets
```

`ConfigManager` resolves `${DB_URL}` in `execution.properties` by calling `dotenv.get("DB_URL", System.getenv("DB_URL"))`. Since no `.env` file is present, `dotenv` returns `null` and the fallback `System.getenv("DB_URL")` picks up the injected secret.

**Priority order:**

```
JVM system property (-Dkey=value)   ← highest
  ↓
GitHub Actions env var / OS env
  ↓
.env file (local only)
  ↓
execution.properties / testdata.properties value   ← lowest
```

---

## Accessing the ExtentReport

After any run:

```
GitHub → Actions → select the run → Artifacts → extent-report-<run-number>
```

Download the zip, extract, and open `ExtentReport.html` in a browser. Screenshots for failed tests are embedded in the same artifact under `screenshots/`.

The artifact is retained for **30 days**. Adjust `retention-days` in the workflow if you need longer.

---

## Required GitHub Actions secrets

Configure these at **Settings → Secrets and variables → Actions → New repository secret**:

| Secret | Description |
|--------|-------------|
| `DB_URL` | Full JDBC connection string, e.g. `jdbc:postgresql://host:5432/dbname` |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `APP_USERNAME` | SauceDemo login username |
| `APP_PASSWORD` | SauceDemo login password |
| `DB_TABLE_PRODUCT` | Fully qualified product table (`schema.table`) |
| `DB_TABLE_CLASSIFICATION` | Fully qualified classification table |
| `DB_TABLE_TAX` | Fully qualified tax table |
| `DB_COL_TAX_RATE` | Tax rate column |
| `DB_COL_PRODUCT_ID` | Product ID column |
| `DB_COL_CLASSIFICATION_ITEM_ID` | Classification → item FK column |
| `DB_COL_CLASSIFICATION_CODE` | Classification code column |
| `DB_COL_TAX_CODE` | Tax code column |
| `DB_COL_PRODUCT_NAME` | Product name column |

---

## Parallel mode

| Mode | Suite file | Driver scope | When to use |
|------|-----------|--------------|-------------|
| `classes` (default) | `testng-classes.xml` | One browser per test class | Faster, lower resource usage |
| `methods` | `testng-methods.xml` | One browser per test method | Full isolation, slower |

Push and PR runs always use `classes`. Manual runs let you choose via the `parallel_mode` input.
