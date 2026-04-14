package com.lambdatest.dg.kotlin.tests

import com.lambdatest.dg.kotlin.base.DGBaseTest
import com.lambdatest.dg.kotlin.pages.HomePage
import com.lambdatest.dg.kotlin.pages.SignInPage
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.testng.annotations.Test
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Dumps page source XML and screenshot after login so real resource IDs can be identified.
 *
 * Run once:
 *   mvn test -Dsuite=dg-diagnostic-kotlin.xml
 */
class DGDiagnosticTest : DGBaseTest() {

    @Test(description = "Login then dump home screen to identify real element IDs")
    fun dumpAppState() {
        SignInPage(driver).loginWith("dollarnewcloud67@mailinator.com", "Test@123")

        Thread.sleep(12000)
        HomePage(driver).dismissPromoIfPresent()
        Thread.sleep(3000)

        val src = driver.pageSource
        if (src.contains("id/webView") && !src.contains("id/home_search_view")) {
            println("WebView interstitial detected, pressing back...")
            driver.navigate().back()
            Thread.sleep(4000)
        }

        val pageSource = driver.pageSource
        val xmlPath = "target/dg-page-source.xml"
        Files.createDirectories(Paths.get("target"))
        FileWriter(xmlPath).use { it.write(pageSource) }
        println("✅  Page source written to: $xmlPath")

        val screenshot = driver.getScreenshotAs(OutputType.BYTES)
        Files.write(Paths.get("target/dg-screenshot.png"), screenshot)
        println("✅  Screenshot saved to: target/dg-screenshot.png")

        println("\n── Resource IDs visible on screen ───────────────────────────────")
        pageSource.split("\n")
            .filter { it.contains("resource-id") && !it.contains("resource-id=\"\"") }
            .mapNotNull { line ->
                val id = line.replace(Regex(".*resource-id=\"([^\"]+)\".*"), "$1").trim()
                id.takeIf { it != line.trim() }
            }
            .distinct()
            .forEach { println("  $it") }
        println("──────────────────────────────────────────────────────────────────")

        println("\n── WebView context switch ────────────────────────────────────────")
        val contexts = driver.contextHandles
        println("All contexts: $contexts")
        for (ctx in contexts) {
            if (ctx.uppercase().contains("WEBVIEW") && !ctx.contains("stetho")) {
                driver.context(ctx)
                val html = (driver as JavascriptExecutor)
                    .executeScript("return document.documentElement.outerHTML") as String
                FileWriter("target/dg-webview.html").use { it.write(html) }
                println("✅  WebView HTML saved to: target/dg-webview.html")
                break
            }
        }
        println("──────────────────────────────────────────────────────────────────")
    }
}
