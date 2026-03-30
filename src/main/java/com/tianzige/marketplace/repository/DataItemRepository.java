package com.tianzige.marketplace.repository;

import com.tianzige.marketplace.model.dir.DataItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataItemRepository extends JpaRepository<DataItem, UUID> {

    List<DataItem> findByProvider(String provider);

    List<DataItem> findBySourceType(DataItem.SourceType sourceType);

    Optional<DataItem> findByElasticsearchId(String elasticsearchId);

    Page<DataItem> findByProviderContainingIgnoreCase(String provider, Pageable pageable);

    @Query("SELECT d FROM DataItem d WHERE " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.provider) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<DataItem> searchByQuery(@Param("query") String query, Pageable pageable);
}
