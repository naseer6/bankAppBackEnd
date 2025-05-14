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

    public enum AccountType {
        CHECKING,
        SAVINGS
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

}