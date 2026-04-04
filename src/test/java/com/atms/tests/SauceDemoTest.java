package com.atms.tests;

import com.atms.base.BasePage;
import com.atms.base.BaseTest;
import com.atms.config.ConfigManager;
import com.atms.pages.*;
import com.atms.reporting.TestListener;
import com.atms.utils.assertion.AssertEngine;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(TestListener.class)
public class SauceDemoTest extends BaseTest {

    private final String username  = ConfigManager.getTestData("valid.username");
    private final String password  = ConfigManager.getTestData("valid.password");
    private final String firstname = ConfigManager.getTestData("firstname");
    private final String lastname  = ConfigManager.getTestData("lastname");
    private final String postcode  = ConfigManager.getTestData("postcode");

    private final BasePage              basePage     = new BasePage();
    private final LoginPage             loginPage    = new LoginPage();
    private final InventoryPage         inventoryPage = new InventoryPage();
    private final CartPage              cartPage      = new CartPage();
    private final CheckoutPage          checkoutPage  = new CheckoutPage();
    private final CheckoutOverviewPage  overviewPage  = new CheckoutOverviewPage();
    private final CheckoutCompletePage  completePage  = new CheckoutCompletePage();

    @Test
    public void login() {
        basePage.launchApplication();
        loginPage.login(username, password);
    }

    @Test
    public void cartFlow() {
        basePage.launchApplication();
        loginPage.login(username, password);

        inventoryPage.addBackpackToCart();

        String cartCount = inventoryPage.getCartCount();
        AssertEngine.assertDoubleEquals(cartCount, 1.0);

        cartPage.clickRemoveButton();
    }

    @Test
    public void verifyCheckoutPriceAssert() {
        basePage.launchApplication();
        loginPage.login(username, password);
        inventoryPage.addBackpackToCart();
        inventoryPage.openCart();

        Assert.assertTrue(cartPage.isRemoveButtonPresent(), "Remove button not present in cart!");

        cartPage.clickCheckout();
        checkoutPage.enterDetails(firstname, lastname, postcode);
        checkoutPage.clickContinue();

        String price = overviewPage.getItemPrice();
        AssertEngine.assertDoubleEquals(price.replace("$", ""), 28.99);

        overviewPage.clickFinish();

        String message = completePage.getConfirmationMessage();
        Assert.assertTrue(message.contains("Thank you"), "Order completion message not displayed!");
    }

    @Test
    public void verifyCheckoutFlow() {
        basePage.launchApplication();
        loginPage.login(username, password);
        inventoryPage.addBackpackToCart();
        inventoryPage.openCart();

        Assert.assertTrue(cartPage.isRemoveButtonPresent(), "Remove button not present in cart!");

        cartPage.clickCheckout();
        checkoutPage.enterDetails(firstname, lastname, postcode);
        checkoutPage.clickContinue();

        String price = overviewPage.getItemPrice();
        AssertEngine.assertDoubleEquals(price.replace("$", ""), 29.99);

        overviewPage.clickFinish();

        String message = completePage.getConfirmationMessage();
        Assert.assertTrue(message.contains("Thank you"), "Order completion message not displayed!");
    }
}
