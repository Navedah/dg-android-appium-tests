package com.lambdatest.dg.kotlin.pages

import io.appium.java_client.android.AndroidDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class HomePage(private val driver: AndroidDriver) {

    private val wait      = WebDriverWait(driver, Duration.ofSeconds(90))
    private val shortWait = WebDriverWait(driver, Duration.ofSeconds(5))

    companion object {
        // Confirmed from page source dump
        private val BTN_DISMISS    = By.id("com.dollargeneral.qa2.android:id/dismiss")
        private val BTN_START_SHOP = By.id("com.dollargeneral.qa2.android:id/start_shopping")

        // Confirmed from home screen page source
        private val SEARCH_BAR = By.id("com.dollargeneral.qa2.android:id/home_search_view")
        private val CART_ICON  = By.id("com.dollargeneral.qa2.android:id/nav_bar_cart_layout")
        private val CART_BADGE = By.id("com.dollargeneral.qa2.android:id/tv_cart_count")
    }

    /** Dismiss the "SAME DAY DELIVERY IS HERE!" promo if present. */
    fun dismissPromoIfPresent() {
        try {
            val btns = shortWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(BTN_DISMISS))
            if (btns.isNotEmpty() && btns[0].isDisplayed) {
                btns[0].click()
                println("Dismissed promo screen.")
            }
        } catch (e: Exception) {
            println("No promo screen to dismiss.")
        }
    }

    fun verifyLoaded() {
        // Dismiss any interstitial screens that appear after login/fresh install
        // Loop up to 8 times to handle stacked overlays
        for (attempt in 0 until 8) {
            Thread.sleep(3000)
            val src = driver.pageSource

            // Already on home screen — done
            if (src.contains("id/home_search_view")) {
                println("Home screen loaded (attempt $attempt).")
                return
            }

            // Dump resource-ids on every attempt for diagnostics
            println("── verifyLoaded page source (attempt $attempt) ──")
            src.split("\n").forEach { line ->
                if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                    val id = line.replace(Regex(".*resource-id=\"([^\"]+)\".*"), "$1").trim()
                    if (id != line.trim()) println("  $id")
                }
            }
            println("──────────────────────────────────────────")

            // Handle: email verification screen ("Remind me later" to skip)
            if (src.contains("id/remind_me_later_tv") || src.contains("id/submit_verification_email_btn")) {
                val remindLater = By.id("com.dollargeneral.qa2.android:id/remind_me_later_tv")
                try {
                    driver.findElement(remindLater).click()
                    println("Dismissed email verification (Remind me later).")
                    Thread.sleep(2000)
                    continue
                } catch (ignored: Exception) {}
            }

            // Handle: address selection dialog (appears after tapping Start Shopping)
            // button1 = positive/OK in Android AlertDialog (confirm address)
            if (src.contains("id/address_list") || src.contains("id/address_radio_button")) {
                val btn1 = By.id("android:id/button1")
                try {
                    driver.findElement(btn1).click()
                    println("Confirmed delivery address (button1).")
                    Thread.sleep(2000)
                    continue
                } catch (ignored: Exception) {}
                // If button1 not tapped, try close_btn to dismiss
                val closeBtn = By.id("com.dollargeneral.qa2.android:id/close_btn")
                try {
                    driver.findElement(closeBtn).click()
                    println("Closed address dialog (close_btn).")
                    Thread.sleep(2000)
                    continue
                } catch (ignored: Exception) {}
            }

            // Handle: "Start shopping" CTA — takes us directly to home (check BEFORE dismiss)
            if (src.contains("id/start_shopping")) {
                try {
                    driver.findElement(BTN_START_SHOP).click()
                    println("Tapped Start Shopping.")
                    Thread.sleep(3000)
                    continue
                } catch (ignored: Exception) {}
            }

            // Handle: promo/interstitial dismiss button (only if no start_shopping)
            if (src.contains("id/dismiss")) {
                try {
                    driver.findElement(BTN_DISMISS).click()
                    println("Dismissed overlay (id/dismiss).")
                    continue
                } catch (ignored: Exception) {}
            }

            // Handle: WebView interstitial — press back
            if (src.contains("WebView") || (src.contains("id/webView") && !src.contains("id/home_search_view"))) {
                println("WebView interstitial detected, pressing back...")
                driver.navigate().back()
                continue
            }

            // Handle: any "Continue", "Skip", "Got it", "OK", "Allow", "Next" button
            val genericContinue = By.xpath(
                "//*[@text='Continue' or @text='CONTINUE' or @text='Skip' or @text='SKIP' " +
                "or @text='Got it' or @text='GOT IT' or @text='OK' or @text='Allow' or @text='Next']")
            try {
                val btns = driver.findElements(genericContinue)
                if (btns.isNotEmpty() && btns[0].isDisplayed) {
                    val btnText = btns[0].text
                    btns[0].click()
                    println("Tapped generic continue button: $btnText")
                    continue
                }
            } catch (ignored: Exception) {}
        }

        // Final wait for home screen search bar
        wait.until(ExpectedConditions.presenceOfElementLocated(SEARCH_BAR))
        println("Home screen loaded.")
    }

    fun tapSearch() {
        dismissPromoIfPresent()
        wait.until(ExpectedConditions.elementToBeClickable(SEARCH_BAR)).click()
    }

    fun tapCart() {
        wait.until(ExpectedConditions.elementToBeClickable(CART_ICON)).click()
    }

    fun getCartCount(): Int {
        return try {
            val badge = shortWait.until(ExpectedConditions.visibilityOfElementLocated(CART_BADGE))
            badge.text.trim().toInt()
        } catch (e: Exception) {
            0
        }
    }
}
