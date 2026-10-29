package io.github.samramkumarqa.zap.support;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

/**
 * Chrome routed through the ZAP proxy, so every request the functional tests make is recorded and passively scanned.
 * Selenium Manager resolves the driver, so no binaries are committed.
 */
public final class DriverFactory {

    private DriverFactory() {
    }

    public static WebDriver startProxiedThrough(String zapHost, int zapPort) {
        String zapProxy = zapHost + ":" + zapPort;
        Proxy proxy = new Proxy();
        proxy.setHttpProxy(zapProxy);
        proxy.setSslProxy(zapProxy);

        ChromeOptions options = new ChromeOptions();
        options.setProxy(proxy);
        // Juice Shop is served over https with ZAP's dynamic root CA in front; the browser must accept it
        options.setAcceptInsecureCerts(true);
        options.addArguments("--disable-notifications", "--disable-search-engine-choice-screen");
        if (Config.getBool("headless")) {
            options.addArguments("--headless=new");
        }

        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().setSize(new Dimension(1440, 900));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(Config.getInt("timeout.seconds")));
        return driver;
    }
}
