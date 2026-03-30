package com.tianzige.marketplace.model.financial;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single reporting period for a company (e.g., Q1 FY2024, FY2023 annual).
 * Income statement, balance sheet, and cash-flow rows each hang off one of these.
 */
@Entity
@Table(
    name = "financial_periods",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_financial_period",
        columnNames = {"company_id", "period_type", "fiscal_year", "fiscal_quarter"}
    ),
    indexes = {
        @Index(name = "idx_fp_company_period", columnList = "company_id, period_type, fiscal_year")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinancialPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "period_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    /** Four-digit fiscal year (e.g., 2024). */
    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    /** 1–4 for quarterly; null for annual or TTM. */
    @Column(name = "fiscal_quarter")
    private Integer fiscalQuarter;

    @Column(name = "period_start_date")
    private LocalDate periodStartDate;

    /** Last day of the reporting period (matches SEC `period` field). */
    @Column(name = "period_end_date")
    private LocalDate periodEndDate;

    /** Date the filing was submitted to SEC (or press release date). */
    @Column(name = "filing_date")
    private LocalDate filingDate;

    /** Date the company publicly reported earnings. */
    @Column(name = "report_date")
    private LocalDate reportDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
