package com.prodgate.reporter;

import com.prodgate.config.GateConfig;
import com.prodgate.model.GateResult;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Sends a failure report email via SMTP (Jakarta Mail).
 *
 * Supports:
 *  - TLS/STARTTLS (smtp.tls=true, default port 587)
 *  - SSL          (smtp.tls=false, port 465)
 *  - Local relay  (no auth, smtp.host=localhost)
 */
public class EmailReporter {

    private final GateConfig config;

    public EmailReporter(GateConfig config) {
        this.config = config;
    }

    public void send(GateResult result) {
        if (!config.isEmailEnabled()) {
            System.out.println("[INFO]  Email disabled — skipping. Enable with email.enabled=true in gate.properties.");
            printReportToStdout(result);
            return;
        }

        try {
            Session session = buildSession();
            Message msg     = buildMessage(session, result);
            Transport.send(msg);
            System.out.println("[OK]    Failure report emailed to: " + config.getEmailTo());
        } catch (MessagingException e) {
            System.err.println("[WARN]  Email delivery failed: " + e.getMessage());
            printReportToStdout(result);
        }
    }

    // ── Session ───────────────────────────────────────────────

    private Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host",    config.getSmtpHost());
        props.put("mail.smtp.port",    String.valueOf(config.getSmtpPort()));

        boolean auth = config.getSmtpUser() != null && !config.getSmtpUser().isBlank();
        props.put("mail.smtp.auth", String.valueOf(auth));

        if (config.isSmtpTls()) {
            props.put("mail.smtp.starttls.enable", "true");
        } else {
            props.put("mail.smtp.ssl.enable", "true");
        }

        if (!auth) {
            return Session.getInstance(props);
        }

        return Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getSmtpUser(), config.getSmtpPassword());
            }
        });
    }

    // ── Message ───────────────────────────────────────────────

    private Message buildMessage(Session session, GateResult result) throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(config.getEmailFrom()));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.getEmailTo()));
        msg.setSubject("🚨 [PROD GATE] BLOCKED — " + config.getBranchName()
            + " @ " + config.getCommitSha().substring(0, Math.min(7, config.getCommitSha().length())));

        // Multipart: plain + HTML
        MimeMultipart mp = new MimeMultipart("alternative");

        MimeBodyPart plain = new MimeBodyPart();
        plain.setText(buildPlainText(result), "UTF-8");
        mp.addBodyPart(plain);

        MimeBodyPart html = new MimeBodyPart();
        html.setContent(buildHtml(result), "text/html; charset=UTF-8");
        mp.addBodyPart(html);

        msg.setContent(mp);
        return msg;
    }

    // ── Plain-text body ───────────────────────────────────────

    private String buildPlainText(GateResult result) {
        StringBuilder sb = new StringBuilder();
        String ts = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);

        sb.append("======================================================\n");
        sb.append("  PRODUCTION GATE REPORT\n");
        sb.append("  Status   : ❌ BLOCKED\n");
        sb.append("  Timestamp: ").append(ts).append("\n");
        sb.append("  Branch   : ").append(config.getBranchName()).append("\n");
        sb.append("  Commit   : ").append(config.getCommitSha()).append("\n");
        sb.append("  Author   : ").append(config.getCommitAuthor()).append("\n");
        sb.append("======================================================\n\n");

        sb.append("── SECURITY SCAN ─────────────────────────────────────\n");
        sb.append("Issues found: ").append(result.getVulnCount()).append("\n");
        for (var f : result.getSecurityFindings()) {
            sb.append(String.format("  [%s] %s  →  %s:%d\n    %s\n",
                f.severity, f.category, f.file, f.line, f.detail));
        }

        sb.append("\n── API PERFORMANCE ───────────────────────────────────\n");
        sb.append("Hard limit : ").append(config.getApiHardLimitMs()).append("ms\n");
        sb.append("Regression : ").append(config.getRegressionPct()).append("% threshold\n\n");

        if (!result.getSlowApis().isEmpty()) {
            sb.append("⛔ SLOW APIs (Exceeded hard limit):\n");
            for (var a : result.getSlowApis()) {
                sb.append(String.format("  • %-28s  %4dms  HTTP%d  baseline=%dms\n    URL: %s\n    ↳ %s\n",
                    a.name, a.responseMs, a.httpStatus,
                    a.baselineMs > 0 ? a.baselineMs : -1,
                    a.url, a.failReason));
            }
        }

        if (!result.getRegressionApis().isEmpty()) {
            sb.append("\n⚠️  REGRESSION APIs:\n");
            for (var a : result.getRegressionApis()) {
                sb.append(String.format("  • %-28s  %4dms  HTTP%d  baseline=%dms\n    URL: %s\n    ↳ %s\n",
                    a.name, a.responseMs, a.httpStatus, a.baselineMs, a.url, a.failReason));
            }
        }

        sb.append("\n── ALL API RESPONSE TIMES ────────────────────────────\n");
        for (var a : result.getApiResults()) {
            String baseLabel = a.baselineMs > 0 ? "  (baseline: " + a.baselineMs + "ms)" : "  (first run)";
            sb.append(String.format("  %-30s  %4dms  HTTP%d%s\n",
                a.name, a.responseMs, a.httpStatus, baseLabel));
        }

        sb.append("\n── VERDICT ───────────────────────────────────────────\n");
        sb.append("❌ BLOCKED — Code is NOT eligible for production merge\n");
        sb.append("======================================================\n");

        return sb.toString();
    }

    // ── HTML body ─────────────────────────────────────────────

    private String buildHtml(GateResult result) {
        String ts = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        StringBuilder sb = new StringBuilder();

        sb.append("""
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="UTF-8">
            <style>
              body{font-family:monospace;background:#0d0d0d;color:#e0e0e0;padding:24px;margin:0}
              h2{color:#ff4444;border-bottom:1px solid #333;padding-bottom:8px}
              h3{color:#aaa;margin-top:24px}
              table{width:100%;border-collapse:collapse;margin-top:8px}
              th{background:#1a1a1a;color:#888;text-align:left;padding:8px 12px;font-size:12px}
              td{padding:8px 12px;border-bottom:1px solid #222;font-size:13px}
              .slow{color:#ff4444}
              .regress{color:#ffaa00}
              .ok{color:#44ff88}
              .meta{background:#111;padding:16px;border-left:4px solid #ff4444;margin-bottom:24px;border-radius:4px}
              .meta span{color:#888;margin-right:12px}
              .label{display:inline-block;padding:2px 8px;border-radius:3px;font-size:11px;font-weight:bold}
              .label-crit{background:#ff2222;color:#fff}
              .label-high{background:#ff6600;color:#fff}
              .label-med {background:#ffaa00;color:#000}
              .label-warn{background:#555;color:#fff}
              .verdict{margin-top:24px;padding:16px;text-align:center;font-size:20px;font-weight:bold;
                       background:#1a0000;border:2px solid #ff4444;border-radius:6px;color:#ff4444}
            </style>
            </head>
            <body>
            <h2>🚨 Production Gate — BLOCKED</h2>
            """);

        sb.append("<div class='meta'>");
        sb.append("<span>Branch: <b>").append(esc(config.getBranchName())).append("</b></span>");
        sb.append("<span>Commit: <b>").append(esc(config.getCommitSha())).append("</b></span>");
        sb.append("<span>Author: <b>").append(esc(config.getCommitAuthor())).append("</b></span>");
        sb.append("<span>Time: <b>").append(esc(ts)).append("</b></span>");
        sb.append("</div>");

        // Security findings
        sb.append("<h3>🔍 Security Findings (").append(result.getVulnCount()).append(")</h3>");
        if (result.getSecurityFindings().isEmpty()) {
            sb.append("<p class='ok'>✅ No security issues found.</p>");
        } else {
            sb.append("<table><tr><th>Severity</th><th>Category</th><th>Location</th><th>Detail</th></tr>");
            for (var f : result.getSecurityFindings()) {
                String cls = switch (f.severity) {
                    case "CRITICAL" -> "label-crit";
                    case "HIGH"     -> "label-high";
                    case "MEDIUM"   -> "label-med";
                    default         -> "label-warn";
                };
                sb.append("<tr>")
                  .append("<td><span class='label ").append(cls).append("'>").append(esc(f.severity)).append("</span></td>")
                  .append("<td>").append(esc(f.category)).append("</td>")
                  .append("<td>").append(esc(f.file)).append(":").append(f.line).append("</td>")
                  .append("<td>").append(esc(f.detail)).append("</td>")
                  .append("</tr>");
            }
            sb.append("</table>");
        }

        // API results
        sb.append("<h3>⚡ API Performance Results</h3>");
        sb.append("<table><tr><th>API</th><th>Method</th><th>Response</th><th>Baseline</th><th>HTTP</th><th>Status</th><th>URL</th></tr>");
        for (var a : result.getApiResults()) {
            boolean isSlow   = result.getSlowApis().contains(a);
            boolean isRegr   = result.getRegressionApis().contains(a);
            String  rowClass = isSlow ? "slow" : (isRegr ? "regress" : "ok");
            String  statusLabel = isSlow ? "⛔ SLOW" : (isRegr ? "⚠️ REGRESS" : "✅ OK");
            String  baseline = a.baselineMs > 0 ? a.baselineMs + "ms" : "—";
            sb.append("<tr class='").append(rowClass).append("'>")
              .append("<td>").append(esc(a.name)).append("</td>")
              .append("<td>").append(esc(a.method)).append("</td>")
              .append("<td><b>").append(a.responseMs).append("ms</b></td>")
              .append("<td>").append(baseline).append("</td>")
              .append("<td>").append(a.httpStatus).append("</td>")
              .append("<td>").append(statusLabel).append("</td>")
              .append("<td style='font-size:11px'>").append(esc(a.url)).append("</td>")
              .append("</tr>");
        }
        sb.append("</table>");

        sb.append("<div class='verdict'>❌ BLOCKED — Merge to production is not allowed</div>");
        sb.append("</body></html>");

        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void printReportToStdout(GateResult result) {
        System.out.println("\n════ GATE REPORT (email disabled) ════");
        System.out.println(buildPlainText(result));
        System.out.println("══════════════════════════════════════");
    }
}
