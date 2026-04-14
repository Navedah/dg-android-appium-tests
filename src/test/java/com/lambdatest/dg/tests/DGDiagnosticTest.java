package com.lambdatest.dg.tests;

import com.lambdatest.dg.base.DGBaseTest;
import org.openqa.selenium.OutputType;
import org.testng.annotations.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Dumps the page source XML and a screenshot after app launch so we can
 * identify the real resource IDs used by the DG app.
 *
 * Run this once:
 *   mvn test -Dsuite=dg-diagnostic.xml
 *
 * Then open target/dg-page-source.xml to see all element IDs on screen.
 */
public class DGDiagnosticTest extends DGBaseTest {

    @Test(description = "Login then dump home screen to identify real element IDs")
    public void dumpAppState() throws IOException {
        // Login first so we can inspect the home screen
        new com.lambdatest.dg.pages.SignInPage(driver)
            .loginWith("dollarnewcloud67@mailinator.com", "Test@123");

        // Wait for home to load, dismiss promo, then navigate past any interstitials
        try { Thread.sleep(12000); } catch (InterruptedException ignored) {}
        new com.lambdatest.dg.pages.HomePage(driver).dismissPromoIfPresent();
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

        // If still on a full-screen WebView (coupon/ad interstitial), press back
        String src = driver.getPageSource();
        if (src.contains("id/webView") && !src.contains("id/et_search") && !src.contains("id/search_bar")) {
            System.out.println("WebView interstitial detected, pressing back...");
            driver.navigate().back();
            try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
        }

        // ── Page source (contains all resource-ids on the current screen) ──────
        String pageSource = driver.getPageSource();
        String xmlPath = "target/dg-page-source.xml";
        Files.createDirectories(Paths.get("target"));
        try (FileWriter fw = new FileWriter(xmlPath)) {
            fw.write(pageSource);
        }
        System.out.println("✅  Page source written to: " + xmlPath);
        System.out.println("    Open it and search for 'resource-id' to find real element IDs.");

        // ── Screenshot ────────────────────────────────────────────────────────
        byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
        String imgPath = "target/dg-screenshot.png";
        Files.write(Paths.get(imgPath), screenshot);
        System.out.println("✅  Screenshot saved to: " + imgPath);

        // Print a condensed list of all resource-ids found
        System.out.println("\n── Resource IDs visible on screen ───────────────────────────────");
        for (String line : pageSource.split("\n")) {
            if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                if (!id.equals(line.trim())) {
                    System.out.println("  " + id);
                }
            }
        }
        System.out.println("──────────────────────────────────────────────────────────────────");

        // ── Tap search bar, type keyword, dump search results screen ─────────
        System.out.println("\n── Search screen inspection ──────────────────────────────────────");
        try {
            new com.lambdatest.dg.pages.HomePage(driver).tapSearch();
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

            // Type into search input (confirmed ID: search_src_text)
            org.openqa.selenium.WebElement searchInput = driver.findElement(
                org.openqa.selenium.By.id("com.dollargeneral.qa2.android:id/search_src_text"));
            searchInput.sendKeys("snickers");
            driver.executeScript("mobile: performEditorAction", java.util.Map.of("action", "search"));
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}

            String resultsSource = driver.getPageSource();
            try (FileWriter fw3 = new FileWriter("target/dg-search-results.xml")) { fw3.write(resultsSource); }
            System.out.println("✅  Search results page source written to: target/dg-search-results.xml");
            byte[] resultsShot = driver.getScreenshotAs(OutputType.BYTES);
            Files.write(Paths.get("target/dg-search-results.png"), resultsShot);
            System.out.println("✅  Search results screenshot saved to: target/dg-search-results.png");

            System.out.println("\n── Search results resource IDs ───────────────────────────────────");
            for (String line : resultsSource.split("\n")) {
                if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                    String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                    if (!id.equals(line.trim())) System.out.println("  " + id);
                }
            }

            // ── Tap first product and dump PDP ────────────────────────────────
            System.out.println("\n── Tapping first product to inspect PDP ──────────────────────────");
            try {
                org.openqa.selenium.WebElement firstProduct = driver.findElement(
                    org.openqa.selenium.By.id("com.dollargeneral.qa2.android:id/product_item_layout"));
                firstProduct.click();
                // Wait for PDP to fully load (lottie animation then actual content)
                try { Thread.sleep(8000); } catch (InterruptedException ignored) {}
                String pdpSource = driver.getPageSource();
                try (FileWriter fw4 = new FileWriter("target/dg-pdp.xml")) { fw4.write(pdpSource); }
                System.out.println("✅  PDP page source written to: target/dg-pdp.xml");
                byte[] pdpShot = driver.getScreenshotAs(OutputType.BYTES);
                Files.write(Paths.get("target/dg-pdp.png"), pdpShot);
                System.out.println("✅  PDP screenshot saved to: target/dg-pdp.png");
                System.out.println("\n── PDP resource IDs ──────────────────────────────────────────────");
                for (String line : pdpSource.split("\n")) {
                    if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                        String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                        if (!id.equals(line.trim())) System.out.println("  " + id);
                    }
                }
                // ── Tap Add to Cart (or + increment if already in cart) ───────────
                System.out.println("\n── Add to Cart → post-add state ──────────────────────────────────");
                try {
                    java.util.List<org.openqa.selenium.WebElement> addBtns =
                        driver.findElements(org.openqa.selenium.By.id(
                            "com.dollargeneral.qa2.android:id/add_to_cart_btn"));
                    java.util.List<org.openqa.selenium.WebElement> incrBtns =
                        driver.findElements(org.openqa.selenium.By.id(
                            "com.dollargeneral.qa2.android:id/add_btn"));

                    if (!addBtns.isEmpty() && addBtns.get(0).isDisplayed()) {
                        addBtns.get(0).click();
                        System.out.println("Tapped 'Add to cart' button.");
                    } else if (!incrBtns.isEmpty() && incrBtns.get(0).isDisplayed()) {
                        incrBtns.get(0).click();
                        System.out.println("Item already in cart — tapped '+' increment.");
                    } else {
                        System.out.println("Neither add_to_cart_btn nor add_btn found — skipping add.");
                    }

                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    String addedSource = driver.getPageSource();
                    try (FileWriter fw5 = new FileWriter("target/dg-added-to-cart.xml")) { fw5.write(addedSource); }
                    byte[] addedShot = driver.getScreenshotAs(OutputType.BYTES);
                    Files.write(Paths.get("target/dg-added-to-cart.png"), addedShot);
                    System.out.println("✅  Post-add page source: target/dg-added-to-cart.xml");
                    System.out.println("✅  Post-add screenshot: target/dg-added-to-cart.png");
                    System.out.println("\n── Post-add resource IDs ─────────────────────────────────────────");
                    for (String line : addedSource.split("\n")) {
                        if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                            String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                            if (!id.equals(line.trim())) System.out.println("  " + id);
                        }
                    }
                    // ── Tap View Cart and capture cart screen ─────────────────────
                    try {
                        // Try snackbar "View Cart" button first; fall back to cart nav bar icon
                        java.util.List<org.openqa.selenium.WebElement> snackBtns =
                            driver.findElements(org.openqa.selenium.By.id(
                                "com.dollargeneral.qa2.android:id/snackbar_action"));
                        if (!snackBtns.isEmpty() && snackBtns.get(0).isDisplayed()) {
                            snackBtns.get(0).click();
                            System.out.println("Tapped snackbar 'View Cart'.");
                        } else {
                            // Navigate via cart icon in nav bar
                            driver.findElement(org.openqa.selenium.By.id(
                                "com.dollargeneral.qa2.android:id/nav_bar_cart_layout")).click();
                            System.out.println("Tapped cart nav bar icon.");
                        }
                        try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                        String cartSource = driver.getPageSource();
                        try (FileWriter fw6 = new FileWriter("target/dg-cart.xml")) { fw6.write(cartSource); }
                        byte[] cartShot = driver.getScreenshotAs(OutputType.BYTES);
                        Files.write(Paths.get("target/dg-cart.png"), cartShot);
                        System.out.println("✅  Cart page source: target/dg-cart.xml");
                        System.out.println("✅  Cart screenshot: target/dg-cart.png");
                        System.out.println("\n── Cart resource IDs ─────────────────────────────────────────────");
                        for (String line : cartSource.split("\n")) {
                            if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                                String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                                if (!id.equals(line.trim())) System.out.println("  " + id);
                            }
                        }
                        // ── Tap Checkout, dismiss deals dialog, capture checkout screen ──
                        System.out.println("\n── Checkout screen inspection ────────────────────────────────────");
                        try {
                            driver.findElement(org.openqa.selenium.By.id(
                                "com.dollargeneral.qa2.android:id/cart_checkout_button")).click();
                            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

                            // Dismiss "You missed some deals" bottom sheet if present
                            java.util.List<org.openqa.selenium.WebElement> continueBtns =
                                driver.findElements(org.openqa.selenium.By.id(
                                    "com.dollargeneral.qa2.android:id/continue_button"));
                            if (!continueBtns.isEmpty() && continueBtns.get(0).isDisplayed()) {
                                continueBtns.get(0).click();
                                System.out.println("Dismissed 'missed deals' dialog.");
                                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                            }

                            // Capture actual checkout screen
                            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                            String checkoutSource = driver.getPageSource();
                            try (FileWriter fw7 = new FileWriter("target/dg-checkout.xml")) { fw7.write(checkoutSource); }
                            byte[] checkoutShot = driver.getScreenshotAs(OutputType.BYTES);
                            Files.write(Paths.get("target/dg-checkout.png"), checkoutShot);
                            System.out.println("✅  Checkout page source: target/dg-checkout.xml");
                            System.out.println("✅  Checkout screenshot: target/dg-checkout.png");
                            System.out.println("\n── Checkout resource IDs ─────────────────────────────────────────");
                            for (String line : checkoutSource.split("\n")) {
                                if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                                    String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                                    if (!id.equals(line.trim())) System.out.println("  " + id);
                                }
                            }
                            // Print text of visible elements to understand the screen
                            System.out.println("\n── Checkout visible text ─────────────────────────────────────────");
                            for (String line : checkoutSource.split("\n")) {
                                if (line.contains("text=") && !line.contains("text=\"\"")) {
                                    String txt = line.replaceAll(".*text=\"([^\"]+)\".*", "$1").trim();
                                    if (!txt.equals(line.trim()) && txt.length() > 2) System.out.println("  " + txt);
                                }
                            }
                            // ── Step 2: select ASAP and tap Continue ──────────────────────
                            System.out.println("\n── Step 2: Selecting ASAP delivery time and continuing ───────────");
                            try {
                                // Select ASAP delivery time
                                java.util.List<org.openqa.selenium.WebElement> asapTiles =
                                    driver.findElements(org.openqa.selenium.By.id(
                                        "com.dollargeneral.qa2.android:id/delivery_asap_tile"));
                                if (!asapTiles.isEmpty()) {
                                    asapTiles.get(0).click();
                                    System.out.println("Selected ASAP delivery.");
                                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                                }
                                // Tap Continue
                                driver.findElement(org.openqa.selenium.By.id(
                                    "com.dollargeneral.qa2.android:id/continue_btn")).click();
                                System.out.println("Tapped Continue (step 2).");
                                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}

                                String step2Source = driver.getPageSource();
                                try (FileWriter fw8 = new FileWriter("target/dg-checkout2.xml")) { fw8.write(step2Source); }
                                Files.write(Paths.get("target/dg-checkout2.png"), driver.getScreenshotAs(OutputType.BYTES));
                                System.out.println("✅  Checkout step 2 page source: target/dg-checkout2.xml");
                                System.out.println("\n── Step 2 resource IDs ───────────────────────────────────────────");
                                for (String line : step2Source.split("\n")) {
                                    if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                                        String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                                        if (!id.equals(line.trim())) System.out.println("  " + id);
                                    }
                                }
                                System.out.println("\n── Step 2 visible text ───────────────────────────────────────────");
                                java.util.Set<String> seenTexts2 = new java.util.LinkedHashSet<>();
                                for (String line : step2Source.split("\n")) {
                                    if (line.contains("text=") && !line.contains("text=\"\"")) {
                                        String txt = line.replaceAll(".*\\btext=\"([^\"]+)\".*", "$1").trim();
                                        if (!txt.equals(line.trim()) && txt.length() > 2) seenTexts2.add(txt);
                                    }
                                }
                                seenTexts2.forEach(t -> System.out.println("  " + t));

                                // ── Step 3: tap first time slot on "Schedule for later" screen ──
                                System.out.println("\n── Step 3: Selecting a time slot ────────────────────────────────");
                                try {
                                    // Time slots have no specific resource-id; find by XPath text pattern
                                    org.openqa.selenium.WebElement firstSlot = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
                                        .until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(
                                            org.openqa.selenium.By.xpath("//*[contains(@text,'AM') or contains(@text,'PM')][1]")));
                                    System.out.println("Found time slot: " + firstSlot.getText());
                                    firstSlot.click();
                                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

                                    String step3Source = driver.getPageSource();
                                    try (FileWriter fw9 = new FileWriter("target/dg-checkout3.xml")) { fw9.write(step3Source); }
                                    Files.write(Paths.get("target/dg-checkout3.png"), driver.getScreenshotAs(OutputType.BYTES));
                                    System.out.println("✅  After time slot tap: target/dg-checkout3.xml");
                                    System.out.println("\n── Step 3 resource IDs ───────────────────────────────────────────");
                                    for (String line : step3Source.split("\n")) {
                                        if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                                            String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                                            if (!id.equals(line.trim())) System.out.println("  " + id);
                                        }
                                    }
                                    System.out.println("\n── Step 3 visible text ───────────────────────────────────────────");
                                    java.util.Set<String> seenTexts3 = new java.util.LinkedHashSet<>();
                                    for (String line : step3Source.split("\n")) {
                                        if (line.contains("text=") && !line.contains("text=\"\"")) {
                                            String txt = line.replaceAll(".*\\btext=\"([^\"]+)\".*", "$1").trim();
                                            if (!txt.equals(line.trim()) && txt.length() > 2) seenTexts3.add(txt);
                                        }
                                    }
                                    seenTexts3.forEach(t -> System.out.println("  " + t));

                                    // ── Step 4: tap Continue then Save ────────────────────────────
                                    System.out.println("\n── Step 4: Continue → Save ───────────────────────────────────────");
                                    // Try all possible button IDs/texts for Continue
                                    String[] contIds = {
                                        "com.dollargeneral.qa2.android:id/continue_btn",
                                        "com.dollargeneral.qa2.android:id/btn_continue"
                                    };
                                    boolean tappedCont = false;
                                    for (String cid : contIds) {
                                        java.util.List<org.openqa.selenium.WebElement> cb = driver.findElements(org.openqa.selenium.By.id(cid));
                                        if (!cb.isEmpty() && cb.get(0).isDisplayed()) {
                                            cb.get(0).click();
                                            System.out.println("Tapped Continue (" + cid + ")");
                                            tappedCont = true;
                                            break;
                                        }
                                    }
                                    if (!tappedCont) {
                                        // Fall back to XPath text
                                        java.util.List<org.openqa.selenium.WebElement> contByText =
                                            driver.findElements(org.openqa.selenium.By.xpath("//*[@text='Continue']"));
                                        if (!contByText.isEmpty()) {
                                            contByText.get(0).click();
                                            System.out.println("Tapped Continue (by text)");
                                        } else {
                                            System.out.println("No Continue button found.");
                                        }
                                    }
                                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}

                                    String step4Source = driver.getPageSource();
                                    try (FileWriter fw10 = new FileWriter("target/dg-checkout4.xml")) { fw10.write(step4Source); }
                                    Files.write(Paths.get("target/dg-checkout4.png"), driver.getScreenshotAs(OutputType.BYTES));
                                    System.out.println("✅  After Continue: target/dg-checkout4.xml");
                                    System.out.println("\n── Step 4 resource IDs ───────────────────────────────────────────");
                                    for (String line : step4Source.split("\n")) {
                                        if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                                            String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                                            if (!id.equals(line.trim())) System.out.println("  " + id);
                                        }
                                    }
                                    System.out.println("\n── Step 4 visible text ───────────────────────────────────────────");
                                    java.util.Set<String> seenTexts4 = new java.util.LinkedHashSet<>();
                                    for (String line : step4Source.split("\n")) {
                                        if (line.contains("text=") && !line.contains("text=\"\"")) {
                                            String txt = line.replaceAll(".*\\btext=\"([^\"]+)\".*", "$1").trim();
                                            if (!txt.equals(line.trim()) && txt.length() > 2) seenTexts4.add(txt);
                                        }
                                    }
                                    seenTexts4.forEach(t -> System.out.println("  " + t));
                                } catch (Exception step3Ex) {
                                    System.out.println("Could not proceed through time slot: " + step3Ex.getMessage());
                                }
                            } catch (Exception step2Ex) {
                                System.out.println("Could not proceed to step 2: " + step2Ex.getMessage());
                            }
                        } catch (Exception checkEx) {
                            System.out.println("Could not capture Checkout: " + checkEx.getMessage());
                        }
                    } catch (Exception viewEx) {
                        System.out.println("Could not navigate to Cart: " + viewEx.getMessage());
                    }
                } catch (Exception addEx) {
                    System.out.println("Could not tap Add to Cart: " + addEx.getMessage());
                }
            } catch (Exception ex) {
                System.out.println("Could not capture PDP: " + ex.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Could not capture search results: " + e.getMessage());
        }
        System.out.println("──────────────────────────────────────────────────────────────────");
    }
}
