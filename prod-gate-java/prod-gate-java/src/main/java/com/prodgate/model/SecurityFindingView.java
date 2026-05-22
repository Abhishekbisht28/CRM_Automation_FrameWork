package com.prodgate.model;

/**
 * Public record exposing a security finding to reporters.
 * Kept separate so the model package's internal classes remain package-private.
 */
public final class SecurityFindingView {
    public final String severity;
    public final String category;
    public final String detail;
    public final String file;
    public final int    line;

    public SecurityFindingView(String severity, String category,
                               String detail, String file, int line) {
        this.severity = severity;
        this.category = category;
        this.detail   = detail;
        this.file     = file;
        this.line     = line;
    }
}
