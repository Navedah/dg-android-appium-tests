package com.lambdatest.dg.kotlin.pages

import io.appium.java_client.android.AndroidDriver
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class CartPage(private val driver: AndroidDriver) {

    private val wait = WebDriverWait(driver, Duration.ofSeconds(20))

    companion object {
        private val FILLED_CART     = By.id("com.dollargeneral.qa2.android:id/filled_cart_layout")
        private val CART_RECYCLER   = By.id("com.dollargeneral.qa2.android:id/cart_recycler_view")
        private val CART_ITEM_NAMES = By.id("com.dollargeneral.qa2.android:id/bopis_product_name")
        private val SUBTOTAL        = By.id("com.dollargeneral.qa2.android:id/subtotal_value")
        private val CHECKOUT_BTN    = By.id("com.dollargeneral.qa2.android:id/cart_checkout_button")
        private val MISSED_DEALS_OK = By.id("com.dollargeneral.qa2.android:id/continue_button")
    }

    fun verifyHasItems() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(FILLED_CART))
        wait.until(ExpectedConditions.visibilityOfElementLocated(CART_RECYCLER))
        val items = driver.findElements(CART_ITEM_NAMES)
        check(items.isNotEmpty()) { "Cart should have at least one item" }
    }

    fun getOrderTotal(): String {
        return try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(SUBTOTAL)).text
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun tapProceedToCheckout() {
        wait.until(ExpectedConditions.elementToBeClickable(CHECKOUT_BTN)).click()
        // Dismiss "You missed some deals" bottom sheet if it appears
        try {
            WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.elementToBeClickable(MISSED_DEALS_OK)).click()
            println("Dismissed 'missed deals' dialog — continuing to checkout.")
        } catch (ignored: Exception) {}
    }
}
