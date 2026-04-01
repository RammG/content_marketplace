package com.tianzige.marketplace.repository.financial;

import com.tianzige.marketplace.model.financial.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID>,
        JpaSpecificationExecutor<Company> {

    Optional<Company> findByTicker(String ticker);

    Optional<Company> findByCik(String cik);

    Page<Company> findByExchange(String exchange, Pageable pageable);

    Page<Company> findBySector(String sector, Pageable pageable);

    Page<Company> findByIndustry(String industry, Pageable pageable);

    @Query("SELECT c FROM Company c WHERE " +
            "(:ticker IS NULL OR c.ticker = :ticker) AND " +
            "(:cik IS NULL OR c.cik = :cik) AND " +
            "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:exchange IS NULL OR c.exchange = :exchange) AND " +
            "(:sector IS NULL OR c.sector = :sector) AND " +
            "(:industry IS NULL OR c.industry = :industry)")
    Page<Company> findByFilters(
            @Param("ticker") String ticker,
            @Param("cik") String cik,
            @Param("name") String name,
            @Param("exchange") String exchange,
            @Param("sector") String sector,
            @Param("industry") String industry,
            Pageable pageable
    );
}
