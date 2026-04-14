package com.lambdatest.dg.tests;

import com.lambdatest.dg.base.DGBaseTest;
import com.lambdatest.dg.pages.*;
import org.testng.annotations.Test;

/**
 * End-to-end order flow POC for the Dollar General Android app on LambdaTest.
 *
 * Prerequisites:
 *   1. Upload the DG APK to LambdaTest and set the returned lt:// URL:
 *        export LT_APP_URL=lt://YOUR_APP_ID
 *        (or pass -DappUrl=lt://YOUR_APP_ID to Maven)
 *   2. Set credentials:
 *        export LT_USERNAME=your_lt_username
 *        export LT_ACCESS_KEY=your_lt_access_key
 *   3. Run:
 *        mvn test -Dsuite=dg-android.xml
 *
 * No test APK is required — Appium drives the app via UIAutomator2 remotely.
 */
public class DGOrderFlowTest extends DGBaseTest {

    // Test data — override via system properties or env variables
    private String email()    { return testData("loginEmail",    "dollarnewcloud67@mailinator.com"); }
    private String password() { return testData("loginPassword", "Test@123"); }
    private String keyword()  { return testData("searchKeyword", "snickers"); }
    private String zip()      { return testData("zipCode",       "30301"); }

    // ── TC-01: Login ───────────────────────────────────────────────────────────

    @Test(description = "TC-01: Verify user can log into the DG app")
    public void loginToDGApp() {
        SignInPage signIn = new SignInPage(driver);
        HomePage   home   = new HomePage(driver);

        signIn.loginWith(email(), password());
        home.verifyLoaded();

        System.out.println("TC-01 PASSED: Login successful");
    }

    // ── TC-02: Add product to cart ─────────────────────────────────────────────

    @Test(description = "TC-02: Search for a product and add it to the cart")
    public void addProductToCart() {
        SignInPage      signIn = new SignInPage(driver);
        HomePage        home   = new HomePage(driver);
        SearchPage      search = new SearchPage(driver);
        ProductDetailPage pdp  = new ProductDetailPage(driver);
        CartPage        cart   = new CartPage(driver);

        signIn.loginWith(email(), password());
        home.verifyLoaded();

        home.tapSearch();
        search.searchFor(keyword());
        search.verifyResultsLoaded();
        search.tapFirstProduct();

        pdp.verifyLoaded();
        pdp.addToCartAndViewCart();

        cart.verifyHasItems();
        System.out.println("TC-02 PASSED: Cart total = " + cart.getOrderTotal());
    }

    // ── TC-03: Place order ─────────────────────────────────────────────────────

    @Test(description = "TC-03: Complete store-pickup order end-to-end")
    public void placeOrder() {
        SignInPage      signIn   = new SignInPage(driver);
        HomePage        home     = new HomePage(driver);
        SearchPage      search   = new SearchPage(driver);
        ProductDetailPage pdp    = new ProductDetailPage(driver);
        CartPage        cart     = new CartPage(driver);
        CheckoutPage    checkout = new CheckoutPage(driver);

        // Login
        signIn.loginWith(email(), password());
        home.verifyLoaded();

        // Add product
        home.tapSearch();
        search.searchFor(keyword());
        search.verifyResultsLoaded();
        search.tapFirstProduct();
        pdp.verifyLoaded();
        pdp.addToCartAndViewCart();
        cart.verifyHasItems();

        // Checkout — delivery flow (confirmed from page source dumps)
        cart.tapProceedToCheckout();        // dismisses "missed deals" dialog internally
        checkout.selectDeliveryAsap();      // delivery time screen → Continue
        checkout.tapPlaceOrder();           // order review screen → Place order

        String orderNumber = checkout.verifyOrderConfirmationAndGetOrderNumber();
        System.out.println("TC-03 PASSED: Order placed — order number = " + orderNumber);
    }

    // ── TC-04: Full E2E smoke ──────────────────────────────────────────────────

    @Test(description = "TC-04: Full E2E smoke — login → search → PDP → cart → checkout → confirm")
    public void fullOrderFlowE2E() {
        SignInPage      signIn   = new SignInPage(driver);
        HomePage        home     = new HomePage(driver);
        SearchPage      search   = new SearchPage(driver);
        ProductDetailPage pdp    = new ProductDetailPage(driver);
        CartPage        cart     = new CartPage(driver);
        CheckoutPage    checkout = new CheckoutPage(driver);

        // ① Login
        signIn.loginWith(email(), password());
        home.verifyLoaded();
        int initialCount = home.getCartCount();
        System.out.println("Initial cart count: " + initialCount);

        // ② Search & open product
        home.tapSearch();
        search.searchFor(keyword());
        search.verifyResultsLoaded();
        search.tapFirstProduct();
        pdp.verifyLoaded();

        // ③ Add to cart
        pdp.addToCartAndViewCart();
        cart.verifyHasItems();
        String total = cart.getOrderTotal();
        System.out.println("Order total: " + total);

        // ④ Checkout → place order (delivery flow)
        cart.tapProceedToCheckout();        // dismisses "missed deals" dialog internally
        checkout.selectDeliveryAsap();      // delivery time screen → Continue
        checkout.tapPlaceOrder();           // order review screen → Place order

        String orderNumber = checkout.verifyOrderConfirmationAndGetOrderNumber();
        System.out.println("TC-04 PASSED: E2E complete — order " + orderNumber + " confirmed");
    }
}
