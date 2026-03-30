package com.tianzige.marketplace.model.financial;

import com.tianzige.marketplace.model.dir.DataItem;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Metadata for a single SEC EDGAR filing, derived from the {@code sub.txt} dataset.
 *
 * <p>Links the normalised financial model back to the raw ingested {@link DataItem}
 * stored in the directory, and carries all SEC-specific identifiers needed to
 * cross-reference XBRL tags, numeric facts, and presentation trees.
 */
@Entity
@Table(
    name = "sec_filings",
    indexes = {
        @Index(name = "idx_sf_adsh",       columnList = "adsh",       unique = true),
        @Index(name = "idx_sf_company",    columnList = "company_id"),
        @Index(name = "idx_sf_form_filed", columnList = "form_type, filed_date")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SecFiling {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /**
     * SEC accession number — globally unique filing identifier
     * (format: XXXXXXXXXX-YY-ZZZZZZ, e.g., 0000320193-24-000006).
     */
    @Column(nullable = false, length = 25)
    private String adsh;

    /**
     * SEC form type (e.g., 10-K, 10-Q, 8-K, 20-F).
     * Stored as-is from {@code sub.txt} {@code form} column.
     */
    @Column(name = "form_type", nullable = false, length = 10)
    private String formType;

    /** Date the filing was accepted by EDGAR. */
    @Column(name = "filed_date")
    private LocalDate filedDate;

    /** Period of report as declared in the filing. */
    @Column(name = "period_of_report")
    private LocalDate periodOfReport;

    /** Company fiscal year-end (MM/DD, e.g., "12/31"). */
    @Column(name = "fiscal_year_end", length = 6)
    private String fiscalYearEnd;

    /**
     * SEC-assigned entity type (e.g., "operating").
     * Maps to {@code sub.txt} {@code entitytype} column.
     */
    @Column(name = "entity_type", length = 40)
    private String entityType;

    /** SEC Standard Industrial Classification code. */
    @Column(length = 10)
    private String sic;

    /** ISO 3166-1 alpha-2 incorporation country from SEC. */
    @Column(name = "country_of_incorporation", length = 5)
    private String countryOfIncorporation;

    /** State of incorporation (US state code). */
    @Column(name = "state_of_incorporation", length = 5)
    private String stateOfIncorporation;

    /**
     * Whether the filer is a well-known seasoned issuer.
     * Maps to {@code sub.txt} {@code wksi} column.
     */
    private Boolean wksi;

    /** Number of interactive XBRL facts (from {@code sub.txt} {@code numq} / {@code numc}). */
    @Column(name = "xbrl_fact_count")
    private Integer xbrlFactCount;

    /**
     * Back-reference to the raw ingested {@link DataItem} that holds the original
     * TSV rows parsed from the quarterly SEC ZIP file.  Nullable — set after ingestion.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_item_id")
    private DataItem dataItem;

    /**
     * The {@link FinancialPeriod} this filing maps to after normalisation.
     * Null until period matching is performed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_period_id")
    private FinancialPeriod financialPeriod;

    /**
     * Full raw {@code sub.txt} row plus any supplementary fields — stored as JSONB
     * so no raw data is lost during normalisation.
     */
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> rawDetails;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
