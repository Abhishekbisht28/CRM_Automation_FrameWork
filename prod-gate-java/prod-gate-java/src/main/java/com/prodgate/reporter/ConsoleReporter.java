package com.prodgate.reporter;

import com.prodgate.config.GateConfig;
import com.prodgate.model.GateResult;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes formatted, coloured output to stdout.
 */
public class ConsoleReporter {

    private static final String RESET  = "\033[0m";
    private static final String RED    = "\033[0;31m";
    private static final String GREEN  = "\033[0;32m";
    private static final String YELLOW = "\033[1;33m";
    private static final String CYAN   = "\033[0;36m";
    private static final String BOLD   = "\033[1m";

    public void printBanner(GateConfig config) {
        System.out.println();
        System.out.println(BOLD + "╔══════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BOLD + "║       PRODUCTION GATE  —  Pre-merge Checker          ║" + RESET);
        System.out.println(BOLD + "╚══════════════════════════════════════════════════════╝" + RESET);
        System.out.println("  Branch : " + config.getBranchName());
        System.out.println("  Commit : " + config.getCommitSha());
        System.out.println("  Author : " + config.getCommitAuthor());
        System.out.println("  Time   : " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
        System.out.println();
    }

    public void section(String title) {
        separator();
        System.out.println(BOLD + "  " + title + RESET);
        separator();
    }

    public void separator() {
        System.out.println(BOLD + "──────────────────────────────────────────────────────" + RESET);
    }

    public void info(String msg)  { System.out.println(CYAN   + "[INFO]  " + RESET + msg); }
    public void ok(String msg)    { System.out.println(GREEN  + "[OK]    " + RESET + msg); }
    public void warn(String msg)  { System.out.println(YELLOW + "[WARN]  " + RESET + msg); }
    public void fail(String msg)  { System.out.println(RED    + "[FAIL]  " + RESET + msg); }

    public void printVerdict(GateResult result) {
        separator();
        if (result.isPassed()) {
            System.out.println(GREEN + BOLD);
            System.out.println("   ██████╗  █████╗ ███████╗███████╗███████╗██████╗ ");
            System.out.println("   ██╔══██╗██╔══██╗██╔════╝██╔════╝██╔════╝██╔══██╗");
            System.out.println("   ██████╔╝███████║███████╗███████╗█████╗  ██║  ██║");
            System.out.println("   ██╔═══╝ ██╔══██║╚════██║╚════██║██╔══╝  ██║  ██║");
            System.out.println("   ██║     ██║  ██║███████║███████║███████╗██████╔╝ ");
            System.out.println("   ╚═╝     ╚═╝  ╚═╝╚══════╝╚══════╝╚══════╝╚═════╝  ");
            System.out.println(RESET);
            System.out.println("   ✅  All checks passed — code is eligible for production");
        } else {
            System.out.println(RED + BOLD);
            System.out.println("   ██████╗ ██╗      ██████╗  ██████╗██╗  ██╗███████╗██████╗ ");
            System.out.println("   ██╔══██╗██║     ██╔═══██╗██╔════╝██║ ██╔╝██╔════╝██╔══██╗");
            System.out.println("   ██████╔╝██║     ██║   ██║██║     █████╔╝ █████╗  ██║  ██║");
            System.out.println("   ██╔══██╗██║     ██║   ██║██║     ██╔═██╗ ██╔══╝  ██║  ██║");
            System.out.println("   ██████╔╝███████╗╚██████╔╝╚██████╗██║  ██╗███████╗██████╔╝ ");
            System.out.println("   ╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝╚══════╝╚═════╝  ");
            System.out.println(RESET);
            System.out.println("   ❌  Gate FAILED — merge to production is BLOCKED");
            System.out.println("   Vulnerabilities : " + result.getVulnCount());
            System.out.println("   Slow APIs       : " + result.getSlowApis().size());
            System.out.println("   Regressions     : " + result.getRegressionApis().size());
        }
        separator();
    }
}
