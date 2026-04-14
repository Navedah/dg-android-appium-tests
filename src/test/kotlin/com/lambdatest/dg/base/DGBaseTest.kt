package com.lambdatest.dg.kotlin.base

import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import org.openqa.selenium.JavascriptExecutor
import org.testng.ITestResult
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Parameters
import java.lang.reflect.Method
import java.net.URL
import java.time.Duration

/**
 * Kotlin base class for DG Android tests on LambdaTest real devices.
 * Uses Appium + UIAutomator2 engine (no test APK required).
 *
 * Run with:
 *   mvn test -Dsuite=dg-android-kotlin.xml
 */
open class DGBaseTest {

    protected lateinit var driver: AndroidDriver
    private var testStatus = "failed"

    companion object {
        private const val LT_HUB = "https://%s:%s@mobile-hub.lambdatest.com/wd/hub"
    }

    @BeforeMethod(alwaysRun = true)
    @Parameters("appUrl", "device", "version")
    fun setUp(appUrl: String?, device: String, version: String, method: Method) {
        val username  = envOrDefault("LT_USERNAME",   "Your LT Username")
        val accessKey = envOrDefault("LT_ACCESS_KEY", "Your LT AccessKey")

        val resolvedAppUrl = appUrl?.takeIf { it.isNotEmpty() }
            ?: System.getProperty("appUrl")
            ?: System.getenv("LT_APP_URL")
            ?: throw IllegalStateException("appUrl not set. Upload the DG APK and pass -DappUrl=lt://APP_ID")

        val options = UiAutomator2Options().apply {
            setPlatformName("Android")
            setDeviceName(device)
            setPlatformVersion(version)
            setApp(resolvedAppUrl)
            setAutoGrantPermissions(true)
            setNoReset(false)
            setFullReset(true)
            setNewCommandTimeout(Duration.ofSeconds(300))
            // Force Appium's Unicode bypass keyboard so sendKeys() bypasses the device IME
            setCapability("unicodeKeyboard", true)
            setCapability("resetKeyboard", true)
            setCapability("lt:options", ltOptions(username, accessKey, method, device))
        }

        val hubUrl = LT_HUB.format(username, accessKey)
        driver = AndroidDriver(URL(hubUrl), options)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30))

        // Wait for app to finish loading (splash screen / network init)
        Thread.sleep(60000)
        println("App ready — initial sleep complete.")
        // Dump initial page source for debugging what screen the app opened to
        try {
            val initSrc = driver.pageSource
            println("── Initial page source (after startup sleep) ──")
            initSrc.split("\n").forEach { line ->
                if (line.contains("resource-id") && !line.contains("resource-id=\"\"")) {
                    val id = line.replace(Regex(".*resource-id=\"([^\"]+)\".*"), "$1").trim()
                    if (id != line.trim()) println("  $id")
                }
            }
            println("────────────────────────────────────────────────")
        } catch (ignored: Exception) {}
    }

    @AfterMethod(alwaysRun = true)
    fun tearDown(result: ITestResult) {
        testStatus = if (result.isSuccess) "passed" else "failed"
        try {
            if (::driver.isInitialized)
                (driver as JavascriptExecutor).executeScript("lambda-status=$testStatus")
        } finally {
            if (::driver.isInitialized) driver.quit()
        }
    }

    /** Switch Appium context to the first available WEBVIEW. */
    protected fun switchToWebView() {
        Thread.sleep(3000)
        val contexts = driver.contextHandles
        println("Available contexts: $contexts")
        for (ctx in contexts) {
            if (ctx.uppercase().contains("WEBVIEW") && !ctx.contains("stetho")) {
                driver.context(ctx)
                println("Switched to context: $ctx")
                return
            }
        }
        println("No WEBVIEW context found — staying in NATIVE_APP. Contexts: $contexts")
    }

    /** Switch back to native app context. */
    protected fun switchToNative() = driver.context("NATIVE_APP")

    /** Read test data: system property → env variable → default. */
    protected fun testData(key: String, default: String): String =
        System.getProperty(key) ?: System.getenv(key) ?: default

    private fun ltOptions(username: String, accessKey: String, method: Method, device: String) =
        hashMapOf<String, Any>(
            "username"             to username,
            "accessKey"            to accessKey,
            "isRealMobile"         to true,
            "build"                to "DG Android - UIAutomator2 Order Flow (Kotlin)",
            "name"                 to "${method.name} | $device",
            "project"              to "DGAPP-Project",
            "devicelog"            to true,
            "video"                to true,
            "visual"               to true,
            "autoGrantPermissions" to true,
            "plugin"               to "git-testng"
        )

    private fun envOrDefault(key: String, fallback: String) =
        System.getenv(key)?.takeIf { it.isNotEmpty() } ?: fallback
}
