# ZAP Security Testing Framework

[![Security scan](https://github.com/samramkumarqa/zap-security-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/samramkumarqa/zap-security-framework/actions/workflows/ci.yml)

Selenium functional tests routed through an [OWASP ZAP](https://www.zaproxy.org/) proxy, so a DAST security scan runs
alongside the functional run. The browsing feeds ZAP's **passive** scanner; a **spider** and **active** scan then probe
the app, and the build is **gated on risk** — any alert at or above a configurable level fails it.

**Stack:** Java 17 · JUnit 5 · Selenium 4 · OWASP ZAP (`zap-clientapi`) · AssertJ · Docker Compose · GitHub Actions

> **Scope and ethics.** The spider and active scanner send real (attack) traffic, so they only ever run against
> `target.url`. This project targets [OWASP Juice Shop](https://owasp.org/www-project-juice-shop/) — a deliberately
> vulnerable app, published for exactly this purpose — running on your own machine. **Only ever point it at a system you
> own or are explicitly authorised to test.**

## How it works

```
Selenium (Chrome) ──proxy──> OWASP ZAP ──> Target app (Juice Shop)
                               │
          passive scan ◄───────┘  (records all proxied traffic)
          spider          ───────> crawls for more endpoints
          active scan     ───────> probes for vulnerabilities
          alerts + HTML report ──> build fails if risk ≥ threshold
```

The suite is one ordered flow (`SecurityScanTest`):

| Order | Step | What it does |
|-------|------|--------------|
| 1 | Browse through proxy | Selenium drives Juice Shop (catalogue, search, login, add to basket); ZAP passively scans the traffic |
| 2 | Passive scan | Waits for ZAP's passive queue to drain |
| 3 | Spider | Crawls the target for endpoints the browser didn't reach |
| 4 | Active scan | Runs ZAP's active scanner against the target |
| 5 | Report + gate | Writes ZAP's HTML report, then fails the build on any alert at or above `fail.on.risk` |

## Running

**One command (Docker):** starts Juice Shop + ZAP, scans, tears down.

```bash
scripts/run-scan.sh
scripts/run-scan.sh -Dfail.on.risk=MEDIUM      # stricter gate
```

Needs Docker, a JDK 17+, Maven and Chrome. The script publishes ZAP on `localhost:8080`, and ZAP reaches the app as
`http://juice-shop:3000`; the browser only ever talks to the proxy, so nothing on the host has to resolve that name.

**Against a ZAP you already run** (desktop or `zap.sh -daemon`) with Juice Shop on `localhost:3000`:

```bash
mvn test                                        # uses config.properties defaults
mvn test -Dtarget.url=http://localhost:3000 -Dzap.port=8080 -Dheadless=false
```

The report lands at `target/zap-report/zap-report.html`.

## Configuration

Values resolve `-Dkey=value` → environment variable (`target.url` → `TARGET_URL`) → `config.properties`.

| Key | Default | Notes |
|-----|---------|-------|
| `target.url` | `http://localhost:3000` | App under test. **Authorised targets only.** |
| `zap.host` / `zap.port` | `localhost` / `8080` | ZAP daemon |
| `zap.api.key` | *(unset)* | Set `ZAP_API_KEY` for a key-protected daemon; never commit it |
| `fail.on.risk` | `HIGH` | Gate: `INFORMATIONAL`, `LOW`, `MEDIUM`, `HIGH` |
| `ascan.timeout.minutes` | `20` | Cap on the active scan |
| `headless` | `true` | |
| `app.user.email` / `app.user.password` | Juice Shop demo account | Real apps: use env vars / CI secrets |

## CI

`.github/workflows/ci.yml` runs the whole thing on every push and PR via `scripts/run-scan.sh`, and uploads the ZAP HTML
report as a build artifact. The "Run workflow" button takes a `fail_on_risk` input so you can tighten or loosen the gate
for a manual run.

## Notes on the rework

The original was a single TestNG method that proxied a live public site through a hard-coded local ZAP install, with
`System.setProperty` driver paths and no assertions — the scan produced no pass/fail signal. This version:

- targets a **legal, self-hosted** vulnerable app instead of public sites;
- orchestrates **passive + spider + active** scans through ZAP's REST API rather than relying on manual proxying;
- turns the scan into a **build gate** with an HTML report, so findings actually fail CI;
- runs **reproducibly in Docker** with no local ZAP install or driver binaries (Selenium Manager handles the driver);
- keeps **no secrets or machine-specific paths** in the repo.
