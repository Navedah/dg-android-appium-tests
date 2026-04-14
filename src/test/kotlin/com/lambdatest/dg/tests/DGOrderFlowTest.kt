package com.lambdatest.dg.kotlin.tests

import com.lambdatest.dg.kotlin.base.DGBaseTest
import com.lambdatest.dg.kotlin.pages.*
import org.testng.annotations.Test

/**
 * Kotlin end-to-end order flow tests for the Dollar General Android app on LambdaTest.
 * Uses Appium + UIAutomator2 engine (no test APK required).
 *
 * Run with:
 *   mvn test -Dsuite=dg-android-kotlin.xml
 */
class DGOrderFlowTest : DGBaseTest() {

    private fun email()    = testData("loginEmail",    "dollarnewcloud67@mailinator.com")
    private fun password() = testData("loginPassword", "Test@123")
    private fun keyword()  = testData("searchKeyword", "snickers")
    private fun zip()      = testData("zipCode",       "30301")

    // ── TC-01: Login ──────────────────────────────────────────────────────────

    @Test(description = "TC-01: Verify user can log into the DG app")
    fun loginToDGApp() {
        SignInPage(driver).loginWith(email(), password())
        HomePage(driver).verifyLoaded()
        println("TC-01 PASSED: Login successful")
    }

    // ── TC-02: Add product to cart ────────────────────────────────────────────

    @Test(description = "TC-02: Search for a product and add it to the cart")
    fun addProductToCart() {
        SignInPage(driver).loginWith(email(), password())
        val home = HomePage(driver)
        home.verifyLoaded()
        home.tapSearch()

        val search = SearchPage(driver)
        search.searchFor(keyword())
        search.verifyResultsLoaded()
        search.tapFirstProduct()

        ProductDetailPage(driver).addToCartAndViewCart()

        val cart = CartPage(driver)
        cart.verifyHasItems()
        println("TC-02 PASSED: Cart total = ${cart.getOrderTotal()}")
    }

    // ── TC-03: Place order ────────────────────────────────────────────────────

    @Test(description = "TC-03: Complete store-pickup order end-to-end")
    fun placeOrder() {
        SignInPage(driver).loginWith(email(), password())
        val home = HomePage(driver)
        home.verifyLoaded()
        home.tapSearch()

        val search = SearchPage(driver)
        search.searchFor(keyword())
        search.verifyResultsLoaded()
        search.tapFirstProduct()

        ProductDetailPage(driver).addToCartAndViewCart()

        val cart = CartPage(driver)
        cart.verifyHasItems()
        // Checkout — delivery flow (confirmed from page source dumps)
        cart.tapProceedToCheckout()        // dismisses "missed deals" dialog internally
        val checkout = CheckoutPage(driver)
        checkout.selectDeliveryAsap()      // delivery time screen → Continue
        checkout.tapPlaceOrder()           // order review screen → Place order

        val orderNumber = checkout.verifyOrderConfirmationAndGetOrderNumber()
        println("TC-03 PASSED: Order placed — order number = $orderNumber")
    }

    // ── TC-04: Full E2E smoke ─────────────────────────────────────────────────

    @Test(description = "TC-04: Full E2E smoke — login → search → PDP → cart → checkout → confirm")
    fun fullOrderFlowE2E() {
        SignInPage(driver).loginWith(email(), password())
        val home = HomePage(driver)
        home.verifyLoaded()
        println("Initial cart count: ${home.getCartCount()}")

        home.tapSearch()
        val search = SearchPage(driver)
        search.searchFor(keyword())
        search.verifyResultsLoaded()
        search.tapFirstProduct()

        val pdp = ProductDetailPage(driver)
        pdp.verifyLoaded()
        pdp.addToCartAndViewCart()

        val cart = CartPage(driver)
        cart.verifyHasItems()
        println("Order total: ${cart.getOrderTotal()}")

        // Checkout — delivery flow
        cart.tapProceedToCheckout()        // dismisses "missed deals" dialog internally
        val checkout = CheckoutPage(driver)
        checkout.selectDeliveryAsap()      // delivery time screen → Continue
        checkout.tapPlaceOrder()           // order review screen → Place order

        val orderNumber = checkout.verifyOrderConfirmationAndGetOrderNumber()
        println("TC-04 PASSED: E2E complete — order $orderNumber confirmed")
    }
}
