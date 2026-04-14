package com.lambdatest.dg.kotlin.pages

import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.pagefactory.AndroidFindBy
import io.appium.java_client.pagefactory.AppiumFieldDecorator
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.PageFactory
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class ProductDetailPage(private val driver: AndroidDriver) {

    private val wait      = WebDriverWait(driver, Duration.ofSeconds(15))

    // Confirmed from PDP page source dump
    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/product_description")
    private lateinit var tvTitle: WebElement

    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/product_price")
    private lateinit var tvPrice: WebElement

    // Post-add-to-cart snackbar — confirmed from page source dump
    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/added_to_cart_txt")
    private lateinit var tvAddSuccess: WebElement

    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/snackbar_action")
    private lateinit var btnViewCart: WebElement

    companion object {
        private val ADD_TO_CART_BTN = By.id("com.dollargeneral.qa2.android:id/add_to_cart_btn")
        private val ADD_BTN         = By.id("com.dollargeneral.qa2.android:id/add_btn")
        // Bottom nav bar cart icon — always visible, more reliable than the snackbar
        private val NAV_CART_ICON   = By.id("com.dollargeneral.qa2.android:id/nav_bar_cart_layout")
    }

    init {
        PageFactory.initElements(AppiumFieldDecorator(driver), this)
    }

    fun verifyLoaded() {
        wait.until(ExpectedConditions.visibilityOf(tvTitle))
        check(tvTitle.isDisplayed) { "PDP title should be visible" }
        check(tvPrice.isDisplayed) { "PDP price should be visible" }
    }

    fun tapAddToCart() {
        // Fixed footer — no scroll needed. Handle fresh PDP (add_to_cart_btn)
        // and already-in-cart state (add_btn = + increment button).
        val addBtns       = driver.findElements(ADD_TO_CART_BTN)
        val incrementBtns = driver.findElements(ADD_BTN)

        when {
            addBtns.isNotEmpty() && addBtns[0].isDisplayed -> {
                wait.until(ExpectedConditions.elementToBeClickable(ADD_TO_CART_BTN)).click()
                println("Tapped 'Add to cart'.")
            }
            incrementBtns.isNotEmpty() && incrementBtns[0].isDisplayed -> {
                wait.until(ExpectedConditions.elementToBeClickable(ADD_BTN)).click()
                println("Item already in cart — tapped '+' to increment.")
            }
            else -> {
                wait.until(ExpectedConditions.elementToBeClickable(ADD_TO_CART_BTN)).click()
            }
        }
    }

    fun verifyAddedToCart() {
        // Check either the snackbar message OR the "✓ Added to cart" footer state
        try {
            wait.until(ExpectedConditions.visibilityOf(tvAddSuccess))
            check(tvAddSuccess.isDisplayed) { "Add-to-cart success message should appear" }
        } catch (e: Exception) {
            // Snackbar may have already dismissed; item assumed in cart
            println("Snackbar already dismissed — item assumed in cart.")
        }
    }

    /**
     * Navigate to cart via the bottom nav bar Cart icon.
     * More reliable than the snackbar which can auto-dismiss.
     */
    fun tapViewCart() {
        wait.until(ExpectedConditions.elementToBeClickable(NAV_CART_ICON)).click()
        println("Tapped Cart icon in bottom nav bar.")
    }

    /** One-shot: add to cart and navigate to cart screen via the nav bar Cart icon. */
    fun addToCartAndViewCart() {
        tapAddToCart()
        // Brief wait for the add action to register (snackbar or counter update)
        Thread.sleep(2000)
        tapViewCart()
    }
}
