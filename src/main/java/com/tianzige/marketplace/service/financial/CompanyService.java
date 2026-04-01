package com.tianzige.marketplace.service.financial;

import com.tianzige.marketplace.model.financial.Company;
import com.tianzige.marketplace.repository.financial.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public Optional<Company> findById(UUID id) {
        return companyRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Company> findByTicker(String ticker) {
        return companyRepository.findByTicker(ticker);
    }

    @Transactional(readOnly = true)
    public Optional<Company> findByCik(String cik) {
        return companyRepository.findByCik(cik);
    }

    @Transactional(readOnly = true)
    public Page<Company> findAll(Pageable pageable) {
        return companyRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Company> findByFilters(
            String ticker,
            String cik,
            String name,
            String exchange,
            String sector,
            String industry,
            Pageable pageable) {
        return companyRepository.findByFilters(
                ticker, cik, name, exchange, sector, industry, pageable
        );
    }

    @Transactional
    public Company create(Company company) {
        log.debug("Creating company: {}", company.getName());
        return companyRepository.save(company);
    }

    @Transactional
    public Company update(UUID id, String ticker, String cik, String name,
                          String exchange, String sector, String industry,
                          String sic, String countryOfIncorporation,
                          Map<String, Object> metadata) {
        return companyRepository.findById(id)
                .map(existing -> {
                    if (ticker != null) existing.setTicker(ticker);
                    if (cik != null) existing.setCik(cik);
                    if (name != null) existing.setName(name);
                    if (exchange != null) existing.setExchange(exchange);
                    if (sector != null) existing.setSector(sector);
                    if (industry != null) existing.setIndustry(industry);
                    if (sic != null) existing.setSic(sic);
                    if (countryOfIncorporation != null) existing.setCountryOfIncorporation(countryOfIncorporation);
                    if (metadata != null) existing.setMetadata(metadata);
                    log.debug("Updating company: {}", existing.getName());
                    return companyRepository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + id));
    }

    @Transactional
    public void delete(UUID id) {
        log.debug("Deleting company: {}", id);
        companyRepository.deleteById(id);
    }
}
