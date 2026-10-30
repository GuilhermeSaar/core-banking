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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private UUID idempotencyKey;
    private String requestHash;
    @Enumerated(EnumType.STRING)
    private IdempotencyStatus status;
    @Column(nullable = false)
    private LocalDateTime responseData;

    @PrePersist
    public void prePersist() {
        this.responseData = LocalDateTime.now();
    }

}
