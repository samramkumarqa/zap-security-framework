package io.github.samramkumarqa.zap.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Small wait helpers shared by the Juice Shop page objects. */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected void type(By locator, String text) {
        visible(locator).sendKeys(text);
    }

    /** Clicks something that may or may not be on the page (a banner, a cookie notice) without failing if it's absent. */
    protected void clickIfPresent(By locator) {
        try {
            WebDriverWait brief = new WebDriverWait(driver, Duration.ofSeconds(4));
            brief.until(ExpectedConditions.elementToBeClickable(locator)).click();
        } catch (TimeoutException | NoSuchElementException ignored) {
            // nothing to dismiss
        }
    }
}
