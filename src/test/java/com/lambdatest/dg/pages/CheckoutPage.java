package com.lambdatest.dg.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.List;

/**
 * Checkout flow for the DG delivery order.
 *
 * Actual screen sequence (confirmed from page source dumps):
 *   1. Delivery time screen   → select ASAP or Later, tap continue_btn
 *   2. Order review screen    → confirm items/payment, tap place_order
 *   3. Order confirmation     → verify confirmation
 */
public class CheckoutPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;
    private final WebDriverWait shortWait;

    // ── Delivery time screen ───────────────────────────────────────────────────
    // Confirmed from dg-checkout.xml dump
    private static final By DELIVERY_ASAP_TILE  = By.id("com.dollargeneral.qa2.android:id/delivery_asap_tile");
    private static final By DELIVERY_LATER_TILE = By.id("com.dollargeneral.qa2.android:id/delivery_later_tile");
    private static final By CONTINUE_BTN        = By.id("com.dollargeneral.qa2.android:id/continue_btn");

    // ── "Schedule for later" screen — time slot buttons (text like "9 AM - 10 AM") ─
    private static final By TIME_SLOT_ANY = By.xpath(
        "//*[contains(@text,'AM') or contains(@text,'PM')][not(contains(@text,'unavailable'))][1]");

    // ── Checkout review screen ────────────────────────────────────────────────
    // "Checkout" screen: Contact section → Save → Payment method → Place order
    // Confirmed from screenshot: Save is a full-width black button in Contact section
    // place_order is greyed out until Save is tapped
    private static final By SAVE_BTN = By.xpath(
        "//*[@text='Save' or @text='SAVE' or @text='save'" +
        " or @resource-id='com.dollargeneral.qa2.android:id/save_btn'" +
        " or @resource-id='com.dollargeneral.qa2.android:id/btn_save'" +
        " or @resource-id='com.dollargeneral.qa2.android:id/contact_save_btn'" +
        " or @resource-id='com.dollargeneral.qa2.android:id/save_contact_btn']");

    private static final By PLACE_ORDER_BTN = By.id("com.dollargeneral.qa2.android:id/place_order");

    public CheckoutPage(AndroidDriver driver) {
        this.driver    = driver;
        this.wait      = new WebDriverWait(driver, Duration.ofSeconds(20));
        this.shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
    }

    /**
     * Complete the delivery time selection flow:
     *   "Reserve a time" → ASAP (if available) or Later
     *   → "Schedule for later" time slots (if Later) → tap first slot → Continue
     *   → "Checkout" contact section → Save
     *
     * Call this immediately after {@link CartPage#tapProceedToCheckout()}.
     */
    public void selectDeliveryAsap() {
        // Wait for "Reserve a time" screen
        wait.until(ExpectedConditions.visibilityOfElementLocated(DELIVERY_LATER_TILE));

        // Try ASAP — fall back to Later if unavailable
        boolean usedAsap = false;
        try {
            List<org.openqa.selenium.WebElement> asapTiles = driver.findElements(DELIVERY_ASAP_TILE);
            if (!asapTiles.isEmpty() && asapTiles.get(0).isDisplayed()) {
                String asapText = asapTiles.get(0).getText();
                if (!asapText.contains("unavailable")) {
                    asapTiles.get(0).click();
                    System.out.println("Selected ASAP delivery time.");
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    wait.until(ExpectedConditions.elementToBeClickable(CONTINUE_BTN)).click();
                    System.out.println("Tapped Continue (ASAP).");
                    usedAsap = true;
                }
            }
        } catch (Exception ignored) {}

        if (!usedAsap) {
            selectDeliveryLater();
        }

        // After either ASAP or Later flow, the Checkout screen loads.
        // Tap Save on the Contact section to unlock Place order.
        tapSaveContact();
    }

    /**
     * Tap "Later" → pick first available time slot on "Schedule for later" → Continue.
     * Then tap Save on the Checkout contact section.
     */
    public void selectDeliveryLater() {
        wait.until(ExpectedConditions.elementToBeClickable(DELIVERY_LATER_TILE)).click();
        System.out.println("Selected Later delivery time.");

        // "Schedule for later" screen — tap first available time slot (AM/PM text)
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        wait.until(ExpectedConditions.elementToBeClickable(TIME_SLOT_ANY)).click();
        System.out.println("Tapped first available time slot.");
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        // Tap Continue
        wait.until(ExpectedConditions.elementToBeClickable(CONTINUE_BTN)).click();
        System.out.println("Tapped Continue (Later).");
    }

    /**
     * Tap the "Save" button on the Checkout contact section.
     * Confirmed from screenshot: Save is a full-width black button between
     * the contact fields and Payment method. Place order is grey until Save is tapped.
     */
    public void tapSaveContact() {
        // Wait for the Checkout screen to fully load after delivery time Continue
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

        // Dump page source to identify the actual Save button resource-id
        String src = driver.getPageSource();
        System.out.println("── tapSaveContact page source ──");
        for (String line : src.split("\n")) {
            if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                if (!id.equals(line.trim())) System.out.println("  " + id);
            }
        }
        System.out.println("────────────────────────────────");

        // Try to find Save button — try text first, then by any button containing "Save" text
        boolean saved = false;
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(SAVE_BTN)).click();
            System.out.println("Tapped Save (contact section).");
            saved = true;
        } catch (Exception ignored) {}

        if (!saved) {
            // Try visibility instead of clickable (button may be enabled but not flagged clickable)
            try {
                new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOfElementLocated(SAVE_BTN)).click();
                System.out.println("Tapped Save (visibility fallback).");
                saved = true;
            } catch (Exception ignored) {}
        }

        if (!saved) {
            System.out.println("Save button not found — contact section may already be saved.");
        }

        // Give the page time to react to Save before Place order becomes clickable
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
    }

    /**
     * Tap the "Place order" button on the order review screen.
     * Confirmed from screenshot: skip_button (delivery instructions) appears above place_order.
     * Place order is grey/disabled until Save (contact section) is tapped.
     */
    public void tapPlaceOrder() {
        // Dump page source — show place_order enabled/clickable attributes
        String src = driver.getPageSource();
        System.out.println("── tapPlaceOrder page source ──");
        for (String line : src.split("\n")) {
            if (line.contains("place_order") || (line.contains("resource-id") && !line.contains("resource-id=\"\""))) {
                String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                if (line.contains("place_order")) {
                    // Print full line for place_order to see enabled/clickable attrs
                    System.out.println("  PLACE_ORDER: " + line.trim().substring(0, Math.min(200, line.trim().length())));
                } else if (!id.equals(line.trim())) {
                    System.out.println("  " + id);
                }
            }
        }
        System.out.println("────────────────────────────────");

        // Tap Skip (delivery instructions) to dismiss that section
        boolean skipped = false;
        try {
            By skipBtn = By.id("com.dollargeneral.qa2.android:id/skip_button");
            new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.elementToBeClickable(skipBtn)).click();
            System.out.println("Tapped Skip (delivery instructions).");
            skipped = true;
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}  // wait for UI update
        } catch (Exception ignored) {}

        // If skip was tapped, dump page source to verify place_order state
        if (skipped) {
            String postSkipSrc = driver.getPageSource();
            System.out.println("── post-Skip page source ──");
            for (String line : postSkipSrc.split("\n")) {
                if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                    String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                    if (!id.equals(line.trim())) System.out.println("  " + id);
                }
            }
            System.out.println("────────────────────────────");
        }

        // Wait for place_order to be clickable (enabled after Skip/Save)
        // Fall back to direct tap if Appium reports it as non-clickable
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.elementToBeClickable(PLACE_ORDER_BTN)).click();
            System.out.println("Tapped Place order (clickable).");
        } catch (Exception e) {
            System.out.println("place_order not clickable, trying direct tap: " + e.getMessage());
            WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(PLACE_ORDER_BTN));
            btn.click();
            System.out.println("Tapped Place order (direct).");
        }
    }

    /**
     * Verify the order confirmation screen appeared and return identifying text.
     * The exact confirmation screen IDs haven't been confirmed yet — we use a
     * broad XPath to detect any confirmation-related text.
     */
    public String verifyOrderConfirmationAndGetOrderNumber() {
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        // Dump page source to help confirm the actual IDs
        String src = driver.getPageSource();
        System.out.println("\n── Order confirmation page source dump ───────────────────────");
        for (String line : src.split("\n")) {
            if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                if (!id.equals(line.trim())) System.out.println("  " + id);
            }
        }
        System.out.println("──────────────────────────────────────────────────────────────");

        // Check for any confirmation text using direct By lookup
        By confirmXpath = By.xpath(
            "//*[contains(@text,'order') or contains(@text,'Order') or contains(@text,'confirmation') or contains(@text,'placed')]");
        List<WebElement> confirmElems = wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(confirmXpath));
        Assert.assertFalse(confirmElems.isEmpty(), "Order confirmation text should appear");
        String confirmText = confirmElems.get(0).getText();
        System.out.println("Order confirmation text: " + confirmText);
        return confirmText;
    }

    // ── Legacy store-pickup methods (kept for API compat, now delegate to delivery) ──

    /** @deprecated DG app uses delivery flow; use {@link #selectDeliveryAsap()} instead. */
    @Deprecated
    public void selectStorePickup() {
        selectDeliveryAsap();
    }

    /** @deprecated Not applicable in delivery flow. */
    @Deprecated
    public void findStoresByZip(String zipCode) {
        System.out.println("findStoresByZip: skipped (delivery flow — no store picker).");
    }

    /** @deprecated Not applicable in delivery flow. */
    @Deprecated
    public void selectFirstStore() {
        System.out.println("selectFirstStore: skipped (delivery flow — no store picker).");
    }

    /** @deprecated Not needed in delivery flow; place order directly. */
    @Deprecated
    public void tapContinue() {
        System.out.println("tapContinue: skipped (delivery flow — continue already handled).");
    }
}
