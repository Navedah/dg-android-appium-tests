package com.lambdatest.dg.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;

/**
 * Sign-In page — native screen, locators confirmed from LambdaTest UI Inspector.
 *
 * XPath //*[@resource-id="user-email"] → 1/1 match (confirmed)
 */
public class SignInPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    // Navigation to sign-in form (tap from home/browse screen)
    // Confirmed: app launches to home screen; "Sign In" link appears in nav or profile tab
    private static final By NAV_SIGN_IN_LINK = By.xpath(
        "//*[@text='Sign In' or @text='Sign in' or @text='SIGN IN' " +
        "or @resource-id='com.dollargeneral.qa2.android:id/nav_sign_in' " +
        "or @resource-id='com.dollargeneral.qa2.android:id/sign_in_button' " +
        "or @resource-id='com.dollargeneral.qa2.android:id/login_btn']");

    // Sign-in form fields (inside WebView — resource-id without package prefix)
    private static final By EMAIL_FIELD    = By.xpath("//*[@resource-id='user-email']");
    private static final By PASSWORD_FIELD = By.xpath("//*[@resource-id='user-password']");
    // Submit button — native or WebView
    private static final By SUBMIT_BTN     = By.xpath(
        "//*[@resource-id='signInButton' or @resource-id='sign-in-button' " +
        "or @resource-id='com.dollargeneral.qa2.android:id/btn_sign_in' " +
        "or (@text='Sign in' and not(@resource-id='user-email'))]");
    private static final By ERROR_MSG      = By.xpath(
        "//*[contains(@text,'incorrect') or contains(@text,'Invalid') or contains(@text,'wrong')]");

    public SignInPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(120));
    }

    /**
     * Navigate from home screen to the sign-in form.
     * Dumps page source if sign-in form is not immediately visible, to aid debugging.
     */
    public void navigateToSignIn() {
        // If the email field is already visible, we're already on sign-in — skip navigation
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD));
            System.out.println("Sign-in form already visible — skipping navigation.");
            return;
        } catch (Exception ignored) {}

        // Dump page source to find the navigation element
        String src = driver.getPageSource();
        System.out.println("── navigateToSignIn page source ──");
        for (String line : src.split("\n")) {
            if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                if (!id.equals(line.trim())) System.out.println("  " + id);
            }
        }
        System.out.println("──────────────────────────────────");

        // Tap the sign-in navigation link
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.elementToBeClickable(NAV_SIGN_IN_LINK)).click();
            System.out.println("Tapped Sign In navigation link.");
        } catch (Exception e) {
            System.out.println("Sign-in nav link not found: " + e.getMessage());
        }
    }

    public void enterEmail(String email) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(EMAIL_FIELD));
        field.clear();
        field.sendKeys(email);
        System.out.println("Entered email: " + email);
    }

    public void enterPassword(String password) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD));
        field.clear();
        field.sendKeys(password);
        System.out.println("Entered password.");
    }

    public void tapSignIn() {
        // Try native submit button first, then WebView button
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(SUBMIT_BTN)).click();
        } catch (Exception ignored) {
            // Fall back: any visible "Sign in" / "SIGN IN" button
            By fallback = By.xpath("//*[@text='Sign in' or @text='SIGN IN' or @text='Sign In']");
            wait.until(ExpectedConditions.elementToBeClickable(fallback)).click();
        }
        System.out.println("Tapped Sign In.");
        // Wait for auth + home screen load (network round-trip + promo dismissal)
        try { Thread.sleep(15000); } catch (InterruptedException ignored) {}
    }

    public void loginWith(String email, String password) {
        navigateToSignIn();
        enterEmail(email);
        enterPassword(password);
        tapSignIn();
    }

    public void verifyLoginError() {
        WebElement err = wait.until(ExpectedConditions.visibilityOfElementLocated(ERROR_MSG));
        Assert.assertTrue(err.isDisplayed(), "Login error message should be visible");
    }
}
