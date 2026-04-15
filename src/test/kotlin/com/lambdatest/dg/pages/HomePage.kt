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
        private val BTN_DISMISS    = By.id("com.dollargeneral.qa2.android:id/dismiss")
        private val BTN_START_SHOP = By.id("com.dollargeneral.qa2.android:id/start_shopping")
        private val SEARCH_BAR     = By.id("com.dollargeneral.qa2.android:id/home_search_view")
        private val CART_ICON      = By.id("com.dollargeneral.qa2.android:id/nav_bar_cart_layout")
        private val CART_BADGE     = By.id("com.dollargeneral.qa2.android:id/tv_cart_count")
    }

    /** Dismiss the promo/SDD overlay if present. */
    fun dismissPromoIfPresent() {
        val btns = driver.findElements(BTN_DISMISS)
        if (btns.isNotEmpty()) {
            try { btns[0].click(); println("Dismissed promo screen.") } catch (ignored: Exception) {}
        }
    }

    /** Click element immediately using findElements (avoids implicit wait delay). */
    private fun clickIfPresent(by: By, label: String): Boolean {
        val els = driver.findElements(by)
        if (els.isNotEmpty()) {
            try { els[0].click(); println("Tapped: $label"); return true } catch (ignored: Exception) {}
        }
        return false
    }

    fun verifyLoaded() {
        for (attempt in 0 until 10) {
            Thread.sleep(2000)
            val src = driver.pageSource

            if (src.contains("id/home_search_view")) {
                println("Home screen loaded (attempt $attempt).")
                return
            }

            println("── verifyLoaded attempt $attempt ──")
            src.split("\n").forEach { line ->
                if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                    val id = line.replace(Regex(".*resource-id=\"([^\"]+)\".*"), "$1").trim()
                    if (id != line.trim()) println("  $id")
                }
            }
            println("────────────────────────────────────")

            // Email verification — tap "Remind me later"
            if (src.contains("id/remind_me_later_tv")) {
                if (clickIfPresent(By.id("com.dollargeneral.qa2.android:id/remind_me_later_tv"), "Remind me later")) {
                    Thread.sleep(2000); continue
                }
            }

            // Address dialog after Start Shopping — tap button1 (OK/Confirm) or close_btn
            if (src.contains("id/address_list") || src.contains("id/address_radio_button")) {
                if (clickIfPresent(By.id("android:id/button1"), "address dialog OK")) {
                    Thread.sleep(2000); continue
                }
                if (clickIfPresent(By.id("com.dollargeneral.qa2.android:id/close_btn"), "address dialog close")) {
                    Thread.sleep(2000); continue
                }
            }

            // SDD "Start shopping" CTA — navigate to home
            if (src.contains("id/start_shopping")) {
                if (clickIfPresent(BTN_START_SHOP, "Start Shopping")) {
                    Thread.sleep(3000); continue
                }
            }

            // Generic dismiss button
            if (src.contains("id/dismiss")) {
                if (clickIfPresent(BTN_DISMISS, "dismiss overlay")) continue
            }

            // WebView interstitial — press back
            if (src.contains("WebView") && !src.contains("id/home_search_view")) {
                println("WebView detected, pressing back...")
                driver.navigate().back()
                continue
            }
        }

        wait.until(ExpectedConditions.presenceOfElementLocated(SEARCH_BAR))
        println("Home screen loaded.")
    }

    fun tapSearch() {
        wait.until(ExpectedConditions.elementToBeClickable(SEARCH_BAR)).click()
    }

    fun tapCart() {
        wait.until(ExpectedConditions.elementToBeClickable(CART_ICON)).click()
    }

    fun getCartCount(): Int {
        return try {
            val badge = shortWait.until(ExpectedConditions.visibilityOfElementLocated(CART_BADGE))
            badge.text.trim().toInt()
        } catch (e: Exception) { 0 }
    }
}
