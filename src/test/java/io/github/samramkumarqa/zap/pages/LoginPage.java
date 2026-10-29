package io.github.samramkumarqa.zap.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Juice Shop account login. */
public class LoginPage extends BasePage {

    private static final By ACCOUNT_MENU = By.id("navbarAccount");
    private static final By LOGIN_NAV = By.id("navbarLoginButton");
    private static final By EMAIL = By.id("email");
    private static final By PASSWORD = By.id("password");
    private static final By SUBMIT = By.id("loginButton");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void goToLogin() {
        click(ACCOUNT_MENU);
        click(LOGIN_NAV);
        visible(EMAIL);
    }

    public void login(String email, String password) {
        type(EMAIL, email);
        type(PASSWORD, password);
        click(SUBMIT);
        // After a successful login the account menu exposes a logout entry
        wait.until(ExpectedConditions.presenceOfElementLocated(ACCOUNT_MENU));
    }

    public boolean isLoggedIn() {
        click(ACCOUNT_MENU);
        boolean loggedIn = !driver.findElements(By.id("navbarLogoutButton")).isEmpty();
        // close the menu again
        driver.findElement(By.tagName("body")).click();
        return loggedIn;
    }
}
