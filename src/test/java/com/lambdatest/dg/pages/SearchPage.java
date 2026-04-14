package com.lambdatest.dg.pages;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.List;

public class SearchPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    // Confirmed from search screen + results page source dumps
    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/search_src_text")
    private WebElement etSearchInput;

    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/product_search_view")
    private WebElement rvSearchResults;

    // Product description (name) — confirmed from search results page source
    @AndroidFindBy(id = "com.dollargeneral.qa2.android:id/shopping_product_description")
    private List<WebElement> productNames;

    public SearchPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(15));
        PageFactory.initElements(new AppiumFieldDecorator(driver), this);
    }

    public void searchFor(String keyword) {
        wait.until(ExpectedConditions.visibilityOf(etSearchInput));
        etSearchInput.click();
        etSearchInput.clear();
        etSearchInput.sendKeys(keyword);
        // Use AndroidKey.ENTER to reliably trigger IME search action
        // Trigger the IME "Search" action (not the same as the ENTER key)
        driver.executeScript("mobile: performEditorAction", java.util.Map.of("action", "search"));
        System.out.println("Searched for: " + keyword);
    }

    public void verifyResultsLoaded() {
        // Wait for results container (ID to be confirmed; fall back to checking product names)
        try {
            wait.until(ExpectedConditions.visibilityOf(rvSearchResults));
        } catch (Exception e) {
            // If results container ID isn't right yet, fall through and check product names below
        }
        Assert.assertFalse(productNames.isEmpty(), "Search results should not be empty");
    }

    /** Tap the first product card to open its PDP. */
    public void tapFirstProduct() {
        wait.until(ExpectedConditions.visibilityOfAllElements(productNames));
        productNames.get(0).click();
    }
}
