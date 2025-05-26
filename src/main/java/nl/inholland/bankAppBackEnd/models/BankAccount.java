package nl.inholland.bankAppBackEnd.models;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String iban;

    private Double balance;

    @Enumerated(EnumType.STRING)
    private AccountType type;

    @ManyToOne
    private User owner;

    // Transfer limits
    private Double absoluteLimit = 0.0; // Minimum balance allowed
    private Double dailyLimit = 1000.0; // Maximum daily transfer amount
    private Double dailySpent = 0.0; // Amount spent today
    private LocalDate lastResetDate = LocalDate.now(); // Date when daily spent was last reset

    public enum AccountType {
        CHECKING,
        SAVINGS
    }

    // Reset daily spending if date has changed
    public void resetDailySpentIfNewDay() {
        LocalDate today = LocalDate.now();
        if (!today.equals(this.lastResetDate)) {
            this.dailySpent = 0.0;
            this.lastResetDate = today;
        }
    }

    // Check if transaction amount would exceed daily limit
    public boolean wouldExceedDailyLimit(Double amount) {
        resetDailySpentIfNewDay();
        return (this.dailySpent + amount) > this.dailyLimit;
    }

    // Check if transaction would violate absolute limit
    public boolean wouldViolateAbsoluteLimit(Double amount) {
        return (this.balance - amount) < this.absoluteLimit;
    }

    // Add to daily spent amount
    public void addToDailySpent(Double amount) {
        resetDailySpentIfNewDay();
        this.dailySpent += amount;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }

    public Double getAbsoluteLimit() { return absoluteLimit; }
    public void setAbsoluteLimit(Double absoluteLimit) { this.absoluteLimit = absoluteLimit; }

    public Double getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Double dailyLimit) { this.dailyLimit = dailyLimit; }

    public Double getDailySpent() {
        resetDailySpentIfNewDay();
        return dailySpent;
    }
    public void setDailySpent(Double dailySpent) { this.dailySpent = dailySpent; }

    public LocalDate getLastResetDate() { return lastResetDate; }
    public void setLastResetDate(LocalDate lastResetDate) { this.lastResetDate = lastResetDate; }

    public Double getRemainingDailyLimit() {
        resetDailySpentIfNewDay();
        return Math.max(0, this.dailyLimit - this.dailySpent);
    }
    private Boolean active = true;

    public boolean isActive() {
        return active == null ? true : active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

}