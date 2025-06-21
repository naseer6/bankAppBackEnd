package nl.inholland.bankAppBackEnd.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "EUR"; // Default currency

    @ManyToOne
    @JoinColumn(name = "from_account_id")
    private BankAccount fromAccount;

    @ManyToOne
    @JoinColumn(name = "to_account_id")
    private BankAccount toAccount;

    @ManyToOne
    @JoinColumn(name = "initiated_by_user_id", nullable = false)
    private User initiatedByUser;

    private LocalDateTime timestamp;

    // --- Standard getters and setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BankAccount getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(BankAccount fromAccount) {
        this.fromAccount = fromAccount;
    }

    public BankAccount getToAccount() {
        return toAccount;
    }

    public void setToAccount(BankAccount toAccount) {
        this.toAccount = toAccount;
    }

    public User getInitiatedByUser() {
        return initiatedByUser;
    }

    public void setInitiatedByUser(User initiatedByUser) {
        this.initiatedByUser = initiatedByUser;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
