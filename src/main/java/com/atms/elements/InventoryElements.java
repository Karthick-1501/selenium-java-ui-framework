package com.atms.elements;

import org.openqa.selenium.By;

public class InventoryElements {
	
    public static final String ADD_TO_CART_BACKPACK = "id=add-to-cart-sauce-labs-backpack";
    public static final String ADD_TO_CART_BIKE = "id=add-to-cart-sauce-labs-bike-light";
    public static final String ADD_TO_CART_TSHIRT = "id=add-to-cart-sauce-labs-bolt-t-shirt";
	public static final String ADD_TO_CART_JACKET = "id=add-to-cart-sauce-labs-fleece-jacket";
	public static final String ADD_TO_CART_ONESIE = "id=add-to-cart-sauce-labs-onesie";
	public static final String ADD_TO_CART_TESTALL ="id=add-to-cart-test.allthethings()-t-shirt-(red)";
	
	public static By PRICE_LIST = By.cssSelector("[data-test='inventory-item-price']");
	
	
    
    public static final String CART_ICON            = "css=[data-test='shopping-cart-link']";
    public static final String CART_BADGE           = "css=.shopping_cart_badge";
}
