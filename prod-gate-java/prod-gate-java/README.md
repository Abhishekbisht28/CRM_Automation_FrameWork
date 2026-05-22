# 🛡️ Production Gate — Java

A Java 17 pre-merge automation that blocks code from reaching production if it contains security vulnerabilities or API performance regressions.

## Build

 ```bash
mvn package -DskipTests
# Output: target/prod-gate.jar  (fat JAR, ~10 MB, no external deps)
```

## Quick Start

```bash
# 1. Configure
cp config/gate.properties.example config/gate.properties
# Edit: email settings, SMTP, thresholds

# 2. Define APIs
nano config/apis.json

# 3. Run
java -jar target/prod-gate.jar \
  --config   config/gate.properties \
  --code-path /path/to/your/repo \
  --apis     config/apis.json \
  --baseline baseline/api_baselines.json

# Exit 0 = PASSED, Exit 1 = BLOCKED
```

## CLI Options

| Flag | Default | Description |
|---|---|---|
| `--config` | `config/gate.properties` | Path to config file |
| `--code-path` | `.` | Root of the code to scan |
| `--apis` | `config/apis.json` | API list |
| `--baseline` | `baseline/api_baselines.json` | Baseline store |

## What It Checks

### Phase 1 — Security
- **npm audit** — critical/high dependency vulnerabilities
- **Secret leaks** — AWS keys, GitHub PATs, OpenAI keys, hardcoded passwords/tokens
- **SQL injection patterns** — string-concatenated queries in Java, JS, Python
- **Dangerous calls** — `eval()`, `Runtime.exec()` with variables

### Phase 2 — API Performance
- Every API in `apis.json` is measured via HTTP
- **Hard limit:** any API > 600 ms is flagged as slow
- **Count gate:** if 3+ APIs are slow → merge blocked
- **Regression guard:** if an API was 500 ms last run and is now 551 ms (+10%) → merge blocked
- **Email:** HTML + plain-text report sent on any failure

## Configuration

### gate.properties

```properties
api.hard.limit.ms=600
api.max.slow.count=3
api.regression.pct=10

email.enabled=true
email.to=devops@yourcompany.com
email.from=prodgate@yourcompany.com

smtp.host=smtp.yourcompany.com
smtp.port=587
smtp.tls=true
# smtp.user / smtp.password → use env vars SMTP_USER, SMTP_PASSWORD
```

### apis.json

```json
[
  {
    "name": "health_check",
    "method": "GET",
    "url": "https://api.yourapp.com/health",
    "headers": { "Authorization": "Bearer TOKEN" }
  },
  {
    "name": "create_order",
    "method": "POST",
    "url": "https://api.yourapp.com/v1/orders",
    "headers": { "Content-Type": "application/json" },
    "body": "{\"product_id\": \"test-001\"}"
  }
]
```

### Baseline store

`baseline/api_baselines.json` is auto-created on first run. **Commit it or cache it in CI** so baselines persist between pipeline runs.

```json
{
  "health_check": {
    "baseline_ms": 45,
    "last_ms": 48,
    "last_updated": "2024-03-15T14:00:00Z"
  }
}
```

### Reset a baseline (after intentional optimisation)

```bash
# Edit baseline/api_baselines.json and delete the entry for that API,
# or delete the whole file to reset all baselines.
```

## CI/CD Integration

### GitHub Actions
Copy `ci/github-actions.yml` to `.github/workflows/prod-gate.yml`.

Secrets needed: `SMTP_USER`, `SMTP_PASSWORD`, `STAGING_API_TOKEN`
Variables needed: `EMAIL_TO`, `EMAIL_FROM`, `SMTP_HOST`

### GitLab CI
```yaml
include:
  - project: 'your-org/prod-gate-java'
    file: 'ci/gitlab-ci.yml'
```

### Jenkins
```groovy
stage('Production Gate') {
    steps {
        sh 'java -jar prod-gate/target/prod-gate.jar --config prod-gate/config/gate.properties'
    }
}
```

## Project Structure

```
prod-gate-java/
├── pom.xml
├── config/
│   ├── gate.properties.example   ← copy → gate.properties
│   └── apis.json                 ← define your endpoints here
├── baseline/
│   └── api_baselines.json        ← auto-generated; cache in CI
├── ci/
│   └── github-actions.yml
└── src/main/java/com/prodgate/
    ├── ProductionGate.java        ← entry point (CLI)
    ├── config/GateConfig.java     ← loads gate.properties
    ├── model/
    │   ├── GateResult.java        ← accumulates phase results
    │   ├── ApiResultRecord.java   ← single API measurement
    │   └── SecurityFindingView.java
    ├── scanner/SecurityScanner.java  ← Phase 1
    ├── checker/
    │   ├── ApiPerformanceChecker.java ← Phase 2
    │   └── BaselineStore.java         ← JSON baseline persistence
    └── reporter/
        ├── ConsoleReporter.java   ← coloured terminal output
        └── EmailReporter.java     ← HTML + plain-text email
```
