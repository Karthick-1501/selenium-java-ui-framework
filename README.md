# UI Automation Framework 

# Overview
Enterprise-grade UI Automation Framework built using Selenium, Java, and TestNG.

Designed to solve real-world challenges:
- Parallel execution issues
- Flaky tests
- Reporting clarity
- UI precision mismatches

# Key Engineering Highlights

- ThreadLocal WebDriver (Parallel Safe)
- Dual-mode Execution (Methods & Classes)
- Config-driven Framework (No Hardcoding)
- Retry Mechanism (Environment Controlled)
- Extent Reports (Class-grouped + Inline Screenshots)
- Custom Assertion Engine (BigDecimal precision handling)
- State Isolation (Cookie cleanup per test)

# Architecture

src/
 ├── base → lifecycle handling
 ├── driver → thread-safe driver
 ├── config → property handling
 ├── utils → reusable logic
 ├── pages → POM
 ├── elements → locators
 ├── reporting → listener + extent
 ├── tests → business flows

# Execution Modes

## Class-Level Parallel
- One browser per class
- Faster execution
- Requires state reset

## Method-Level Parallel
- One browser per test
- Fully isolated
- Slower but stable


# Problem Solving 

## 1. Parallel Execution Failure
Issue: Driver collision  
Solution: ThreadLocal WebDriver

## 2. Flaky Tests
Issue: Element not clickable  
Solution: Retry + Explicit waits

## 3. UI Precision Bug
Issue: 1.00 vs 1.0  
Solution: BigDecimal assertion engine

## 4. Reporting Chaos
Issue: Flat reports  
Solution: Class-level grouping using ConcurrentHashMap


# Reporting

- Class → Parent node
- Methods → Child nodes
- Screenshots inline
- Stack trace included

# Covered Scenarios

- Login
- Cart validation
- Checkout flow
- Price verification

# Why I did what I did

This is not demo automation.

This solves:
- Concurrency issues
- State leakage
- Reporting clarity
- Real UI validation problems

# Author
Karthick S | Java | Selenium | TestNG 
