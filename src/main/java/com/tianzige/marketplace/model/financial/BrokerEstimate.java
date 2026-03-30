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
 * A single analyst/broker estimate for one metric of one company's future period.
 * Multiple rows per (company, period, metric) — one per broker/analyst.
 */
@Entity
@Table(
    name = "broker_estimates",
    indexes = {
        @Index(name = "idx_be_company",         columnList = "company_id"),
        @Index(name = "idx_be_company_period",  columnList = "company_id, fiscal_year, fiscal_quarter, metric"),
        @Index(name = "idx_be_broker_metric",   columnList = "broker_name, metric, estimate_date")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BrokerEstimate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** Name of the brokerage / research firm (e.g., "Goldman Sachs"). */
    @Column(name = "broker_name", nullable = false)
    private String brokerName;

    /** Individual analyst name (optional). */
    @Column(name = "analyst_name")
    private String analystName;

    /** Date this estimate was published / last revised. */
    @Column(name = "estimate_date", nullable = false)
    private LocalDate estimateDate;

    @Column(name = "period_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    /** Fiscal year the estimate is for (e.g., 2025). */
    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    /** Fiscal quarter (1–4), null for annual estimates. */
    @Column(name = "fiscal_quarter")
    private Integer fiscalQuarter;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EstimateMetric metric;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal value;

    /** ISO 4217 currency code (e.g., USD). */
    @Column(length = 5)
    private String currency;

    /** Analyst price target (applicable when metric is a price-based measure). */
    @Column(name = "price_target", precision = 10, scale = 2)
    private BigDecimal priceTarget;

    /** BUY / HOLD / SELL rating string as provided by the source. */
    private String rating;

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
