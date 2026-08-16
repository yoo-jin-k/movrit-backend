package com.movrit.movrit.settlement.service;

import com.movrit.movrit.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {
    private final SettlementRepository settlementRepository;
}
