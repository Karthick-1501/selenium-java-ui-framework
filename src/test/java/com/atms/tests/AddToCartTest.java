package com.atms.tests;

import com.atms.base.BasePage;
import com.atms.base.BaseTest;
import com.atms.config.ConfigManager;
import com.atms.pages.*;
import com.atms.reporting.TestListener;
import com.atms.utils.assertion.AssertEngine;

import java.math.BigDecimal;
import java.util.*;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(TestListener.class)
public class AddToCartTest extends BaseTest {

    private final String username  = ConfigManager.getTestData("valid.username");
    private final String password  = ConfigManager.getTestData("valid.password");
    private final String firstname = ConfigManager.getTestData("firstname");
    private final String lastname  = ConfigManager.getTestData("lastname");
    private final String postcode  = ConfigManager.getTestData("postcode");

    private final String item1 = ConfigManager.getTestData("item1");
    private final String item2 = ConfigManager.getTestData("item2");
    private final String item3 = ConfigManager.getTestData("item3");
    private final String item4 = ConfigManager.getTestData("item4");
    private final String item5 = ConfigManager.getTestData("item5");
    private final String item6 = ConfigManager.getTestData("item6");

    private final BasePage             basePage      = new BasePage();
    private final LoginPage            loginPage     = new LoginPage();
    private final InventoryPage        inventoryPage = new InventoryPage();
    private final CartPage             cartPage      = new CartPage();
    private final CheckoutPage         checkoutPage  = new CheckoutPage();
    private final CheckoutOverviewPage overviewPage  = new CheckoutOverviewPage();
    private final CheckoutCompletePage completePage  = new CheckoutCompletePage();
    private final TaxPage              taxPage       = new TaxPage();



    @Test
    public void subtotalValidation() {
        basePage.launchApplication();
        loginPage.login(username, password);

        inventoryPage.addItem(item1);
        inventoryPage.addItem(item2);
        inventoryPage.addItem(item3);
        inventoryPage.addItem(item4);
        inventoryPage.addItem(item5);
        inventoryPage.addItem(item6);

        List<BigDecimal> prices = inventoryPage.getAllItemPrices();
        BigDecimal calculatedTotal = inventoryPage.sumPrices(prices);

        inventoryPage.openCart();
        cartPage.clickCheckout();
        checkoutPage.enterDetails(firstname, lastname, postcode);
        checkoutPage.clickContinue();

        BigDecimal uiSubtotal = overviewPage.getUISubtotal();
        AssertEngine.assertBigDecimalEquals(uiSubtotal, calculatedTotal, "Subtotal mismatch");
        
        overviewPage.clickFinish();

        String message = completePage.getConfirmationMessage();
        Assert.assertTrue(message.contains("Thank you"), "Order completion message not displayed");
    }

    @Test
    public void validateTax() {
        basePage.launchApplication();
        loginPage.login(username, password);

        inventoryPage.addItem(item1);

        inventoryPage.openCart();
        cartPage.clickCheckout();
        checkoutPage.enterDetails(firstname, lastname, postcode);
        checkoutPage.clickContinue();

        BigDecimal subtotal     = overviewPage.getUISubtotal();
        double     taxPercent   = taxPage.fetchTaxPercent(item1);
        BigDecimal expectedTax  = taxPage.calculateTax(subtotal, taxPercent);
        BigDecimal uiTax        = taxPage.getUITax();

        AssertEngine.assertBigDecimalEquals(uiTax, expectedTax, "Tax mismatch for single item");
        
        overviewPage.clickFinish();

        String message = completePage.getConfirmationMessage();
        Assert.assertTrue(message.contains("Thank you"), "Order completion message not displayed");
    }

    @Test
    public void validateTaxAllItems() {
        basePage.launchApplication();
        loginPage.login(username, password);

        inventoryPage.addItem(item1);
        inventoryPage.addItem(item2);
        inventoryPage.addItem(item3);
        inventoryPage.addItem(item4);
        inventoryPage.addItem(item5);
        inventoryPage.addItem(item6);

        inventoryPage.openCart();
        cartPage.clickCheckout();
        checkoutPage.enterDetails(firstname, lastname, postcode);
        checkoutPage.clickContinue();

        BigDecimal subtotal    = overviewPage.getUISubtotal();
        double     taxPercent  = taxPage.fetchTaxPercent(item1);
        BigDecimal expectedTax = taxPage.calculateTax(subtotal, taxPercent);
        BigDecimal uiTax       = taxPage.getUITax();

        AssertEngine.assertBigDecimalEquals(uiTax, expectedTax, "Tax mismatch for all items");
        
        overviewPage.clickFinish();

        String message = completePage.getConfirmationMessage();
        Assert.assertTrue(message.contains("Thank you"), "Order completion message not displayed");
    }
}
