package com.corebaking.model;

import com.corebaking.model.enums.IdempotencyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "idempotency_records")
@Getter
@Setter
@Entity
@NoArgsConstructor
public class Idempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private UUID idempotencyKey;

    private String requestHash;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "transfer_id")
    private Transfer transfer;

    @Enumerated(EnumType.STRING)
    private IdempotencyStatus status;

    @Column(nullable = false)
    private LocalDateTime responseData;

    public Idempotency(UUID idempotencyKey, String currentHash, IdempotencyStatus idempotencyStatus, Transfer transfer) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = currentHash;
        this.status = idempotencyStatus;
        this.transfer = transfer;
    }

    @PrePersist
    public void prePersist() {
        this.responseData = LocalDateTime.now();
    }

}
