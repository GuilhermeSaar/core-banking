package com.corebaking.repository;

import com.corebaking.model.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LedgerRepository extends JpaRepository<Ledger, UUID> {

    Ledger findByTransferId(UUID transferId);

}
