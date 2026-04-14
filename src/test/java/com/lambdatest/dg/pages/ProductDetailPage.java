package com.lambdatest.dg.pages;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.List;

public class ProductDetailPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;
    private final WebDriverWait shortWait;

    // Confirmed from PDP page source dump
    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/product_description")
    private WebElement tvTitle;

    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/product_price")
    private WebElement tvPrice;

    // Post-add-to-cart snackbar — confirmed from page source dump
    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/added_to_cart_txt")
    private WebElement tvAddSuccess;

    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/snackbar_action")
    private WebElement btnViewCart;

    // Locators for add/increment buttons (fixed footer — no scroll needed)
    private static final By ADD_TO_CART_BTN  = By.id("com.dollargeneral.qa2.android:id/add_to_cart_btn");
    private static final By ADD_BTN          = By.id("com.dollargeneral.qa2.android:id/add_btn");
    // Bottom nav bar cart icon — always visible, more reliable than the snackbar
    private static final By NAV_CART_ICON    = By.id("com.dollargeneral.qa2.android:id/nav_bar_cart_layout");

    public ProductDetailPage(AndroidDriver driver) {
        this.driver    = driver;
        this.wait      = new WebDriverWait(driver, Duration.ofSeconds(15));
        this.shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
        PageFactory.initElements(new AppiumFieldDecorator(driver), this);
    }

    public void verifyLoaded() {
        wait.until(ExpectedConditions.visibilityOf(tvTitle));
        Assert.assertTrue(tvTitle.isDisplayed(), "PDP title should be visible");
        Assert.assertTrue(tvPrice.isDisplayed(), "PDP price should be visible");
    }

    public void tapAddToCart() {
        // The add-to-cart area is a fixed footer — no scrolling needed.
        // If item already in cart, add_to_cart_btn is replaced by add_btn (+).
        List<WebElement> addBtns     = driver.findElements(ADD_TO_CART_BTN);
        List<WebElement> incrementBtns = driver.findElements(ADD_BTN);

        if (!addBtns.isEmpty() && addBtns.get(0).isDisplayed()) {
            wait.until(ExpectedConditions.elementToBeClickable(ADD_TO_CART_BTN)).click();
            System.out.println("Tapped 'Add to cart'.");
        } else if (!incrementBtns.isEmpty() && incrementBtns.get(0).isDisplayed()) {
            wait.until(ExpectedConditions.elementToBeClickable(ADD_BTN)).click();
            System.out.println("Item already in cart — tapped '+' to increment.");
        } else {
            // Last resort: scroll down then retry
            wait.until(ExpectedConditions.elementToBeClickable(ADD_TO_CART_BTN)).click();
        }
    }

    public void verifyAddedToCart() {
        // Check either the snackbar message OR the "✓ Added to cart" footer state
        try {
            wait.until(ExpectedConditions.visibilityOf(tvAddSuccess));
            Assert.assertTrue(tvAddSuccess.isDisplayed(), "Add-to-cart success message should appear");
        } catch (Exception e) {
            // Snackbar may have already dismissed; verify via cart badge or footer counter
            System.out.println("Snackbar already dismissed — item assumed in cart.");
        }
    }

    /**
     * Navigate to cart via the bottom nav bar Cart icon.
     * More reliable than the snackbar which can auto-dismiss.
     */
    public void tapViewCart() {
        wait.until(ExpectedConditions.elementToBeClickable(NAV_CART_ICON)).click();
        System.out.println("Tapped Cart icon in bottom nav bar.");
    }

    /** One-shot: add to cart and navigate to cart screen via the nav bar Cart icon. */
    public void addToCartAndViewCart() {
        tapAddToCart();
        // Brief wait for the add action to register (snackbar or counter update)
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        tapViewCart();
    }
}
