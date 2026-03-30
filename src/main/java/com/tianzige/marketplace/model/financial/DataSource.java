package com.tianzige.marketplace.model.financial;

public enum DataSource {
    COMPANY_REPORT,   // Direct from company press release or investor relations
    SEC_FILING,       // From SEC EDGAR filing
    BROKER_ESTIMATE,  // Analyst/broker projection
    THIRD_PARTY       // Other data vendor
}
