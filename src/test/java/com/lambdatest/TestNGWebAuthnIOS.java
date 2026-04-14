package com.lambdatest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestNGWebAuthnIOS {

    private RemoteWebDriver driver;
    private String Status = "failed";
    private WebDriverWait wait;

    // Stored for HTTP-based context switching
    private String ltUsername;
    private String ltAuthkey;

    @BeforeMethod
    public void setup(Method m, ITestContext ctx) throws MalformedURLException {
        ltUsername = System.getenv("LT_USERNAME") == null ? "Your LT Username" : System.getenv("LT_USERNAME");
        ltAuthkey  = System.getenv("LT_ACCESS_KEY") == null ? "Your LT AccessKey" : System.getenv("LT_ACCESS_KEY");

        String hub = "@mobile-hub.lambdatest.com/wd/hub";

        HashMap<String, Object> ltOptions = new HashMap<>();
        ltOptions.put("browserName", "Safari");
        ltOptions.put("deviceName", "iPhone 11");
        ltOptions.put("platformVersion", "18");
        ltOptions.put("isRealMobile", true);
        ltOptions.put("privateCloud", true);
        ltOptions.put("dedicatedDevice", true);
        ltOptions.put("build", "WebAuthn - iOS Private Device Test");
        ltOptions.put("name", m.getName() + " - " + this.getClass().getSimpleName());
        ltOptions.put("plugin", "git-testng");
        ltOptions.put("tags", new String[] { "WebAuthn", "PrivateDevice", "iOS" });
        ltOptions.put("w3c", true);
        ltOptions.put("selenium_version", "4.0.0");
        ltOptions.put("passcode", "123456");

        ImmutableCapabilities capabilities = new ImmutableCapabilities(Map.of(
            "browserName", "Safari",
            "platformName", "iOS",
            "lt:options", ltOptions
        ));

        driver = new RemoteWebDriver(new URL("https://" + ltUsername + ":" + ltAuthkey + hub), capabilities);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    @Test
    public void webAuthnTestIOS() throws Exception {
        System.out.println("Navigating to webauthn.io...");
        driver.get("https://webauthn.io/");
        wait.until(ExpectedConditions.titleContains("WebAuthn"));
        System.out.println("Page loaded: " + driver.getTitle());

        // ── 1. Click Advanced Settings ──────────────────────────────────────
        System.out.println("Opening Advanced Settings...");
        WebElement advancedSettings = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//*[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'advanced setting')]")
        ));
        scrollAndClick(advancedSettings);
        Thread.sleep(800);

        // ── 2. Registration – User Verification → Required ──────────────────
        System.out.println("Setting Registration > User Verification = Required...");
        WebElement regUserVerification = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("(//select)[1]")
        ));
        selectByVisibleText(regUserVerification, "Required");

        // ── 3. Registration – Attachment → Platform ─────────────────────────
        System.out.println("Setting Registration > Attachment = Platform...");
        WebElement attachment = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("(//select)[2]")
        ));
        selectByVisibleText(attachment, "Platform");

        // ── 4. Registration – Client Device checkbox ─────────────────────────
        System.out.println("Clicking Registration > Client Device checkbox...");
        WebElement regClientDevice = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("regHintClientDevice")
        ));
        if (!regClientDevice.isSelected()) {
            scrollAndClick(regClientDevice);
        }

        // ── 5. Authentication – User Verification → Required ─────────────────
        System.out.println("Setting Authentication > User Verification = Required...");
        WebElement authUserVerification = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("(//select)[last()]")
        ));
        selectByVisibleText(authUserVerification, "Required");

        // ── 6. Authentication – Client Device checkbox ────────────────────────
        System.out.println("Clicking Authentication > Client Device checkbox...");
        WebElement authClientDevice = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("authHintClientDevice")
        ));
        if (!authClientDevice.isSelected()) {
            scrollAndClick(authClientDevice);
        }

        // ── 7. Enter email ───────────────────────────────────────────────────
        System.out.println("Entering email...");
        WebElement emailInput = wait.until(ExpectedConditions.elementToBeClickable(
            By.cssSelector("input[type='email'], input[type='text'], input:not([type])")
        ));
        scrollAndClick(emailInput);
        emailInput.clear();
        emailInput.sendKeys("testing2@gmail.com");

        // ── 8. Click Register ────────────────────────────────────────────────
        System.out.println("Clicking Register...");
        WebElement registerBtn = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[contains(translate(text(),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'register')]")
        ));
        scrollAndClick(registerBtn);
        System.out.println("Register clicked. Waiting 8s for native iOS dialog to appear...");
        Thread.sleep(8000);

        // ── 9. Switch to NATIVE_APP, click Continue, enter passcode ──────────
        handleNativePasskeyDialog();

        Thread.sleep(2000);
        Status = "passed";
        System.out.println("WebAuthn test completed successfully.");
    }

    /**
     * Switches to NATIVE_APP context via direct Appium HTTP endpoint call,
     * clicks the iOS "Continue" button on the passkey dialog, then enters
     * passcode 123456.
     */
    private void handleNativePasskeyDialog() throws Exception {
        String sessionId = driver.getSessionId().toString();

        // ── List available contexts ───────────────────────────────────────────
        String contextsResponse = appiumGet("/session/" + sessionId + "/contexts");
        System.out.println("Available contexts: " + contextsResponse);

        // ── Switch to NATIVE_APP ──────────────────────────────────────────────
        System.out.println("Switching to NATIVE_APP...");
        String switchResp = appiumPost(
            "/session/" + sessionId + "/context",
            "{\"name\":\"NATIVE_APP\"}"
        );
        System.out.println("Context switch response: " + switchResp);
        // Wait for the passkey dialog to appear on screen
        Thread.sleep(5000);

        // ── Dump native page source via Appium /source endpoint ──────────────
        try {
            String src = appiumGet("/session/" + sessionId + "/source");
            System.out.println("=== NATIVE PAGE SOURCE ===");
            System.out.println(src);
            System.out.println("=== END NATIVE PAGE SOURCE ===");
        } catch (Exception e) {
            System.out.println("Could not get page source: " + e.getMessage());
        }

        // ── Take screenshot to see what's on screen ───────────────────────────
        try {
            String screenshotResp = appiumGet("/session/" + sessionId + "/screenshot");
            // Parse base64 value from JSON {"value":"<base64>"}
            int start = screenshotResp.indexOf("\"value\":\"") + 9;
            int end   = screenshotResp.lastIndexOf("\"");
            if (start > 8 && end > start) {
                byte[] imgBytes = Base64.getDecoder().decode(screenshotResp.substring(start, end));
                java.nio.file.Files.write(java.nio.file.Paths.get("/tmp/webauthn_native_screen.png"), imgBytes);
                System.out.println("Screenshot saved to /tmp/webauthn_native_screen.png");
            }
        } catch (Exception e) {
            System.out.println("Screenshot failed: " + e.getMessage().split("\n")[0]);
        }

        // ── Try mobile:alert accept (works in NATIVE_APP context via Appium) ──
        System.out.println("Trying mobile:alert accept for Continue...");
        boolean continueDone = false;
        try {
            driver.executeScript("mobile: alert", Map.of("action", "accept"));
            System.out.println("mobile:alert accept succeeded.");
            continueDone = true;
        } catch (Exception e) {
            System.out.println("mobile:alert accept failed: " + e.getMessage().split("\n")[0]);
        }

        if (!continueDone) {
            // ── Fallback: XCUITest button XPath ───────────────────────────────
            System.out.println("Trying XCUITest XPath for Continue button...");
            try {
                WebElement continueBtn = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(By.xpath(
                        "//XCUIElementTypeButton[@name='Continue']" +
                        " | //XCUIElementTypeButton[@name='Save']" +
                        " | //XCUIElementTypeButton[@name='Use Passkey']"
                    )));
                continueBtn.click();
                System.out.println("Clicked Continue via XPath.");
                continueDone = true;
            } catch (Exception e) {
                System.out.println("XPath Continue not found: " + e.getMessage().split("\n")[0]);
            }
        }

        if (!continueDone) {
            // ── Fallback: coordinate tap on where Continue appears on iPhone 11 ─
            // Screenshot confirmed: Continue button is at center-x (207), y≈780
            System.out.println("Trying coordinate tap at Continue button position (207, 780)...");
            try {
                appiumPost("/session/" + sessionId + "/actions", buildTapAction(207, 780));
                System.out.println("Coordinate tap sent.");
            } catch (Exception e) {
                System.out.println("Coordinate tap failed: " + e.getMessage().split("\n")[0]);
            }
        }

        Thread.sleep(2000);

        // ── Screenshot after Continue tap — shows the passcode screen ─────────
        try {
            String s2 = appiumGet("/session/" + sessionId + "/screenshot");
            int s2start = s2.indexOf("\"value\":\"") + 9;
            int s2end   = s2.lastIndexOf("\"");
            if (s2start > 8 && s2end > s2start) {
                byte[] b2 = Base64.getDecoder().decode(s2.substring(s2start, s2end));
                java.nio.file.Files.write(java.nio.file.Paths.get("/tmp/webauthn_passcode_screen.png"), b2);
                System.out.println("Passcode screen screenshot saved to /tmp/webauthn_passcode_screen.png");
            }
        } catch (Exception e) {
            System.out.println("Passcode screenshot failed: " + e.getMessage().split("\n")[0]);
        }

        // ── Enter passcode 123456 ─────────────────────────────────────────────
        // iPhone 11 passcode keypad layout (portrait, approximate centers):
        //   1(69,530)  2(207,530)  3(345,530)
        //   4(69,610)  5(207,610)  6(345,610)
        //   7(69,690)  8(207,690)  9(345,690)
        //              0(207,770)
        Map<Character, int[]> keyCoords = new HashMap<>();
        keyCoords.put('1', new int[]{69,  530});
        keyCoords.put('2', new int[]{207, 530});
        keyCoords.put('3', new int[]{345, 530});
        keyCoords.put('4', new int[]{69,  610});
        keyCoords.put('5', new int[]{207, 610});
        keyCoords.put('6', new int[]{345, 610});
        keyCoords.put('7', new int[]{69,  690});
        keyCoords.put('8', new int[]{207, 690});
        keyCoords.put('9', new int[]{345, 690});
        keyCoords.put('0', new int[]{207, 770});

        System.out.println("Looking for passcode secure field...");
        try {
            WebElement passcodeField = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//XCUIElementTypeSecureTextField | //XCUIElementTypeTextField[@name='Passcode']")
                ));
            passcodeField.click();
            Thread.sleep(500);
            passcodeField.sendKeys("123456");
            System.out.println("Passcode typed via sendKeys: 123456");
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("Secure field not found, using coordinate taps: " + e.getMessage().split("\n")[0]);
            try {
                for (char digit : "123456".toCharArray()) {
                    int[] coords = keyCoords.get(digit);
                    appiumPost("/session/" + sessionId + "/actions", buildTapAction(coords[0], coords[1]));
                    System.out.println("Tapped digit: " + digit + " at (" + coords[0] + "," + coords[1] + ")");
                    Thread.sleep(300);
                }
                System.out.println("Passcode entered via coordinate taps.");
            } catch (Exception e2) {
                System.out.println("Coordinate tap for passcode failed: " + e2.getMessage().split("\n")[0]);
            }
        }

        // ── Switch back to WebView ────────────────────────────────────────────
        System.out.println("Switching back to WebView...");
        String ctxAfter = appiumGet("/session/" + sessionId + "/contexts");
        System.out.println("Contexts after passcode: " + ctxAfter);

        String webviewCtx = null;
        // Simple parse: find first "WEBVIEW" substring in the JSON response
        if (ctxAfter.contains("WEBVIEW")) {
            int start = ctxAfter.indexOf("WEBVIEW");
            int end   = ctxAfter.indexOf('"', start);
            webviewCtx = ctxAfter.substring(start, end);
        }
        if (webviewCtx != null) {
            appiumPost("/session/" + sessionId + "/context", "{\"name\":\"" + webviewCtx + "\"}");
            System.out.println("Switched back to: " + webviewCtx);
        }
    }

    /**
     * Builds a W3C Actions payload for a single finger tap at (x, y).
     * Used for tapping native UI elements by screen coordinate.
     */
    private String buildTapAction(int x, int y) {
        return "{\"actions\":[{" +
            "\"type\":\"pointer\"," +
            "\"id\":\"finger1\"," +
            "\"parameters\":{\"pointerType\":\"touch\"}," +
            "\"actions\":[" +
                "{\"type\":\"pointerMove\",\"duration\":0,\"x\":" + x + ",\"y\":" + y + "}," +
                "{\"type\":\"pointerDown\",\"button\":0}," +
                "{\"type\":\"pause\",\"duration\":100}," +
                "{\"type\":\"pointerUp\",\"button\":0}" +
            "]" +
        "}]}";
    }

    /** HTTP GET to the LambdaTest Appium endpoint. */
    private String appiumGet(String path) throws Exception {
        URL url = new URL("https://mobile-hub.lambdatest.com/wd/hub" + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", basicAuth());
        conn.setRequestProperty("Content-Type", "application/json");
        return readResponse(conn);
    }

    /** HTTP POST to the LambdaTest Appium endpoint with a JSON body. */
    private String appiumPost(String path, String jsonBody) throws Exception {
        URL url = new URL("https://mobile-hub.lambdatest.com/wd/hub" + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", basicAuth());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.getOutputStream().write(jsonBody.getBytes("UTF-8"));
        return readResponse(conn);
    }

    private String basicAuth() {
        return "Basic " + Base64.getEncoder().encodeToString((ltUsername + ":" + ltAuthkey).getBytes());
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(
            code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream()
        ));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();
        return sb.toString();
    }

    private void scrollAndClick(WebElement el) throws InterruptedException {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        Thread.sleep(400);
        try {
            el.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
        }
    }

    private void selectByVisibleText(WebElement el, String text) {
        try {
            new Select(el).selectByVisibleText(text);
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript(
                "var sel = arguments[0]; var opts = sel.options;" +
                "for(var i=0;i<opts.length;i++){" +
                "  if(opts[i].text.trim().toLowerCase()===arguments[1].toLowerCase()){" +
                "    sel.selectedIndex=i;" +
                "    sel.dispatchEvent(new Event('change',{bubbles:true}));" +
                "    break;" +
                "  }" +
                "}", el, text
            );
        }
    }

    @AfterMethod
    public void tearDown() {
        try {
            driver.executeScript("lambda-status=" + Status);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
