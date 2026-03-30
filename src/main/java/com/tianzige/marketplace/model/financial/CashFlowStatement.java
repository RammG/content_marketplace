package com.tianzige.marketplace.model.financial;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Statement of cash flows for one {@link FinancialPeriod}.
 */
@Entity
@Table(
    name = "cash_flow_statements",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_cash_flow_statement",
        columnNames = {"financial_period_id", "data_source"}
    ),
    indexes = {
        @Index(name = "idx_cfs_period", columnList = "financial_period_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CashFlowStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_period_id", nullable = false)
    private FinancialPeriod financialPeriod;

    @Column(name = "data_source", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private DataSource dataSource;

    @Column(name = "source_ref")
    private String sourceRef;

    // ── Operating Activities ─────────────────────────────────────────────────

    @Column(name = "net_income")
    private BigDecimal netIncome;

    @Column(name = "depreciation_and_amortization")
    private BigDecimal depreciationAndAmortization;

    @Column(name = "stock_based_compensation")
    private BigDecimal stockBasedCompensation;

    @Column(name = "change_in_working_capital")
    private BigDecimal changeInWorkingCapital;

    @Column(name = "other_operating_activities")
    private BigDecimal otherOperatingActivities;

    @Column(name = "operating_cash_flow")
    private BigDecimal operatingCashFlow;

    // ── Investing Activities ─────────────────────────────────────────────────

    @Column(name = "capital_expenditures")
    private BigDecimal capitalExpenditures;

    @Column(name = "acquisitions")
    private BigDecimal acquisitions;

    @Column(name = "purchases_of_investments")
    private BigDecimal purchasesOfInvestments;

    @Column(name = "sales_of_investments")
    private BigDecimal salesOfInvestments;

    @Column(name = "other_investing_activities")
    private BigDecimal otherInvestingActivities;

    @Column(name = "investing_cash_flow")
    private BigDecimal investingCashFlow;

    // ── Financing Activities ─────────────────────────────────────────────────

    @Column(name = "dividends_paid")
    private BigDecimal dividendsPaid;

    @Column(name = "stock_repurchases")
    private BigDecimal stockRepurchases;

    @Column(name = "common_stock_issuance")
    private BigDecimal commonStockIssuance;

    @Column(name = "debt_issuance")
    private BigDecimal debtIssuance;

    @Column(name = "debt_repayment")
    private BigDecimal debtRepayment;

    @Column(name = "other_financing_activities")
    private BigDecimal otherFinancingActivities;

    @Column(name = "financing_cash_flow")
    private BigDecimal financingCashFlow;

    // ── Summary ──────────────────────────────────────────────────────────────

    @Column(name = "effect_of_exchange_rate")
    private BigDecimal effectOfExchangeRate;

    @Column(name = "net_change_in_cash")
    private BigDecimal netChangeInCash;

    /** free_cash_flow = operating_cash_flow + capital_expenditures (capex is negative). */
    @Column(name = "free_cash_flow")
    private BigDecimal freeCashFlow;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
