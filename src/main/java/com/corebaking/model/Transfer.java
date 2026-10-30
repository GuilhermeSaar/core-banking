package com.corebaking.model;

import com.corebaking.model.enums.TransferStatus;
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
@Table(name = "transfers")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id", nullable = true)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id", nullable = true)
    private Account destinationAccount;

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransferStatus status;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ledger> entries = new ArrayList<>();


    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Transfer(Account acc, BigDecimal amount, TransferStatus status) {
        this.destinationAccount = acc;
        this.amount = amount;
        this.status = status;
    }


    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
