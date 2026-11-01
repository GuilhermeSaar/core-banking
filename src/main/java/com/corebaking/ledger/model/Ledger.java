package com.corebaking.ledger.model;

import com.corebaking.account.model.Account;
import com.corebaking.transfers.model.Transfer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    @Enumerated(EnumType.STRING)
    private EntryType entryType;

    private EntryNature nature;

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Ledger(Account acc, Transfer transfer, EntryType entryType, BigDecimal amount, EntryNature nature) {
        this.account = acc;
        this.transfer = transfer;
        this.entryType = entryType;
        this.amount = amount;
        this.nature = nature;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
