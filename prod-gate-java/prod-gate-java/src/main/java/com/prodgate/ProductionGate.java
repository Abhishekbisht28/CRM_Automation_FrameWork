package com.prodgate;

import com.prodgate.checker.ApiPerformanceChecker;
import com.prodgate.config.GateConfig;
import com.prodgate.model.GateResult;
import com.prodgate.reporter.EmailReporter;
import com.prodgate.reporter.ConsoleReporter;
import com.prodgate.scanner.SecurityScanner;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Production Gate — Entry point.
 *
 * Usage:
 *   java -jar prod-gate.jar --config gate.properties --code-path /path/to/repo
 *
 * Exit codes:
 *   0 = PASSED  (CI/CD allows merge)
 *   1 = BLOCKED (CI/CD blocks merge)
 */
@Command(
    name = "prod-gate",
    description = "Pre-merge security and performance gate for production deployments",
    mixinStandardHelpOptions = true,
    version = "1.0.0"
)
public class ProductionGate implements Callable<Integer> {

    @Option(names = {"--config", "-c"},
            description = "Path to gate.properties config file",
            defaultValue = "config/gate.properties")
    private String configPath;

    @Option(names = {"--code-path", "-p"},
            description = "Root path of the code to scan",
            defaultValue = ".")
    private String codePath;

    @Option(names = {"--apis", "-a"},
            description = "Path to apis.json file",
            defaultValue = "config/apis.json")
    private String apisPath;

    @Option(names = {"--baseline", "-b"},
            description = "Path to baseline JSON file",
            defaultValue = "baseline/api_baselines.json")
    private String baselinePath;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ProductionGate()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        // ── Load configuration ────────────────────────────────
        GateConfig config = GateConfig.load(configPath);
        config.setCodePath(Paths.get(codePath).toAbsolutePath());
        config.setApisPath(Paths.get(apisPath).toAbsolutePath());
        config.setBaselinePath(Paths.get(baselinePath).toAbsolutePath());

        ConsoleReporter console = new ConsoleReporter();
        console.printBanner(config);

        GateResult result = new GateResult();

        // ── Phase 1: Security Scan ────────────────────────────
        SecurityScanner securityScanner = new SecurityScanner(config, console);
        securityScanner.scan(result);

        // ── Phase 2: API Performance Check ───────────────────
        ApiPerformanceChecker apiChecker = new ApiPerformanceChecker(config, console);
        apiChecker.check(result);

        // ── Final verdict ─────────────────────────────────────
        console.printVerdict(result);

        // ── Send email if gate blocked ────────────────────────
        if (!result.isPassed()) {
            EmailReporter emailReporter = new EmailReporter(config);
            emailReporter.send(result);
        }

        return result.isPassed() ? 0 : 1;
    }
}
