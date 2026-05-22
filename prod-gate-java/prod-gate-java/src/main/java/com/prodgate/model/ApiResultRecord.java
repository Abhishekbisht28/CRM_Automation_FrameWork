package com.prodgate.model;

/**
 * Immutable record for a single API measurement result.
 */
public final class ApiResultRecord {
    public final String name;
    public final String method;
    public final String url;
    public final int    httpStatus;
    public final long   responseMs;
    public final long   baselineMs;   // -1 = no baseline yet
    public final String failReason;   // null = OK

    public ApiResultRecord(String name, String method, String url,
                           int httpStatus, long responseMs,
                           long baselineMs, String failReason) {
        this.name       = name;
        this.method     = method;
        this.url        = url;
        this.httpStatus = httpStatus;
        this.responseMs = responseMs;
        this.baselineMs = baselineMs;
        this.failReason = failReason;
    }

    public boolean isFailed()    { return failReason != null; }
    public boolean hasBaseline() { return baselineMs > 0; }
}
