package com.tianzige.marketplace.service.financial;

import com.tianzige.marketplace.model.financial.SecFiling;
import com.tianzige.marketplace.repository.financial.SecFilingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecFilingService {

    private final SecFilingRepository secFilingRepository;

    @Transactional(readOnly = true)
    public Optional<SecFiling> findById(UUID id) {
        return secFilingRepository.findWithRelationsById(id);
    }

    @Transactional(readOnly = true)
    public Optional<SecFiling> findByAdsh(String adsh) {
        return secFilingRepository.findByAdsh(adsh);
    }

    @Transactional(readOnly = true)
    public Page<SecFiling> findByCompanyId(UUID companyId, Pageable pageable) {
        return secFilingRepository.findByCompanyId(companyId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SecFiling> findByFilters(
            UUID companyId,
            String formType,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        return secFilingRepository.findByFilters(companyId, formType, startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SecFiling> findAll(Pageable pageable) {
        return secFilingRepository.findAll(pageable);
    }
}
