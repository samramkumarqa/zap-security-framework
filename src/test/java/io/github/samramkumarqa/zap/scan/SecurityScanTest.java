package io.github.samramkumarqa.zap.scan;

import io.github.samramkumarqa.zap.pages.HomePage;
import io.github.samramkumarqa.zap.pages.LoginPage;
import io.github.samramkumarqa.zap.support.Alert;
import io.github.samramkumarqa.zap.support.Config;
import io.github.samramkumarqa.zap.support.DriverFactory;
import io.github.samramkumarqa.zap.support.Risk;
import io.github.samramkumarqa.zap.support.ZapService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The security pipeline, top to bottom:
 * <ol>
 *   <li>drive the target with Selenium through the ZAP proxy (passive scan records the traffic),</li>
 *   <li>let the passive scanner finish,</li>
 *   <li>spider the target to find pages the browser didn't,</li>
 *   <li>run the active scanner,</li>
 *   <li>write ZAP's HTML report and fail the build if any alert is at or above the configured risk.</li>
 * </ol>
 *
 * <p>The ZAP daemon and the target app run outside the suite (see the README / scripts). The scanners only ever
 * touch {@code target.url} — an app you control and are authorised to test, OWASP Juice Shop by default.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("ZAP security scan of the target application")
class SecurityScanTest {

    private static final Path REPORT = Path.of("target", "zap-report", "zap-report.html");

    private ZapService zap;
    private WebDriver driver;
    private String target;
    private Risk gate;

    @BeforeAll
    void setUp() {
        target = Config.targetUrl();
        gate = Risk.valueOf(Config.get("fail.on.risk", "HIGH").toUpperCase());

        zap = new ZapService(
                Config.get("zap.host", "localhost"),
                Integer.parseInt(Config.get("zap.port", "8080")),
                Config.getOrNull("zap.api.key"));
        zap.waitUntilReady(60);

        driver = DriverFactory.startProxiedThrough(
                Config.get("zap.host", "localhost"),
                Integer.parseInt(Config.get("zap.port", "8080")));
    }

    @Test
    @Order(1)
    @DisplayName("Browse the app so ZAP sees real traffic (passive scan)")
    void browseThroughProxy() {
        HomePage home = new HomePage(driver).open(target);
        assertThat(home.hasProducts()).as("product catalogue is shown").isTrue();

        home.search("apple");

        // A logged-in session exercises authenticated endpoints too. These are Juice Shop's documented demo
        // credentials; real credentials would come from environment variables, never the repo.
        String email = Config.getOrNull("app.user.email");
        String password = Config.getOrNull("app.user.password");
        if (email != null && password != null) {
            LoginPage login = new LoginPage(driver);
            login.goToLogin();
            login.login(email, password);
            new HomePage(driver).open(target).addFirstProductToBasket();
        }
    }

    @Test
    @Order(2)
    @DisplayName("Passive scanner drains its queue")
    void passiveScanCompletes() {
        zap.waitForPassiveScan(Duration.ofMinutes(3));
    }

    @Test
    @Order(3)
    @DisplayName("Spider discovers the rest of the site")
    void spider() {
        zap.spider(target, Duration.ofMinutes(5));
        zap.waitForPassiveScan(Duration.ofMinutes(3));
    }

    @Test
    @Order(4)
    @DisplayName("Active scanner probes for vulnerabilities")
    void activeScan() {
        zap.activeScan(target, Duration.ofMinutes(Integer.parseInt(Config.get("ascan.timeout.minutes", "20"))));
    }

    @Test
    @Order(5)
    @DisplayName("Report the findings and gate the build on risk")
    void reportAndGate() {
        zap.writeHtmlReport(REPORT);

        List<Alert> alerts = zap.alerts(target);
        Map<Risk, Long> byRisk = alerts.stream()
                .collect(Collectors.groupingBy(Alert::risk, Collectors.counting()));
        System.out.printf("%nZAP found %d alerts: High=%d Medium=%d Low=%d Informational=%d%n",
                alerts.size(),
                byRisk.getOrDefault(Risk.HIGH, 0L), byRisk.getOrDefault(Risk.MEDIUM, 0L),
                byRisk.getOrDefault(Risk.LOW, 0L), byRisk.getOrDefault(Risk.INFORMATIONAL, 0L));
        System.out.println("HTML report: " + REPORT.toAbsolutePath());

        List<Alert> blocking = alerts.stream()
                .filter(a -> a.risk().ordinal() >= gate.ordinal())
                .sorted(Comparator.comparingInt((Alert a) -> a.risk().ordinal()).reversed())
                .toList();

        String detail = blocking.stream().map(Alert::toString).collect(Collectors.joining("\n  ", "\n  ", ""));
        assertThat(blocking)
                .as("alerts at or above %s. Full report at %s:%s", gate, REPORT, detail)
                .isEmpty();
    }

    @AfterAll
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
