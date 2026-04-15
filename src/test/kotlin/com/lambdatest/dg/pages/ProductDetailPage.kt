package com.lambdatest.dg.kotlin.pages

import io.appium.java_client.android.AndroidDriver
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class ProductDetailPage(private val driver: AndroidDriver) {

    private val wait = WebDriverWait(driver, Duration.ofSeconds(15))

    companion object {
        private val TV_TITLE        = By.id("com.dollargeneral.qa2.android:id/product_description")
        private val TV_PRICE        = By.id("com.dollargeneral.qa2.android:id/product_price")
        private val ADD_TO_CART_BTN = By.id("com.dollargeneral.qa2.android:id/add_to_cart_btn")
        private val ADD_BTN         = By.id("com.dollargeneral.qa2.android:id/add_btn")
        private val NAV_CART_ICON   = By.id("com.dollargeneral.qa2.android:id/nav_bar_cart_layout")
    }

    fun verifyLoaded() {
        val title = wait.until(ExpectedConditions.visibilityOfElementLocated(TV_TITLE))
        check(title.isDisplayed) { "PDP title should be visible" }
    }

    fun tapAddToCart() {
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

    fun tapViewCart() {
        wait.until(ExpectedConditions.elementToBeClickable(NAV_CART_ICON)).click()
        println("Tapped Cart icon in bottom nav bar.")
    }

    fun addToCartAndViewCart() {
        tapAddToCart()
        Thread.sleep(2000)
        tapViewCart()
    }
}
