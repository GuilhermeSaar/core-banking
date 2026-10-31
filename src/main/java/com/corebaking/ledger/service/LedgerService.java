package com.corebaking.ledger.service;

import com.corebaking.ledger.dto.LedgerDTO;
import com.corebaking.ledger.repository.LedgerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class LedgerService {

    private final LedgerRepository ledgerRepository;

    //RF06 — Consultar extrato
    //• O sistema deve permitir consultar os lançamentos financeiros de uma conta, incluindo tipo, valor,
    //data, identificação da operação e natureza do lançamento.
    @Transactional(readOnly = true)
    public List<LedgerDTO> getLedgers(UUID id) {

        var ledgers = ledgerRepository.findAllByAccountId(id);

        return ledgers.stream()
                .map(LedgerDTO::new).toList();
    }
}
