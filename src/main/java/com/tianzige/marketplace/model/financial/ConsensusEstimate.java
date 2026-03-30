package com.tianzige.marketplace.model.financial;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Street consensus estimate — the aggregate view across all broker estimates
 * for a given company, period, and metric.  Recomputed as new broker estimates arrive.
 */
@Entity
@Table(
    name = "consensus_estimates",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_consensus_estimate",
        columnNames = {"company_id", "period_type", "fiscal_year", "fiscal_quarter", "metric", "estimate_date"}
    ),
    indexes = {
        @Index(name = "idx_ce_company_period", columnList = "company_id, fiscal_year, fiscal_quarter, metric")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsensusEstimate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** Date this consensus snapshot was computed. */
    @Column(name = "estimate_date", nullable = false)
    private LocalDate estimateDate;

    @Column(name = "period_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(name = "fiscal_quarter")
    private Integer fiscalQuarter;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EstimateMetric metric;

    /** Number of individual broker estimates included. */
    @Column(name = "estimate_count", nullable = false)
    private Integer estimateCount;

    @Column(name = "mean_value", precision = 20, scale = 4)
    private BigDecimal meanValue;

    @Column(name = "median_value", precision = 20, scale = 4)
    private BigDecimal medianValue;

    @Column(name = "high_value", precision = 20, scale = 4)
    private BigDecimal highValue;

    @Column(name = "low_value", precision = 20, scale = 4)
    private BigDecimal lowValue;

    @Column(name = "std_deviation", precision = 20, scale = 4)
    private BigDecimal stdDeviation;

    /** ISO 4217 currency code. */
    @Column(length = 5)
    private String currency;

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
