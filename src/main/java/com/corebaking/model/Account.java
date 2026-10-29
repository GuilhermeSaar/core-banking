package com.corebaking.model;

import com.corebaking.model.enums.StatusAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Account {

    @Id
    private UUID id;

    @Column(unique = true)
    private String name;
    private StatusAccount status;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "account")
    private List<Ledger> ledgers = new ArrayList<>();

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();

        if (this.status == null) this.status = StatusAccount.ACTIVE;
    }

    public Account(UUID id, String name) {
        this.id = id;
        this.name = name;
    }
}
