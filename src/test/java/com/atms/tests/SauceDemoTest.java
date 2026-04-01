package com.atms.tests;

import com.atms.base.BaseTest;
import com.atms.driver.DriverManager;
import com.atms.pages.CartPage;
import com.atms.pages.CheckoutCompletePage;
import com.atms.pages.CheckoutOverviewPage;
import com.atms.pages.CheckoutPage;
import com.atms.pages.InventoryPage;
import com.atms.pages.LoginPage;
import com.atms.base.BasePage;

import org.testng.annotations.Test;
import com.atms.config.ConfigManager;

import java.io.File;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import com.atms.reporting.TestListener;
import com.atms.utils.assertion.AssertEngine;

@Listeners(TestListener.class)

public class SauceDemoTest extends BaseTest {
	
String url = ConfigManager.getExecution("base.url");
String username = ConfigManager.getTestData("valid.username");
String password = ConfigManager.getTestData("valid.password");
String firstname = ConfigManager.getTestData("firstname");
String lastname = ConfigManager.getTestData("lastname");
String postcode = ConfigManager.getTestData("postcode");

LoginPage loginPage = new LoginPage();
BasePage basePage = new BasePage();
InventoryPage inventoryPage = new InventoryPage();
CartPage cartPage = new CartPage();
CheckoutPage checkoutPage = new CheckoutPage();
CheckoutOverviewPage overviewPage = new CheckoutOverviewPage();
CheckoutCompletePage completePage = new CheckoutCompletePage();




    @Test
    public void Login() {
    	basePage.launchApplication();
        loginPage.login(username, password);
    }
    
    @Test
    public void Cartflow() {
    	basePage.launchApplication();
        loginPage.login(username, password);
        
        inventoryPage.addBackpackToCart();

        String cartCount = inventoryPage.getCartCount();
        AssertEngine.assertDoubleEquals(cartCount, 1.0);
        cartPage.clickRemovebutton();
        
        
    }
    
    @Test
    public void verifyCheckoutPriceAssert() {
    	
    	basePage.launchApplication();
        loginPage.login(username, password);
        inventoryPage.addBackpackToCart();
        inventoryPage.openCart();
        Assert.assertTrue(cartPage.isRemoveButtonPresent(), "Remove button not present!");
        cartPage.clickCheckout();
        checkoutPage.enterDetails(firstname, lastname, postcode);
        checkoutPage.clickContinue();
        String price = overviewPage.getItemPrice();
        AssertEngine.assertDoubleEquals(price.replace("$", ""), 28.99);
        overviewPage.clickFinish();
        String message = completePage.getConfirmationMessage();
        Assert.assertTrue(message.contains("Thank you"),  "Order completion message not displayed!");
    }
    
    
    @Test
    public void verifyCheckoutFlow() {
    	
    	basePage.launchApplication();
        loginPage.login(username, password);
        inventoryPage.addBackpackToCart();
        inventoryPage.openCart();
        Assert.assertTrue(cartPage.isRemoveButtonPresent(), "Remove button not present!");
        cartPage.clickCheckout();
        checkoutPage.enterDetails(firstname, lastname, postcode);
        checkoutPage.clickContinue();
        String price = overviewPage.getItemPrice();
        AssertEngine.assertDoubleEquals(price.replace("$", ""), 29.99);
        overviewPage.clickFinish();
        String message = completePage.getConfirmationMessage();
        Assert.assertTrue(message.contains("Thank you"),  "Order completion message not displayed!");
    }
    
    
   
}