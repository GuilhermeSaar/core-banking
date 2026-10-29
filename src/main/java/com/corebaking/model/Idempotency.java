package com.corebaking.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Table(name = "idempotency_records")
@Getter
@Setter
@Entity
public class Idempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String idempotencyKey;
    private String requestHash;
    private String status;
    private LocalDateTime responseData;

}
