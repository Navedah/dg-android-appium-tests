package com.lambdatest.dg.base;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Set;

/**
 * Base class for all DG app tests on LambdaTest real devices.
 *
 * Connection model (app-only — no test APK required):
 *   1. App APK is uploaded to LambdaTest → returns lt://APP_ID
 *   2. AndroidDriver connects to LambdaTest Appium hub with that app ID
 *   3. LambdaTest installs the app on a real device and Appium drives it
 *      via the UIAutomator2 engine — same as your on-device framework
 */
public class DGBaseTest {

    protected AndroidDriver driver;
    private String testStatus = "failed";

    // ── LambdaTest hub ────────────────────────────────────────────────────────
    private static final String LT_HUB = "https://%s:%s@mobile-hub.lambdatest.com/wd/hub";

    /**
     * @param appUrl   LambdaTest app URL returned after upload, e.g. lt://APP_ID
     *                 Can also be set via system property: -DappUrl=lt://APP_ID
     * @param device   device name, e.g. "Samsung Galaxy S21"
     * @param version  Android OS version, e.g. "11"
     */
    @BeforeMethod(alwaysRun = true)
    @Parameters({"appUrl", "device", "version"})
    public void setUp(String appUrl, String device, String version, Method method)
            throws MalformedURLException {

        String username  = envOrDefault("LT_USERNAME",   "Your LT Username");
        String accessKey = envOrDefault("LT_ACCESS_KEY", "Your LT AccessKey");

        // Resolve appUrl: TestNG param → system property → env variable
        if (appUrl == null || appUrl.isEmpty()) {
            appUrl = System.getProperty("appUrl", System.getenv("LT_APP_URL"));
        }
        if (appUrl == null || appUrl.isEmpty()) {
            throw new IllegalStateException(
                "appUrl not set. Upload the DG APK and pass -DappUrl=lt://APP_ID");
        }

        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setDeviceName(device);
        options.setPlatformVersion(version);
        options.setApp(appUrl);                    // lt://APP_ID from upload step
        options.setAutoGrantPermissions(true);
        options.setNoReset(false);
        options.setFullReset(true);
        options.setNewCommandTimeout(Duration.ofSeconds(300));
        // Force Appium's Unicode bypass keyboard so sendKeys() bypasses the device IME
        options.setCapability("unicodeKeyboard", true);
        options.setCapability("resetKeyboard", true);

        // LambdaTest-specific options
        options.setCapability("lt:options", ltOptions(username, accessKey, method, device));

        String hubUrl = String.format(LT_HUB, username, accessKey);
        driver = new AndroidDriver(new URL(hubUrl), options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));

        // Wait for app to finish loading (splash screen / network init)
        // LambdaTest full-reset can take 30-90s depending on device load
        try { Thread.sleep(60000); } catch (InterruptedException ignored) {}
        System.out.println("App ready — initial sleep complete.");
        // Dump initial page source for debugging what screen the app opened to
        try {
            String initSrc = driver.getPageSource();
            System.out.println("── Initial page source (after startup sleep) ──");
            for (String line : initSrc.split("\n")) {
                if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                    String id = line.replaceAll(".*resource-id=\"([^\"]+)\".*", "$1").trim();
                    if (!id.equals(line.trim())) System.out.println("  " + id);
                }
            }
            System.out.println("────────────────────────────────────────────────");
        } catch (Exception ignored) {}
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        testStatus = result.isSuccess() ? "passed" : "failed";
        try {
            if (driver != null)
                ((JavascriptExecutor) driver).executeScript("lambda-status=" + testStatus);
        } finally {
            if (driver != null) driver.quit();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Switch Appium context to the first available WEBVIEW.
     * Call this before interacting with any screen rendered inside a WebView.
     */
    protected void switchToWebView() {
        // Give the WebView time to fully initialize
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        Set<String> contexts = driver.getContextHandles();
        System.out.println("Available contexts: " + contexts);
        for (String ctx : contexts) {
            if (ctx.toUpperCase().contains("WEBVIEW") && !ctx.contains("stetho")) {
                driver.context(ctx);
                System.out.println("Switched to context: " + ctx);
                return;
            }
        }
        System.out.println("No WEBVIEW context found — staying in NATIVE_APP. Contexts: " + contexts);
    }

    /** Switch back to native app context (for toolbar, dialogs, etc.) */
    protected void switchToNative() {
        driver.context("NATIVE_APP");
    }

    /** Read test data: system property → env variable → default */
    protected String testData(String key, String defaultValue) {
        String val = System.getProperty(key);
        if (val == null) val = System.getenv(key);
        return val != null ? val : defaultValue;
    }

    private java.util.HashMap<String, Object> ltOptions(
            String username, String accessKey, Method method, String device) {
        java.util.HashMap<String, Object> lt = new java.util.HashMap<>();
        lt.put("username",       username);
        lt.put("accessKey",      accessKey);
        lt.put("isRealMobile",   true);
        lt.put("build",          "DG Android - UIAutomator2 Order Flow");
        lt.put("name",           method.getName() + " | " + device);
        lt.put("project",        "DGAPP-Project");
        lt.put("devicelog",      true);
        lt.put("video",          true);
        lt.put("visual",         true);
        lt.put("autoGrantPermissions", true);
        lt.put("plugin",         "git-testng");
        return lt;
    }

    private static String envOrDefault(String key, String fallback) {
        String val = System.getenv(key);
        return (val != null && !val.isEmpty()) ? val : fallback;
    }
}
