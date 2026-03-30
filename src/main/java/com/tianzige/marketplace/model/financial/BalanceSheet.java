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
 * Balance sheet (statement of financial position) for one {@link FinancialPeriod}.
 * Represents a point-in-time snapshot at period end date.
 */
@Entity
@Table(
    name = "balance_sheets",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_balance_sheet",
        columnNames = {"financial_period_id", "data_source"}
    ),
    indexes = {
        @Index(name = "idx_bs_period", columnList = "financial_period_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BalanceSheet {

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

    // ── Current Assets ───────────────────────────────────────────────────────

    @Column(name = "cash_and_equivalents")
    private BigDecimal cashAndEquivalents;

    @Column(name = "short_term_investments")
    private BigDecimal shortTermInvestments;

    @Column(name = "accounts_receivable")
    private BigDecimal accountsReceivable;

    private BigDecimal inventory;

    @Column(name = "other_current_assets")
    private BigDecimal otherCurrentAssets;

    @Column(name = "total_current_assets")
    private BigDecimal totalCurrentAssets;

    // ── Non-current Assets ───────────────────────────────────────────────────

    @Column(name = "property_plant_equipment_net")
    private BigDecimal propertyPlantEquipmentNet;

    private BigDecimal goodwill;

    @Column(name = "intangible_assets")
    private BigDecimal intangibleAssets;

    @Column(name = "long_term_investments")
    private BigDecimal longTermInvestments;

    @Column(name = "other_non_current_assets")
    private BigDecimal otherNonCurrentAssets;

    @Column(name = "total_non_current_assets")
    private BigDecimal totalNonCurrentAssets;

    @Column(name = "total_assets")
    private BigDecimal totalAssets;

    // ── Current Liabilities ──────────────────────────────────────────────────

    @Column(name = "accounts_payable")
    private BigDecimal accountsPayable;

    @Column(name = "short_term_debt")
    private BigDecimal shortTermDebt;

    @Column(name = "deferred_revenue_current")
    private BigDecimal deferredRevenueCurrent;

    @Column(name = "other_current_liabilities")
    private BigDecimal otherCurrentLiabilities;

    @Column(name = "total_current_liabilities")
    private BigDecimal totalCurrentLiabilities;

    // ── Non-current Liabilities ──────────────────────────────────────────────

    @Column(name = "long_term_debt")
    private BigDecimal longTermDebt;

    @Column(name = "deferred_revenue_non_current")
    private BigDecimal deferredRevenueNonCurrent;

    @Column(name = "deferred_tax_liabilities")
    private BigDecimal deferredTaxLiabilities;

    @Column(name = "other_non_current_liabilities")
    private BigDecimal otherNonCurrentLiabilities;

    @Column(name = "total_non_current_liabilities")
    private BigDecimal totalNonCurrentLiabilities;

    @Column(name = "total_liabilities")
    private BigDecimal totalLiabilities;

    // ── Equity ───────────────────────────────────────────────────────────────

    @Column(name = "common_stock")
    private BigDecimal commonStock;

    @Column(name = "additional_paid_in_capital")
    private BigDecimal additionalPaidInCapital;

    @Column(name = "retained_earnings")
    private BigDecimal retainedEarnings;

    @Column(name = "treasury_stock")
    private BigDecimal treasuryStock;

    @Column(name = "accumulated_other_comprehensive_income")
    private BigDecimal accumulatedOtherComprehensiveIncome;

    @Column(name = "total_stockholders_equity")
    private BigDecimal totalStockholdersEquity;

    @Column(name = "minority_interest")
    private BigDecimal minorityInterest;

    @Column(name = "total_equity")
    private BigDecimal totalEquity;

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
