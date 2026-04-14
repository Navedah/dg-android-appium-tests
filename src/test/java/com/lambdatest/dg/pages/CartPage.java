package com.lambdatest.dg.pages;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.List;

public class CartPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    // Confirmed from cart screen page source dump
    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/filled_cart_layout")
    private WebElement filledCartLayout;

    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/cart_recycler_view")
    private WebElement rvCartItems;

    // Item-level IDs confirmed from cart page source (BOPIS = buy online, pick up in store)
    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/bopis_product_name")
    private List<WebElement> cartItemNames;

    // Order total — confirmed from cart page source
    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/subtotal_value")
    private WebElement tvSubtotal;

    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/cart_checkout_button")
    private WebElement btnProceedToCheckout;

    public CartPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(20));
        PageFactory.initElements(new AppiumFieldDecorator(driver), this);
    }

    public void verifyHasItems() {
        wait.until(ExpectedConditions.visibilityOf(filledCartLayout));
        Assert.assertTrue(filledCartLayout.isDisplayed(), "Filled cart layout should be visible");
        wait.until(ExpectedConditions.visibilityOf(rvCartItems));
        Assert.assertFalse(cartItemNames.isEmpty(), "Cart should have at least one item");
    }

    public String getOrderTotal() {
        try {
            wait.until(ExpectedConditions.visibilityOf(tvSubtotal));
            return tvSubtotal.getText();
        } catch (Exception e) {
            return "N/A";
        }
    }

    public void tapProceedToCheckout() {
        wait.until(ExpectedConditions.elementToBeClickable(btnProceedToCheckout)).click();
        // Dismiss "You missed some deals" bottom sheet if it appears
        try {
            org.openqa.selenium.By continueBtn = org.openqa.selenium.By.id(
                "com.dollargeneral.qa2.android:id/continue_button");
            new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.elementToBeClickable(continueBtn)).click();
            System.out.println("Dismissed 'missed deals' dialog — continuing to checkout.");
        } catch (Exception ignored) {
            // Dialog didn't appear — proceed normally
        }
    }
}
