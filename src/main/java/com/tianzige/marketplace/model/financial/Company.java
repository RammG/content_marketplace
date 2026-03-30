package com.tianzige.marketplace.model.financial;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Master company record.  Acts as the root anchor for all financial data —
 * fundamentals, SEC filings, and broker estimates all reference this entity.
 */
@Entity
@Table(
    name = "companies",
    indexes = {
        @Index(name = "idx_companies_ticker", columnList = "ticker"),
        @Index(name = "idx_companies_cik",    columnList = "cik")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Stock ticker symbol (e.g., AAPL, MSFT). */
    @Column(unique = true)
    private String ticker;

    /** SEC Central Index Key — globally unique per filer. */
    @Column(unique = true, length = 20)
    private String cik;

    @Column(nullable = false)
    private String name;

    /** Exchange listed on (NYSE, NASDAQ, OTC, etc.). */
    @Column(length = 20)
    private String exchange;

    /** GICS or custom sector label. */
    private String sector;

    private String industry;

    /** SEC Standard Industrial Classification code. */
    @Column(length = 10)
    private String sic;

    /** ISO 3166-1 alpha-2 country code of incorporation. */
    @Column(length = 5)
    private String countryOfIncorporation;

    /** Extra attributes (fiscal year end month, currency, etc.). */
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
