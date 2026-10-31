package com.corebaking.account.model;

import com.corebaking.ledger.model.Ledger;
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
    @GeneratedValue(strategy = GenerationType.UUID)
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

    public boolean isBlocked() {
        return status == StatusAccount.BLOCKED;
    }

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();

        if (this.status == null) this.status = StatusAccount.ACTIVE;
    }

    public Account(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;

        if (id == null || account.id == null) return false;

        return id.equals(account.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
