package com.lambdatest.dg.kotlin.pages

import io.appium.java_client.android.AndroidDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.testng.Assert
import java.time.Duration

/**
 * Checkout flow for the DG delivery order.
 *
 * Screen sequence (confirmed from page source dumps):
 *   1. Delivery time screen   → select ASAP or Later, tap continue_btn
 *   2. Order review screen    → confirm items/payment, tap place_order
 *   3. Order confirmation
 */
class CheckoutPage(private val driver: AndroidDriver) {

    private val wait      = WebDriverWait(driver, Duration.ofSeconds(20))
    private val shortWait = WebDriverWait(driver, Duration.ofSeconds(5))

    companion object {
        // Delivery time screen — confirmed from dg-checkout.xml
        private val DELIVERY_ASAP_TILE  = By.id("com.dollargeneral.qa2.android:id/delivery_asap_tile")
        private val DELIVERY_LATER_TILE = By.id("com.dollargeneral.qa2.android:id/delivery_later_tile")
        private val CONTINUE_BTN        = By.id("com.dollargeneral.qa2.android:id/continue_btn")

        // "Schedule for later" — first available time slot (text like "9 AM - 10 AM")
        private val TIME_SLOT_ANY = By.xpath(
            "//*[contains(@text,'AM') or contains(@text,'PM')][not(contains(@text,'unavailable'))][1]")

        // Checkout review screen: Save button in Contact section
        // Confirmed from screenshot: full-width black button, place_order disabled until Save tapped
        private val SAVE_BTN = By.xpath(
            "//*[@text='Save' or @text='SAVE' or @text='save'" +
            " or @resource-id='com.dollargeneral.qa2.android:id/save_btn'" +
            " or @resource-id='com.dollargeneral.qa2.android:id/btn_save'" +
            " or @resource-id='com.dollargeneral.qa2.android:id/contact_save_btn'" +
            " or @resource-id='com.dollargeneral.qa2.android:id/save_contact_btn']")

        private val PLACE_ORDER_BTN = By.id("com.dollargeneral.qa2.android:id/place_order")
    }

    /**
     * Complete the delivery time selection flow:
     *   "Reserve a time" → ASAP (if available) or Later
     *   → "Schedule for later" time slots (if Later) → tap first slot → Continue
     *   → "Checkout" contact section → Save
     */
    fun selectDeliveryAsap() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(DELIVERY_LATER_TILE))

        var usedAsap = false
        try {
            val asapTiles = driver.findElements(DELIVERY_ASAP_TILE)
            if (asapTiles.isNotEmpty() && asapTiles[0].isDisplayed) {
                val asapText = asapTiles[0].text
                if (!asapText.contains("unavailable")) {
                    asapTiles[0].click()
                    println("Selected ASAP delivery time.")
                    Thread.sleep(500)
                    wait.until(ExpectedConditions.elementToBeClickable(CONTINUE_BTN)).click()
                    println("Tapped Continue (ASAP).")
                    usedAsap = true
                }
            }
        } catch (ignored: Exception) {}

        if (!usedAsap) selectDeliveryLater()

        // Tap Save on Checkout contact section to unlock Place order
        tapSaveContact()
    }

    /**
     * Tap "Later" → pick first available time slot → Continue.
     * Called by selectDeliveryAsap() when ASAP is unavailable.
     */
    fun selectDeliveryLater() {
        wait.until(ExpectedConditions.elementToBeClickable(DELIVERY_LATER_TILE)).click()
        println("Selected Later delivery time.")

        // "Schedule for later" screen — tap first available time slot
        Thread.sleep(2000)
        wait.until(ExpectedConditions.elementToBeClickable(TIME_SLOT_ANY)).click()
        println("Tapped first available time slot.")
        Thread.sleep(500)

        wait.until(ExpectedConditions.elementToBeClickable(CONTINUE_BTN)).click()
        println("Tapped Continue (Later).")
    }

    /** Tap Save on the Checkout contact section to unlock Place order. */
    fun tapSaveContact() {
        Thread.sleep(2000)
        val saveBtns = driver.findElements(SAVE_BTN)
        if (saveBtns.isNotEmpty()) {
            try { saveBtns[0].click(); println("Tapped Save (contact section).") }
            catch (ignored: Exception) {}
        } else {
            println("Save button not present — contact already saved.")
        }
        Thread.sleep(2000)
    }

    /**
     * Tap "Place order" on the order review screen.
     * Skip delivery instructions if present, then tap Place order.
     */
    fun tapPlaceOrder() {
        // Tap Skip (delivery instructions) if visible
        try {
            val skipBtn = By.id("com.dollargeneral.qa2.android:id/skip_button")
            WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.elementToBeClickable(skipBtn)).click()
            println("Tapped Skip (delivery instructions).")
            Thread.sleep(2000)
        } catch (ignored: Exception) {}

        // Tap Place order — try clickable first, fall back to direct tap
        try {
            WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.elementToBeClickable(PLACE_ORDER_BTN)).click()
            println("Tapped Place order (clickable).")
        } catch (e: Exception) {
            val btn = WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(PLACE_ORDER_BTN))
            btn.click()
            println("Tapped Place order (direct).")
        }
    }

    /** Verify confirmation screen and return identifying text. */
    fun verifyOrderConfirmationAndGetOrderNumber(): String {
        Thread.sleep(3000)
        val src = driver.pageSource
        println("\n── Order confirmation page source dump ───────────────────────")
        src.split("\n").forEach { line ->
            if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                val id = line.replace(Regex(".*resource-id=\"([^\"]+)\".*"), "$1").trim()
                if (id != line.trim()) println("  $id")
            }
        }
        println("──────────────────────────────────────────────────────────────")

        val confirmXpath = By.xpath(
            "//*[contains(@text,'order') or contains(@text,'Order') or contains(@text,'confirmation') or contains(@text,'placed')]")
        val confirmElems = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(confirmXpath))
        Assert.assertFalse(confirmElems.isEmpty(), "Order confirmation text should appear")
        val confirmText = confirmElems[0].text
        println("Order confirmation text: $confirmText")
        return confirmText
    }

    // ── Legacy store-pickup methods (no-ops — delivery flow only) ──────────────

    @Deprecated("DG app uses delivery flow; use selectDeliveryAsap() instead")
    fun selectStorePickup() = selectDeliveryAsap()

    @Deprecated("Not applicable in delivery flow")
    fun findStoresByZip(zipCode: String) =
        println("findStoresByZip: skipped (delivery flow — no store picker).")

    @Deprecated("Not applicable in delivery flow")
    fun selectFirstStore() =
        println("selectFirstStore: skipped (delivery flow — no store picker).")

    @Deprecated("Not needed in delivery flow")
    fun tapContinue() =
        println("tapContinue: skipped (delivery flow — continue already handled).")
}
