package nl.inholland.bankAppBackEnd.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_transfer_tracking")
public class DailyTransferTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccount account;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Column(name = "total_transferred", nullable = false)
    private Double totalTransferred = 0.0;

    // Constructors
    public DailyTransferTracking() {}

    public DailyTransferTracking(BankAccount account, LocalDate transferDate) {
        this.account = account;
        this.transferDate = transferDate;
        this.totalTransferred = 0.0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BankAccount getAccount() {
        return account;
    }

    public void setAccount(BankAccount account) {
        this.account = account;
    }

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(LocalDate transferDate) {
        this.transferDate = transferDate;
    }

    public Double getTotalTransferred() {
        return totalTransferred;
    }

    public void setTotalTransferred(Double totalTransferred) {
        this.totalTransferred = totalTransferred;
    }

    // Business methods
    public void addTransfer(Double amount) {
        this.totalTransferred += amount;
    }

    public boolean canTransfer(Double amount, Double dailyLimit) {
        return (this.totalTransferred + amount) <= dailyLimit;
    }

    public Double getRemainingDailyLimit(Double dailyLimit) {
        return Math.max(0, dailyLimit - this.totalTransferred);
    }
}