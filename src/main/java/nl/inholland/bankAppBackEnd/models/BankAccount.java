package nl.inholland.bankAppBackEnd.models;
import jakarta.persistence.*;

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

    @Column(name = "absolute_limit")
    private Double absoluteLimit = 0.0; // Minimum balance allowed

    @Column(name = "daily_limit")
    private Double dailyLimit = 1000.0; // Maximum daily transfer amount

    public enum AccountType {
        CHECKING,
        SAVINGS
    }

    // Constructors
    public BankAccount() {}

    public BankAccount(String iban, Double balance, AccountType type, User owner) {
        this.iban = iban;
        this.balance = balance;
        this.type = type;
        this.owner = owner;
        this.absoluteLimit = 0.0;
        this.dailyLimit = 1000.0;
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

    // Business logic methods
    public boolean canWithdraw(Double amount) {
        return (balance - amount) >= absoluteLimit;
    }

    public Double getAvailableBalance() {
        return balance - absoluteLimit;
    }
}