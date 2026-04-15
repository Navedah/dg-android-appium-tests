package com.lambdatest.dg.kotlin.pages

import io.appium.java_client.android.AndroidDriver
import org.openqa.selenium.By
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class SearchPage(private val driver: AndroidDriver) {

    private val wait = WebDriverWait(driver, Duration.ofSeconds(30))

    companion object {
        private val SEARCH_INPUT   = By.id("com.dollargeneral.qa2.android:id/search_src_text")
        private val SEARCH_RESULTS = By.id("com.dollargeneral.qa2.android:id/product_search_view")
        private val PRODUCT_NAMES  = By.id("com.dollargeneral.qa2.android:id/shopping_product_description")
    }

    fun searchFor(keyword: String) {
        val input = wait.until(ExpectedConditions.elementToBeClickable(SEARCH_INPUT))
        input.click()
        input.clear()
        input.sendKeys(keyword)
        driver.executeScript("mobile: performEditorAction", mapOf("action" to "search"))
        println("Searched for: $keyword")
    }

    fun verifyResultsLoaded() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(SEARCH_RESULTS))
        } catch (ignored: Exception) {}
        val products = driver.findElements(PRODUCT_NAMES)
        check(products.isNotEmpty()) { "Search results should not be empty" }
        println("Search results loaded: ${products.size} products.")
    }

    fun tapFirstProduct() {
        // Re-fetch fresh elements just before clicking to avoid StaleElementReferenceException
        for (attempt in 0 until 3) {
            try {
                Thread.sleep(1000)
                val products = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(PRODUCT_NAMES))
                products[0].click()
                println("Tapped first product.")
                return
            } catch (e: StaleElementReferenceException) {
                println("Stale element on attempt $attempt — retrying...")
            }
        }
    }
}
