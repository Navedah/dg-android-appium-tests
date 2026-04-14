package com.lambdatest.dg.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.List;

public class HomePage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;
    private final WebDriverWait shortWait;

    // Confirmed from page source dump
    private static final By BTN_DISMISS     = By.id("com.dollargeneral.qa2.android:id/dismiss");
    private static final By BTN_START_SHOP  = By.id("com.dollargeneral.qa2.android:id/start_shopping");

    // Confirmed from home screen page source dump
    private static final By SEARCH_BAR  = By.id("com.dollargeneral.qa2.android:id/home_search_view");
    private static final By CART_ICON   = By.id("com.dollargeneral.qa2.android:id/nav_bar_cart_layout");

    public HomePage(AndroidDriver driver) {
        this.driver    = driver;
        this.wait      = new WebDriverWait(driver, Duration.ofSeconds(90));
        this.shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    /**
     * Dismiss any promo/interstitial screen that appears after login
     * (e.g. "SAME DAY DELIVERY IS HERE!").
     */
    public void dismissPromoIfPresent() {
        try {
            List<WebElement> dismissBtn = shortWait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(BTN_DISMISS));
            if (!dismissBtn.isEmpty() && dismissBtn.get(0).isDisplayed()) {
                dismissBtn.get(0).click();
                System.out.println("Dismissed promo screen.");
            }
        } catch (Exception e) {
            System.out.println("No promo screen to dismiss.");
        }
    }

    public void verifyLoaded() {
        // Dismiss any interstitial screens that appear after login/fresh install
        // Loop up to 8 times to handle stacked overlays
        for (int attempt = 0; attempt < 8; attempt++) {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            String src = driver.getPageSource();

            // Already on home screen — done
            if (src.contains("id/home_search_view")) {
                System.out.println("Home screen loaded (attempt " + attempt + ").");
                return;
            }

            // Dump resource-ids on every attempt for diagnostics
            System.out.println("── verifyLoaded page source (attempt " + attempt + ") ──");
            for (String line : src.split("\n")) {
                if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                    String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                    if (!id.equals(line.trim())) System.out.println("  " + id);
                }
            }
            System.out.println("──────────────────────────────────────────");

            // Handle: email verification screen ("Remind me later" to skip)
            if (src.contains("id/remind_me_later_tv") || src.contains("id/submit_verification_email_btn")) {
                By remindLater = By.id("com.dollargeneral.qa2.android:id/remind_me_later_tv");
                try {
                    driver.findElement(remindLater).click();
                    System.out.println("Dismissed email verification (Remind me later).");
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    continue;
                } catch (Exception ignored) {}
            }

            // Handle: address selection dialog (appears after tapping Start Shopping)
            // button1 = positive/OK in Android AlertDialog (confirm address)
            if (src.contains("id/address_list") || src.contains("id/address_radio_button")) {
                By btn1 = By.id("android:id/button1");
                try {
                    driver.findElement(btn1).click();
                    System.out.println("Confirmed delivery address (button1).");
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    continue;
                } catch (Exception ignored) {}
                // If button1 not tapped, try close_btn to dismiss
                By closeBtn = By.id("com.dollargeneral.qa2.android:id/close_btn");
                try {
                    driver.findElement(closeBtn).click();
                    System.out.println("Closed address dialog (close_btn).");
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    continue;
                } catch (Exception ignored) {}
            }

            // Handle: "Start shopping" CTA — takes us directly to home (check BEFORE dismiss)
            if (src.contains("id/start_shopping")) {
                try {
                    driver.findElement(BTN_START_SHOP).click();
                    System.out.println("Tapped Start Shopping.");
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    continue;
                } catch (Exception ignored) {}
            }

            // Handle: promo/interstitial dismiss button (only if no start_shopping)
            if (src.contains("id/dismiss")) {
                try {
                    driver.findElement(BTN_DISMISS).click();
                    System.out.println("Dismissed overlay (id/dismiss).");
                    continue;
                } catch (Exception ignored) {}
            }

            // Handle: WebView interstitial — press back
            if (src.contains("WebView") || (src.contains("id/webView") && !src.contains("id/home_search_view"))) {
                System.out.println("WebView interstitial detected, pressing back...");
                driver.navigate().back();
                continue;
            }

            // Handle: any "Continue", "Skip", "Got it", "OK", "Allow" button
            By genericContinue = By.xpath(
                "//*[@text='Continue' or @text='CONTINUE' or @text='Skip' or @text='SKIP' " +
                "or @text='Got it' or @text='GOT IT' or @text='OK' or @text='Allow' or @text='Next']");
            try {
                List<WebElement> btns = driver.findElements(genericContinue);
                if (!btns.isEmpty() && btns.get(0).isDisplayed()) {
                    String btnText = btns.get(0).getText();
                    btns.get(0).click();
                    System.out.println("Tapped generic continue button: " + btnText);
                    continue;
                }
            } catch (Exception ignored) {}
        }

        // Final wait for home screen search bar
        wait.until(ExpectedConditions.presenceOfElementLocated(SEARCH_BAR));
        System.out.println("Home screen loaded.");
    }

    public void tapSearch() {
        dismissPromoIfPresent();
        wait.until(ExpectedConditions.elementToBeClickable(SEARCH_BAR)).click();
    }

    public void tapCart() {
        wait.until(ExpectedConditions.elementToBeClickable(CART_ICON)).click();
    }

    public int getCartCount() {
        try {
            By cartBadge = By.id("com.dollargeneral.qa2.android:id/tv_cart_count");
            WebElement badge = shortWait.until(
                ExpectedConditions.visibilityOfElementLocated(cartBadge));
            return Integer.parseInt(badge.getText().trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
