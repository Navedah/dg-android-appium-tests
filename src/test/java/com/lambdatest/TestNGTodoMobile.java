package com.lambdatest;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestNGTodoMobile {

    private RemoteWebDriver driver;
    private String Status = "failed";

    @BeforeMethod
    public void setup(Method m, ITestContext ctx) throws MalformedURLException {
        String username = System.getenv("LT_USERNAME") == null ? "Your LT Username" : System.getenv("LT_USERNAME");
        String authkey = System.getenv("LT_ACCESS_KEY") == null ? "Your LT AccessKey" : System.getenv("LT_ACCESS_KEY");

        String hub = "@mobile-hub.lambdatest.com/wd/hub";

        // LambdaTest-specific caps nested under lt:options (W3C compliant for Selenium 4)
        HashMap<String, Object> ltOptions = new HashMap<>();
        ltOptions.put("browserName", "Chrome");
        ltOptions.put("deviceName", "Pixel 4");
        ltOptions.put("platformVersion", "11");
        ltOptions.put("isRealMobile", false);
        ltOptions.put("build", "TestNG With Java - Mobile");
        ltOptions.put("name", m.getName() + " - " + this.getClass().getSimpleName());
        ltOptions.put("plugin", "git-testng");
        ltOptions.put("tags", new String[] { "Feature", "Tag", "Mobile" });
        ltOptions.put("w3c", true);

        MutableCapabilities capabilities = new MutableCapabilities();
        capabilities.setCapability("browserName", "Chrome");
        capabilities.setCapability("platformName", "Android");
        capabilities.setCapability("lt:options", ltOptions);

        driver = new RemoteWebDriver(new URL("https://" + username + ":" + authkey + hub), capabilities);
    }

    @Test
    public void basicTest() throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        System.out.println("Loading URL...");
        driver.get("https://lambdatest.github.io/sample-todo-app/");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("li1")));

        System.out.println("Checking Boxes...");
        driver.findElement(By.name("li1")).click();
        driver.findElement(By.name("li2")).click();
        driver.findElement(By.name("li3")).click();
        driver.findElement(By.name("li4")).click();

        System.out.println("Adding New Items...");
        addItem(" List Item 6");
        addItem(" List Item 7");
        addItem(" List Item 8");

        // Wait until all 8 items are present in the list
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.xpath("//li"), 7));

        System.out.println("Rechecking Boxes...");
        clickCheckbox(wait, 1);
        clickCheckbox(wait, 3);
        clickCheckbox(wait, 7);
        clickCheckbox(wait, 8);

        System.out.println("Adding Final Todo Item...");
        addItem("Get Taste of Lambda and Stick to It");

        // Wait for the 9th item and click its checkbox
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.xpath("//li"), 8));
        clickCheckbox(wait, 9);

        String spanText = driver.findElement(By.xpath("//li[9]/span")).getText();
        Assert.assertEquals(spanText, "Get Taste of Lambda and Stick to It");

        Status = "passed";
        Thread.sleep(500);
        System.out.println("Test Completed Successfully");
    }

    private void addItem(String text) throws InterruptedException {
        WebElement input = driver.findElement(By.id("sampletodotext"));
        input.click();
        Thread.sleep(300);
        input.sendKeys(text);
        // On mobile Chrome, press ENTER to submit since button clicks may be unreliable
        input.sendKeys(Keys.RETURN);
        Thread.sleep(800);
    }

    private void clickCheckbox(WebDriverWait wait, int position) {
        // Try by name attribute first, fall back to XPath position
        String name = "li" + position;
        try {
            WebElement el = wait.until(ExpectedConditions.elementToBeClickable(By.name(name)));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", el);
            el.click();
        } catch (Exception e) {
            WebElement el = wait.until(
                ExpectedConditions.elementToBeClickable(By.xpath("(//li//input[@type='checkbox'])[" + position + "]"))
            );
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", el);
            el.click();
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
