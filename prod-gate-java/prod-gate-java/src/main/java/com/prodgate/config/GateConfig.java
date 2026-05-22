package com.prodgate.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Loads and holds all gate configuration from gate.properties.
 */
public class GateConfig {

    // Performance thresholds
    private int apiHardLimitMs   = 600;
    private int maxSlowApis      = 3;
    private int regressionPct    = 10;

    // Email
    private boolean emailEnabled = false;
    private String  emailTo;
    private String  emailFrom;
    private String  smtpHost;
    private int     smtpPort     = 587;
    private String  smtpUser;
    private String  smtpPassword;
    private boolean smtpTls      = true;

    // Paths (set by CLI)
    private Path codePath    = Paths.get(".");
    private Path apisPath    = Paths.get("config/apis.json");
    private Path baselinePath= Paths.get("baseline/api_baselines.json");

    // CI context (read from env vars automatically)
    private String branchName;
    private String commitSha;
    private String commitAuthor;

    private GateConfig() {}

    public static GateConfig load(String configFilePath) {
        GateConfig cfg = new GateConfig();
        Properties props = new Properties();

        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            props.load(fis);
        } catch (IOException e) {
            System.out.println("[WARN] Config file not found at " + configFilePath
                + " — using defaults. Copy config/gate.properties.example to config/gate.properties.");
        }

        // Performance
        cfg.apiHardLimitMs = parseInt(props, "api.hard.limit.ms",    600);
        cfg.maxSlowApis    = parseInt(props, "api.max.slow.count",   3);
        cfg.regressionPct  = parseInt(props, "api.regression.pct",   10);

        // Email
        cfg.emailEnabled   = Boolean.parseBoolean(resolve(props, "email.enabled",  "false"));
        cfg.emailTo        = resolve(props, "email.to",       "");
        cfg.emailFrom      = resolve(props, "email.from",     "prodgate@localhost");
        cfg.smtpHost       = resolve(props, "smtp.host",      "localhost");
        cfg.smtpPort       = parseInt(props, "smtp.port",     587);
        cfg.smtpUser       = resolve(props, "smtp.user",      "");
        cfg.smtpPassword   = resolve(props, "smtp.password",  "");
        cfg.smtpTls        = Boolean.parseBoolean(resolve(props, "smtp.tls", "true"));

        // CI context — prefer env vars so secrets stay out of config files
        cfg.branchName   = env("GITHUB_HEAD_REF",
                           env("CI_MERGE_REQUEST_SOURCE_BRANCH_NAME",
                           env("GIT_BRANCH", "unknown")));
        cfg.commitSha    = env("GITHUB_SHA",
                           env("CI_COMMIT_SHA",   "unknown"));
        cfg.commitAuthor = env("GITHUB_ACTOR",
                           env("GITLAB_USER_NAME","unknown"));

        return cfg;
    }

    // ── Helpers ──────────────────────────────────────────────

    /** Resolve: check env var override first, then properties, then fallback. */
    private static String resolve(Properties props, String key, String fallback) {
        // Allow env-var override: email.to → EMAIL_TO
        String envKey = key.toUpperCase().replace('.', '_');
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) return envVal;
        return props.getProperty(key, fallback);
    }

    private static int parseInt(Properties props, String key, int fallback) {
        try { return Integer.parseInt(resolve(props, key, String.valueOf(fallback))); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v != null && !v.isBlank()) ? v : fallback;
    }

    // ── Getters / Setters ─────────────────────────────────────

    public int     getApiHardLimitMs()  { return apiHardLimitMs; }
    public int     getMaxSlowApis()     { return maxSlowApis; }
    public int     getRegressionPct()   { return regressionPct; }
    public boolean isEmailEnabled()     { return emailEnabled; }
    public String  getEmailTo()         { return emailTo; }
    public String  getEmailFrom()       { return emailFrom; }
    public String  getSmtpHost()        { return smtpHost; }
    public int     getSmtpPort()        { return smtpPort; }
    public String  getSmtpUser()        { return smtpUser; }
    public String  getSmtpPassword()    { return smtpPassword; }
    public boolean isSmtpTls()          { return smtpTls; }
    public Path    getCodePath()        { return codePath; }
    public Path    getApisPath()        { return apisPath; }
    public Path    getBaselinePath()    { return baselinePath; }
    public String  getBranchName()      { return branchName; }
    public String  getCommitSha()       { return commitSha; }
    public String  getCommitAuthor()    { return commitAuthor; }

    public void setCodePath(Path p)    { this.codePath    = p; }
    public void setApisPath(Path p)    { this.apisPath    = p; }
    public void setBaselinePath(Path p){ this.baselinePath= p; }
}
