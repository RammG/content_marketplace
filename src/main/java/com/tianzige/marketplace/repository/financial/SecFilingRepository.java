package com.tianzige.marketplace.repository.financial;

import com.tianzige.marketplace.model.financial.SecFiling;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecFilingRepository extends JpaRepository<SecFiling, UUID> {

    Optional<SecFiling> findByAdsh(String adsh);

    @EntityGraph(attributePaths = {"company", "financialPeriod", "dataItem"})
    Optional<SecFiling> findWithRelationsById(UUID id);

    Page<SecFiling> findByCompanyId(UUID companyId, Pageable pageable);

    Page<SecFiling> findByFormType(String formType, Pageable pageable);

    Page<SecFiling> findByCompanyIdAndFormType(UUID companyId, String formType, Pageable pageable);

    @Query("SELECT sf FROM SecFiling sf WHERE " +
            "(:companyId IS NULL OR sf.company.id = :companyId) AND " +
            "(:formType IS NULL OR sf.formType = :formType) AND " +
            "(:startDate IS NULL OR sf.filedDate >= :startDate) AND " +
            "(:endDate IS NULL OR sf.filedDate <= :endDate)")
    Page<SecFiling> findByFilters(
            @Param("companyId") UUID companyId,
            @Param("formType") String formType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}
