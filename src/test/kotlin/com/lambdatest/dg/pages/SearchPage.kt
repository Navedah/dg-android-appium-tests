package com.lambdatest.dg.kotlin.pages

import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.pagefactory.AndroidFindBy
import io.appium.java_client.pagefactory.AppiumFieldDecorator
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.PageFactory
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class SearchPage(private val driver: AndroidDriver) {

    private val wait = WebDriverWait(driver, Duration.ofSeconds(15))

    // Confirmed from search screen + results page source dumps
    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/search_src_text")
    private lateinit var etSearchInput: WebElement

    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/product_search_view")
    private lateinit var rvSearchResults: WebElement

    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/shopping_product_description")
    private lateinit var productNames: List<WebElement>

    init {
        PageFactory.initElements(AppiumFieldDecorator(driver), this)
    }

    fun searchFor(keyword: String) {
        wait.until(ExpectedConditions.visibilityOf(etSearchInput))
        etSearchInput.click()
        etSearchInput.clear()
        etSearchInput.sendKeys(keyword)
        driver.executeScript("mobile: performEditorAction", mapOf("action" to "search"))
        println("Searched for: $keyword")
    }

    fun verifyResultsLoaded() {
        try {
            wait.until(ExpectedConditions.visibilityOf(rvSearchResults))
        } catch (e: Exception) {
            // fall through to product name check
        }
        check(productNames.isNotEmpty()) { "Search results should not be empty" }
    }

    fun tapFirstProduct() {
        wait.until(ExpectedConditions.visibilityOfAllElements(productNames))
        productNames[0].click()
    }
}
