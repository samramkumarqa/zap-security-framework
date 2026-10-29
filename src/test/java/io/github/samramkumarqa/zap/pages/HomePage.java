package io.github.samramkumarqa.zap.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;

/** Juice Shop landing page: the product catalogue, search and the welcome/cookie overlays. */
public class HomePage extends BasePage {

    private static final By WELCOME_CLOSE = By.id("dismiss-welcome-banner");
    private static final By COOKIE_DISMISS = By.cssSelector("a.cc-btn.cc-dismiss");
    private static final By SEARCH_ICON = By.id("searchQuery");
    private static final By SEARCH_INPUT = By.cssSelector("#searchQuery input, input#searchQuery, #mat-input-0");
    private static final By PRODUCT = By.cssSelector("mat-grid-tile .mat-card, .mat-grid-tile");
    private static final By ADD_TO_BASKET = By.cssSelector("button[aria-label='Add to Basket']");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public HomePage open(String baseUrl) {
        driver.get(baseUrl + "/#/");
        dismissOverlays();
        return this;
    }

    /** The welcome dialog and cookie banner sit on top of everything until closed. */
    public HomePage dismissOverlays() {
        clickIfPresent(WELCOME_CLOSE);
        clickIfPresent(COOKIE_DISMISS);
        return this;
    }

    public HomePage search(String term) {
        clickIfPresent(SEARCH_ICON);
        type(SEARCH_INPUT, term + Keys.ENTER);
        return this;
    }

    public boolean hasProducts() {
        return !driver.findElements(PRODUCT).isEmpty();
    }

    /** Needs a logged-in session; adds the first catalogue item to the basket, generating authenticated traffic. */
    public HomePage addFirstProductToBasket() {
        visible(PRODUCT);
        click(ADD_TO_BASKET);
        return this;
    }
}
