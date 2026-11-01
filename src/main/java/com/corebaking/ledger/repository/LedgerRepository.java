package com.corebaking.ledger.repository;

import com.corebaking.ledger.model.Ledger;
import com.corebaking.shared.repository.ReadSaveRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LedgerRepository extends ReadSaveRepository<Ledger, UUID> {

    Ledger findByTransferId(UUID transferId);

    @Query("""
        SELECT l FROM Ledger l where l.account.id = :accountId
    """)
    List<Ledger> findAllByAccountId(@Param("accountId") UUID accountId);
}
