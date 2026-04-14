package com.lambdatest;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestNGWebAuthnMobile {

    private RemoteWebDriver driver;
    private String Status = "failed";

    @BeforeMethod
    public void setup(Method m, ITestContext ctx) throws MalformedURLException {
        String username = System.getenv("LT_USERNAME") == null ? "Your LT Username" : System.getenv("LT_USERNAME");
        String authkey = System.getenv("LT_ACCESS_KEY") == null ? "Your LT AccessKey" : System.getenv("LT_ACCESS_KEY");

        String hub = "@mobile-hub.lambdatest.com/wd/hub";

        // LambdaTest real device capabilities
        HashMap<String, Object> ltOptions = new HashMap<>();
        ltOptions.put("browserName", "Chrome");
        ltOptions.put("deviceName", "Galaxy S21");
        ltOptions.put("platformVersion", "11");
        ltOptions.put("isRealMobile", true);
        ltOptions.put("build", "WebAuthn - Real Device Test");
        ltOptions.put("name", m.getName() + " - " + this.getClass().getSimpleName());
        ltOptions.put("plugin", "git-testng");
        ltOptions.put("tags", new String[] { "WebAuthn", "RealDevice", "Mobile" });
        ltOptions.put("w3c", true);
        ltOptions.put("selenium_version", "4.0.0"); // older version hint to avoid extendedDebuging default

        // Use ImmutableCapabilities to send only what we define — no Selenium defaults
        // that could trigger LambdaTest's extendedDebuging flag on real devices
        ImmutableCapabilities capabilities = new ImmutableCapabilities(Map.of(
            "browserName", "Chrome",
            "platformName", "Android",
            "lt:options", ltOptions
        ));

        driver = new RemoteWebDriver(new URL("https://" + username + ":" + authkey + hub), capabilities);
    }

    @Test
    public void webAuthnTest() throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

        System.out.println("Navigating to webauthn.io...");
        driver.get("https://webauthn.io/");

        // Wait for page to load — title is "WebAuthn.io"
        wait.until(ExpectedConditions.titleContains("WebAuthn"));
        System.out.println("Page title: " + driver.getTitle());
        Assert.assertTrue(driver.getTitle().contains("WebAuthn"), "Page title should contain 'WebAuthn'");

        // Wait for the username input field
        WebElement usernameInput = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector("input"))
        );
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", usernameInput);
        Thread.sleep(500);

        System.out.println("Typing test username...");
        usernameInput.click();
        usernameInput.sendKeys("testuser_lambdatest");
        System.out.println("Username entered: testuser_lambdatest");

        // Verify the Register button is visible
        WebElement btn = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button"))
        );
        Assert.assertTrue(btn.isDisplayed(), "Register button should be visible");
        System.out.println("Button found: " + btn.getText());

        Thread.sleep(1000);
        Status = "passed";
        System.out.println("WebAuthn page test completed successfully.");
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
