# Selenium Java UI Automation Framework

============================================================
> Enterprise-grade UI Automation Framework
> Built with Selenium | Java | TestNG
============================================================

## Overview

This framework is designed to solve real-world automation challenges:

- Flaky tests due to synchronization issues  
- Parallel execution conflicts  
- Poor reporting visibility  
- Hardcoded configurations  

Provides a scalable, maintainable, and production-ready solution.

------------------------------------------------------------

## Tech Stack

- Java 17+
- Selenium WebDriver
- TestNG
- WebDriverManager
- Extent Reports
- Maven

------------------------------------------------------------

## Key Engineering Highlights

- ThreadLocal WebDriver (Thread-safe execution)
- Dual Execution Modes (methods & classes)
- Config-driven framework (no hardcoding)
- Retry mechanism (property controlled)
- Centralized Action Engine
- Custom Assertion Layer
- Screenshot capture on failure
- Extent Report integration

------------------------------------------------------------

## Framework Architecture

src/test/java/com/atms

|-- base            -> BaseTest (lifecycle)
|-- config          -> ConfigManager
|-- driver          -> DriverManager (ThreadLocal)
|-- elements        -> Locators
|-- pages           -> Page Objects
|-- reporting       -> Reports + Listener
|-- tests           -> Test classes
|-- utils
    |-- action      -> ActionEngine
    |-- assertion   -> AssertEngine
    |-- waits       -> WaitUtils

------------------------------------------------------------

## Parallel Execution Strategy

[ METHOD LEVEL ]
- Each test runs in separate browser
- Uses ThreadLocal WebDriver
- Fully isolated execution

parallel="methods"

[ CLASS LEVEL ]
- One browser per test class
- Shared session within class

parallel="classes"

------------------------------------------------------------

## How to Run

[ Using TestNG ]

Method-level execution:
- Run testng-methods.xml

Class-level execution:
- Run testng-classes.xml

[ Using Maven ]

mvn clean test

------------------------------------------------------------

## Environment Configuration

Environment/

|-- execution.properties   -> base URL, retry count
|-- testdata.properties    -> test data

------------------------------------------------------------

## Sample Test Scenario

Checkout Flow:

1. Login
2. Add product to cart
3. Open cart
4. Validate cart
5. Proceed to checkout
6. Validate price
7. Complete order
8. Verify confirmation

------------------------------------------------------------

## Reporting

Generated at:

/reports/ExtentReport.html

Includes:
- Test results
- Logs
- Screenshots on failure

------------------------------------------------------------

## Design Decisions

ThreadLocal WebDriver:
Ensures thread-safe execution in parallel runs.

ActionEngine:
Centralizes reusable UI actions.

Config-driven approach:
Supports environment switching without code changes.

------------------------------------------------------------

## Future Enhancements

- CI/CD integration
- Cross-browser execution
- Docker support
- API + UI integration

------------------------------------------------------------

Author: Karthick S

============================================================
