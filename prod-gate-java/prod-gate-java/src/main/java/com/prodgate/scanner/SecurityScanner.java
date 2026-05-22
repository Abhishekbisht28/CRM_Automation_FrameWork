package com.prodgate.scanner;

import com.prodgate.config.GateConfig;
import com.prodgate.model.GateResult;
import com.prodgate.reporter.ConsoleReporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Phase 1 — Security scan.
 *
 * Checks:
 *  1. npm audit  (if package.json found)
 *  2. Hardcoded secrets / credentials
 *  3. Raw SQL injection patterns
 *  4. Dangerous function calls (eval, exec with user input)
 */
public class SecurityScanner {

    private final GateConfig     config;
    private final ConsoleReporter console;

    // ── Secret patterns ───────────────────────────────────────
    private static final List<PatternDef> SECRET_PATTERNS = List.of(
        new PatternDef("CRITICAL", "AWS Access Key",        "AKIA[0-9A-Z]{16}"),
        new PatternDef("CRITICAL", "GitHub PAT",            "ghp_[a-zA-Z0-9]{36}"),
        new PatternDef("CRITICAL", "OpenAI API Key",        "sk-[a-zA-Z0-9]{40,}"),
        new PatternDef("CRITICAL", "RSA Private Key",       "-----BEGIN RSA PRIVATE KEY-----"),
        new PatternDef("CRITICAL", "OpenSSH Private Key",   "-----BEGIN OPENSSH PRIVATE KEY-----"),
        new PatternDef("HIGH",     "Hardcoded password",    "(?i)password\\s*=\\s*[\"'][^\"']{4,}[\"']"),
        new PatternDef("HIGH",     "Hardcoded API key",     "(?i)api[_-]?key\\s*=\\s*[\"'][^\"']{8,}[\"']"),
        new PatternDef("HIGH",     "Hardcoded secret",      "(?i)secret\\s*=\\s*[\"'][^\"']{8,}[\"']"),
        new PatternDef("HIGH",     "Hardcoded token",       "(?i)token\\s*=\\s*[\"'][^\"']{8,}[\"']"),
        new PatternDef("MEDIUM",   "Possible DB password",  "(?i)db[_-]?pass\\s*=\\s*[\"'][^\"']{4,}[\"']")
    );

    // ── SQL injection patterns ────────────────────────────────
    private static final List<PatternDef> SQL_PATTERNS = List.of(
        new PatternDef("HIGH", "String-concat SQL (Java)", "(?i)(createQuery|createNativeQuery|executeQuery|prepareStatement)\\s*\\(\\s*[\"'].*\\+"),
        new PatternDef("HIGH", "String-concat SQL (JS/TS)", "(?i)(query|execute)\\s*\\(\\s*[`'\"].*\\$\\{"),
        new PatternDef("HIGH", "String-concat SQL (Python)", "(?i)(execute|cursor\\.execute)\\s*\\(\\s*f['\"].*SELECT|UPDATE|DELETE|INSERT")
    );

    // ── Dangerous functions ───────────────────────────────────
    private static final List<PatternDef> DANGEROUS_PATTERNS = List.of(
        new PatternDef("HIGH",   "eval() usage",           "(?i)\\beval\\s*\\("),
        new PatternDef("HIGH",   "Runtime.exec() with var","(?i)Runtime\\.getRuntime\\(\\)\\.exec\\(\\s*[a-z]"),
        new PatternDef("MEDIUM", "System.exit() in code",  "(?i)System\\.exit\\s*\\("),
        new PatternDef("MEDIUM", "Insecure random",        "new Random\\(\\)")
    );

    // ── File extensions to scan ───────────────────────────────
    private static final Set<String> SCAN_EXTENSIONS = Set.of(
        ".java", ".js", ".ts", ".jsx", ".tsx",
        ".py", ".rb", ".go", ".php",
        ".env", ".yaml", ".yml", ".json",
        ".xml", ".properties", ".conf", ".sh"
    );

    // ── Directories to skip ───────────────────────────────────
    private static final Set<String> SKIP_DIRS = Set.of(
        "node_modules", ".git", "build", "dist", "target",
        ".gradle", "__pycache__", ".idea", ".vscode"
    );

    public SecurityScanner(GateConfig config, ConsoleReporter console) {
        this.config  = config;
        this.console = console;
    }

    public void scan(GateResult result) {
        console.section("PHASE 1 — Security Vulnerability Scan");

        int foundBefore = result.getVulnCount();

        // 1. npm audit
        runNpmAudit(result);

        // 2. Walk files
        try {
            Files.walkFileTree(config.getCodePath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (SKIP_DIRS.contains(dir.getFileName().toString()))
                        return FileVisitResult.SKIP_SUBTREE;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String name = file.getFileName().toString();
                    String ext  = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
                    if (SCAN_EXTENSIONS.contains(ext.toLowerCase())) {
                        scanFile(file, result);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            console.warn("File walk error: " + e.getMessage());
        }

        int found = result.getVulnCount() - foundBefore;
        if (found == 0) {
            console.ok("Security scan: all clear — no issues detected");
        } else {
            console.fail("Security scan: " + found + " issue(s) found");
            result.markSecurityFailed();
        }
    }

    // ── npm audit ─────────────────────────────────────────────
    private void runNpmAudit(GateResult result) {
        Path pkgJson = config.getCodePath().resolve("package.json");
        if (!Files.exists(pkgJson)) return;

        console.info("Running npm audit…");
        try {
            ProcessBuilder pb = new ProcessBuilder("npm", "audit", "--json");
            pb.directory(config.getCodePath().toFile());
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) out.append(line);
            }
            proc.waitFor();

            // Parse critical/high counts from JSON
            String json    = out.toString();
            int critical   = extractJsonInt(json, "\"critical\":");
            int high       = extractJsonInt(json, "\"high\":");

            if (critical > 0 || high > 0) {
                result.addSecurityFinding("CRITICAL", "npm audit",
                    critical + " critical, " + high + " high vulnerabilities in dependencies",
                    "package.json", 0);
                console.fail("npm audit: " + critical + " critical, " + high + " high");
            } else {
                console.ok("npm audit: clean");
            }
        } catch (Exception e) {
            console.warn("npm audit skipped: " + e.getMessage());
        }
    }

    // ── Per-file scan ─────────────────────────────────────────
    private void scanFile(Path file, GateResult result) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            return;   // skip unreadable files
        }

        String relPath = config.getCodePath().relativize(file).toString();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // Skip obvious comments / example files
            if (line.trim().startsWith("//") || line.trim().startsWith("#")
                    || relPath.contains(".example") || relPath.contains("test")) {
                continue;
            }

            checkPatterns(SECRET_PATTERNS,    line, relPath, i + 1, result);
            checkPatterns(SQL_PATTERNS,        line, relPath, i + 1, result);
            checkPatterns(DANGEROUS_PATTERNS,  line, relPath, i + 1, result);
        }
    }

    private void checkPatterns(List<PatternDef> patterns, String line,
                                String file, int lineNo, GateResult result) {
        for (PatternDef pd : patterns) {
            if (pd.matches(line)) {
                result.addSecurityFinding(pd.severity, pd.label,
                    truncate(line.trim(), 120), file, lineNo);
                console.fail(String.format("[%s] %s  →  %s:%d",
                    pd.severity, pd.label, file, lineNo));
            }
        }
    }

    // ── Utilities ─────────────────────────────────────────────

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private static int extractJsonInt(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return 0;
        int start = idx + key.length();
        while (start < json.length() && !Character.isDigit(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        try { return Integer.parseInt(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0; }
    }

    // ── Inner: pattern definition ─────────────────────────────
    private static class PatternDef {
        final String  severity;
        final String  label;
        final Pattern pattern;

        PatternDef(String severity, String label, String regex) {
            this.severity = severity;
            this.label    = label;
            this.pattern  = Pattern.compile(regex);
        }

        boolean matches(String line) {
            return pattern.matcher(line).find();
        }
    }
}
