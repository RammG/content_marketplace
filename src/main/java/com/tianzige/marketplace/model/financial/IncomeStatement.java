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
 * Income statement (P&L) for one {@link FinancialPeriod}.
 * All monetary values are in the company's reporting currency (see metadata).
 * Amounts are stored in millions unless the company uses a different unit —
 * record the unit in {@code metadata.unit}.
 */
@Entity
@Table(
    name = "income_statements",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_income_statement",
        columnNames = {"financial_period_id", "data_source"}
    ),
    indexes = {
        @Index(name = "idx_is_period", columnList = "financial_period_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncomeStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_period_id", nullable = false)
    private FinancialPeriod financialPeriod;

    @Column(name = "data_source", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private DataSource dataSource;

    /** External reference (e.g., SEC ADSH, DataItem UUID as string). */
    @Column(name = "source_ref")
    private String sourceRef;

    // ── Revenue ──────────────────────────────────────────────────────────────

    private BigDecimal revenue;

    @Column(name = "cost_of_revenue")
    private BigDecimal costOfRevenue;

    @Column(name = "gross_profit")
    private BigDecimal grossProfit;

    // ── Operating Expenses ───────────────────────────────────────────────────

    @Column(name = "research_and_development")
    private BigDecimal researchAndDevelopment;

    @Column(name = "selling_general_admin")
    private BigDecimal sellingGeneralAdmin;

    @Column(name = "other_operating_expenses")
    private BigDecimal otherOperatingExpenses;

    @Column(name = "total_operating_expenses")
    private BigDecimal totalOperatingExpenses;

    @Column(name = "operating_income")
    private BigDecimal operatingIncome;

    // ── Below-the-line ───────────────────────────────────────────────────────

    @Column(name = "interest_expense")
    private BigDecimal interestExpense;

    @Column(name = "interest_income")
    private BigDecimal interestIncome;

    @Column(name = "other_non_operating_income")
    private BigDecimal otherNonOperatingIncome;

    @Column(name = "pretax_income")
    private BigDecimal pretaxIncome;

    @Column(name = "income_tax_expense")
    private BigDecimal incomeTaxExpense;

    @Column(name = "net_income")
    private BigDecimal netIncome;

    @Column(name = "net_income_attributable_to_minority")
    private BigDecimal netIncomeAttributableToMinority;

    @Column(name = "net_income_to_common")
    private BigDecimal netIncomeToCommon;

    // ── Per-share ────────────────────────────────────────────────────────────

    @Column(name = "eps_basic", precision = 10, scale = 4)
    private BigDecimal epsBasic;

    @Column(name = "eps_diluted", precision = 10, scale = 4)
    private BigDecimal epsDiluted;

    @Column(name = "shares_basic")
    private BigDecimal sharesBasic;

    @Column(name = "shares_diluted")
    private BigDecimal sharesDiluted;

    // ── Non-GAAP / derived ───────────────────────────────────────────────────

    /** EBITDA = operating_income + D&A (derived or reported). */
    private BigDecimal ebitda;

    @Column(name = "depreciation_and_amortization")
    private BigDecimal depreciationAndAmortization;

    /** Flexible store for additional line items or vendor-specific fields. */
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
