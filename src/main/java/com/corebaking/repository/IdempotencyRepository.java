package com.corebaking.repository;

import com.corebaking.model.Idempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository extends JpaRepository<Idempotency, UUID> {

    Optional<Idempotency> findByIdempotencyKey(UUID idempotencyKey);

}
