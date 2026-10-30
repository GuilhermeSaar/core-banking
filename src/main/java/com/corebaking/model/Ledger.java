package com.corebaking.model;

import com.corebaking.model.enums.EntryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id")
    private Transfer transfer;

    @Column(nullable = false)
    private EntryType entryType;
    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate createdAt;

    public Ledger(Account acc, Transfer transfer, EntryType entryType, BigDecimal amount) {
        this.account = acc;
        this.transfer = transfer;
        this.entryType = entryType;
        this.amount = amount;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDate.now();
    }

}
