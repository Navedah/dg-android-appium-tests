package com.lambdatest.dg.kotlin.pages

import io.appium.java_client.android.AndroidDriver
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

/**
 * Sign-In page — native screen locators confirmed from LambdaTest UI Inspector.
 * Fields use resource-id without package prefix → XPath @resource-id required.
 */
class SignInPage(private val driver: AndroidDriver) {

    private val wait = WebDriverWait(driver, Duration.ofSeconds(120))

    companion object {
        // Navigation to sign-in form (tap from home/browse screen)
        private val NAV_SIGN_IN_LINK = By.xpath(
            "//*[@text='Sign In' or @text='Sign in' or @text='SIGN IN' " +
            "or @resource-id='com.dollargeneral.qa2.android:id/nav_sign_in' " +
            "or @resource-id='com.dollargeneral.qa2.android:id/sign_in_button' " +
            "or @resource-id='com.dollargeneral.qa2.android:id/login_btn']")

        // Sign-in form fields (inside WebView — resource-id without package prefix)
        private val EMAIL_FIELD    = By.xpath("//*[@resource-id='user-email']")
        private val PASSWORD_FIELD = By.xpath("//*[@resource-id='user-password']")
        // Submit button — native or WebView
        private val SUBMIT_BTN = By.xpath(
            "//*[@resource-id='signInButton' or @resource-id='sign-in-button' " +
            "or @resource-id='com.dollargeneral.qa2.android:id/btn_sign_in' " +
            "or (@text='Sign in' and not(@resource-id='user-email'))]")
        private val ERROR_MSG = By.xpath(
            "//*[contains(@text,'incorrect') or contains(@text,'Invalid') or contains(@text,'wrong')]")
    }

    /**
     * Navigate from home screen to the sign-in form.
     * Dumps page source if sign-in form is not immediately visible, to aid debugging.
     */
    fun navigateToSignIn() {
        // If the email field is already visible, we're already on sign-in — skip navigation
        try {
            WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD))
            println("Sign-in form already visible — skipping navigation.")
            return
        } catch (ignored: Exception) {}

        // Dump page source to find the navigation element
        val src = driver.pageSource
        println("── navigateToSignIn page source ──")
        src.split("\n").forEach { line ->
            if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                val id = line.replace(Regex(".*resource-id=\"([^\"]+)\".*"), "$1").trim()
                if (id != line.trim()) println("  $id")
            }
        }
        println("──────────────────────────────────")

        // Tap the sign-in navigation link
        try {
            WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.elementToBeClickable(NAV_SIGN_IN_LINK)).click()
            println("Tapped Sign In navigation link.")
        } catch (e: Exception) {
            println("Sign-in nav link not found: ${e.message}")
        }
    }

    fun enterEmail(email: String) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD)).apply {
            clear()
            sendKeys(email)
        }
        println("Entered email: $email")
    }

    fun enterPassword(password: String) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD)).apply {
            clear()
            sendKeys(password)
        }
        println("Entered password.")
    }

    fun tapSignIn() {
        // Try native submit button first, then WebView button
        try {
            WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(SUBMIT_BTN)).click()
        } catch (ignored: Exception) {
            // Fall back: any visible "Sign in" / "SIGN IN" button
            val fallback = By.xpath("//*[@text='Sign in' or @text='SIGN IN' or @text='Sign In']")
            wait.until(ExpectedConditions.elementToBeClickable(fallback)).click()
        }
        println("Tapped Sign In.")
        // Wait for auth + home screen load (network round-trip + promo dismissal)
        Thread.sleep(15000)
    }

    fun loginWith(email: String, password: String) {
        navigateToSignIn()
        enterEmail(email)
        enterPassword(password)
        tapSignIn()
    }

    fun verifyLoginError() {
        val err = wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_MSG))
        check(err.isDisplayed) { "Login error message should be visible" }
    }
}
