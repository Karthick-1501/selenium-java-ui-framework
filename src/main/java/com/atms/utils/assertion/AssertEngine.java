package com.atms.utils.assertion;

import org.testng.Assert;


import java.math.BigDecimal;


/**
 * Custom assertion utilities to handle UI number formatting differences.
 *
 * Problem solved: TestNG's assertEquals treats "1.00" != "1.0" as a string.
 * This engine normalizes both sides via BigDecimal.compareTo() so precision
 * differences in the UI don't cause false failures.
 */
public class AssertEngine {

    /**
     * Asserts that a UI text value is numerically equal to the expected double.
     * Strips trailing zeros before comparison — "28.990" == 28.99 passes cleanly.
     *
     * @param actualUI    raw text from the UI (e.g., "28.99" or "1.00")
     * @param expected    expected numeric value
     */
    public static void assertDoubleEquals(String actualUI, double expected) {
        try {
            BigDecimal actual      = new BigDecimal(actualUI.trim());
            BigDecimal expectedBD  = BigDecimal.valueOf(expected);

            if (actual.compareTo(expectedBD) != 0) {
                Assert.fail("Numeric assertion failed — Expected: " + expectedBD + " | Actual: " + actual);
            }
        } catch (NumberFormatException e) {
            Assert.fail("Invalid numeric format in UI text: '" + actualUI + "'");
        }
    }
    
    
   
    
}
