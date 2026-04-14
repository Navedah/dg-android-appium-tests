package com.lambdatest.dg.kotlin.pages

import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.pagefactory.AndroidFindBy
import io.appium.java_client.pagefactory.AppiumFieldDecorator
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.PageFactory
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class CartPage(private val driver: AndroidDriver) {

    private val wait = WebDriverWait(driver, Duration.ofSeconds(20))

    // Confirmed from cart screen page source dump
    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/filled_cart_layout")
    private lateinit var filledCartLayout: WebElement

    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/cart_recycler_view")
    private lateinit var rvCartItems: WebElement

    // Item-level IDs confirmed from cart page source (BOPIS = buy online, pick up in store)
    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/bopis_product_name")
    private lateinit var cartItemNames: List<WebElement>

    // Confirmed from cart page source
    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/subtotal_value")
    private lateinit var tvSubtotal: WebElement

    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/cart_checkout_button")
    private lateinit var btnProceedToCheckout: WebElement

    init {
        PageFactory.initElements(AppiumFieldDecorator(driver), this)
    }

    fun verifyHasItems() {
        wait.until(ExpectedConditions.visibilityOf(filledCartLayout))
        check(filledCartLayout.isDisplayed) { "Filled cart layout should be visible" }
        wait.until(ExpectedConditions.visibilityOf(rvCartItems))
        check(cartItemNames.isNotEmpty()) { "Cart should have at least one item" }
    }

    fun getOrderTotal(): String {
        return try {
            wait.until(ExpectedConditions.visibilityOf(tvSubtotal))
            tvSubtotal.text
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun tapProceedToCheckout() {
        wait.until(ExpectedConditions.elementToBeClickable(btnProceedToCheckout)).click()
        // Dismiss "You missed some deals" bottom sheet if it appears
        try {
            val continueBtn = org.openqa.selenium.By.id(
                "com.dollargeneral.qa2.android:id/continue_button")
            WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.elementToBeClickable(continueBtn)).click()
            println("Dismissed 'missed deals' dialog — continuing to checkout.")
        } catch (ignored: Exception) {
            // Dialog didn't appear — proceed normally
        }
    }
}
