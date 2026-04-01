package com.tianzige.marketplace.service.financial;

import com.tianzige.marketplace.model.financial.BrokerEstimate;
import com.tianzige.marketplace.model.financial.ConsensusEstimate;
import com.tianzige.marketplace.model.financial.EstimateMetric;
import com.tianzige.marketplace.repository.financial.BrokerEstimateRepository;
import com.tianzige.marketplace.repository.financial.ConsensusEstimateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EstimateService {

    private final BrokerEstimateRepository brokerEstimateRepository;
    private final ConsensusEstimateRepository consensusEstimateRepository;

    // Broker Estimate methods
    @Transactional(readOnly = true)
    public Optional<BrokerEstimate> findBrokerEstimateById(UUID id) {
        return brokerEstimateRepository.findWithCompanyById(id);
    }

    @Transactional(readOnly = true)
    public Page<BrokerEstimate> findBrokerEstimatesByCompanyId(
            UUID companyId,
            EstimateMetric metric,
            Integer fiscalYear,
            Integer fiscalQuarter,
            Pageable pageable) {
        return brokerEstimateRepository.findByCompanyIdAndFilters(
                companyId, metric, fiscalYear, fiscalQuarter, pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<BrokerEstimate> findBrokerEstimatesByBrokerName(String brokerName, Pageable pageable) {
        return brokerEstimateRepository.findByBrokerName(brokerName, pageable);
    }

    // Consensus Estimate methods
    @Transactional(readOnly = true)
    public Optional<ConsensusEstimate> findConsensusEstimateById(UUID id) {
        return consensusEstimateRepository.findWithCompanyById(id);
    }

    @Transactional(readOnly = true)
    public Page<ConsensusEstimate> findConsensusEstimatesByCompanyId(
            UUID companyId,
            EstimateMetric metric,
            Integer fiscalYear,
            Integer fiscalQuarter,
            Pageable pageable) {
        return consensusEstimateRepository.findByCompanyIdAndFilters(
                companyId, metric, fiscalYear, fiscalQuarter, pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<ConsensusEstimate> findLatestConsensusEstimates(
            UUID companyId,
            EstimateMetric metric,
            Pageable pageable) {
        return consensusEstimateRepository.findLatestByCompanyIdAndMetric(companyId, metric, pageable);
    }
}
