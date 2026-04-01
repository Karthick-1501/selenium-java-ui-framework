package com.atms.utils.assertion;

import org.testng.Assert;

import java.math.BigDecimal;

/**
 * Custom assertion utilities to handle UI formatting differences.
 */
public class AssertEngine {

    /**
     * Compares numeric values safely by normalizing precision.
     */
    public static void assertDoubleEquals(String actualUI, double expectedValue) {

        try {
            BigDecimal actual = new BigDecimal(actualUI.trim());
            BigDecimal expected = BigDecimal.valueOf(expectedValue);

            if (actual.compareTo(expected) != 0) {
                Assert.fail("Assertion Failed! Expected: " + expected + " but found: " + actual);
            }

        } catch (Exception e) {
            Assert.fail("Invalid number format in UI: " + actualUI);
        }
    }
}