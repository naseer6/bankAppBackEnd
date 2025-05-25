package nl.inholland.bankAppBackEnd.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double amount;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    @ManyToOne
    @JoinColumn(name = "from_account_id")
    @JsonIgnore // Prevent sending full BankAccount object for 'fromAccount'
    private BankAccount fromAccount;

    @ManyToOne
    @JoinColumn(name = "to_account_id")
    @JsonIgnore // Prevent sending full BankAccount object for 'toAccount'
    private BankAccount toAccount;

    @ManyToOne
    @JoinColumn(name = "initiated_by_user_id", nullable = false)
    @JsonIgnore // Prevent sending full User object
    private User initiatedByUser;

    private LocalDateTime timestamp;

    // Custom getters to expose IBAN strings instead of full accounts
    public String getFromIban() {
        return fromAccount != null ? fromAccount.getIban() : null;
    }

    public String getToIban() {
        return toAccount != null ? toAccount.getIban() : null;
    }

    // Expose username of who initiated the transaction
    @JsonProperty("initiatedBy")
    public String getInitiatedByUsername() {
        return initiatedByUser != null ? initiatedByUser.getUsername() : null;
    }

    // Format timestamp as ISO date string for frontend to parse easily
    @JsonProperty("date")
    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
    }

    // Expose transactionType as 'description' for frontend clarity
    @JsonProperty("description")
    public String getDescription() {
        return transactionType;
    }

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