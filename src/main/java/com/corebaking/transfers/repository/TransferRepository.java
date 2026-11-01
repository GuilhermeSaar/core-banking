package com.corebaking.transfers.repository;

import com.corebaking.shared.repository.ReadSaveRepository;
import com.corebaking.transfers.model.Transfer;

import java.util.UUID;

public interface TransferRepository extends ReadSaveRepository<Transfer, UUID> {
}
