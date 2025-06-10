package nl.inholland.bankAppBackEnd.DTOs;

import nl.inholland.bankAppBackEnd.models.BankAccount;

public class BankAccountDTO {
    private Long id;
    private String iban;
    private Double balance;
    private UserDTO owner;
    private String type;
    private Double absoluteLimit;
    private Double dailyLimit;
    private boolean active;

    // Getters
    public Long getId() {
        return id;
    }

    public String getIban() {
        return iban;
    }

    public Double getBalance() {
        return balance;
    }

    public UserDTO getOwner() {
        return owner;
    }

    public String getType() {
        return type;
    }

    public Double getAbsoluteLimit() {
        return absoluteLimit;
    }

    public Double getDailyLimit() {
        return dailyLimit;
    }

    public boolean isActive() {
        return active;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public void setOwner(UserDTO owner) {
        this.owner = owner;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAbsoluteLimit(Double absoluteLimit) {
        this.absoluteLimit = absoluteLimit;
    }

    public void setDailyLimit(Double dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Factory method
    public static BankAccountDTO fromEntity(BankAccount account) {
        if (account == null) return null;

        BankAccountDTO dto = new BankAccountDTO();
        dto.setId(account.getId());
        dto.setIban(account.getIban());
        dto.setBalance(account.getBalance());
        dto.setOwner(UserDTO.fromEntity(account.getOwner()));
        dto.setType(account.getType() != null ? account.getType().toString() : null);
        dto.setAbsoluteLimit(account.getAbsoluteLimit());
        dto.setDailyLimit(account.getDailyLimit());
        dto.setActive(account.isActive());
        return dto;
    }
}