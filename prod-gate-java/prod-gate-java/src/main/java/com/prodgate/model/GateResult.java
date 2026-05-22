package com.prodgate.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accumulates results from all gate phases and carries the final verdict.
 */
public final class GateResult {

    private boolean passed = true;

    private final List<SecurityFindingView> securityFindings = new ArrayList<>();
    private final List<ApiResultRecord>     apiResults       = new ArrayList<>();
    private final List<ApiResultRecord>     slowApis         = new ArrayList<>();
    private final List<ApiResultRecord>     regressionApis   = new ArrayList<>();

    public void addSecurityFinding(String severity, String category,
                                   String detail, String file, int line) {
        securityFindings.add(new SecurityFindingView(severity, category, detail, file, line));
    }

    public void markSecurityFailed() { this.passed = false; }

    public static ApiResultRecord newApiResult(String name, String method, String url,
                                               int status, long ms, long baseline, String reason) {
        return new ApiResultRecord(name, method, url, status, ms, baseline, reason);
    }

    public void addApiResult(ApiResultRecord r)    { apiResults.add(r); }
    public void addSlowApi(ApiResultRecord r)       { slowApis.add(r);       this.passed = false; }
    public void addRegressionApi(ApiResultRecord r) { regressionApis.add(r); this.passed = false; }

    public boolean isPassed()     { return passed; }
    public int     getVulnCount() { return securityFindings.size(); }

    public List<SecurityFindingView> getSecurityFindings() { return Collections.unmodifiableList(securityFindings); }
    public List<ApiResultRecord>     getApiResults()       { return Collections.unmodifiableList(apiResults); }
    public List<ApiResultRecord>     getSlowApis()         { return Collections.unmodifiableList(slowApis); }
    public List<ApiResultRecord>     getRegressionApis()   { return Collections.unmodifiableList(regressionApis); }
}
