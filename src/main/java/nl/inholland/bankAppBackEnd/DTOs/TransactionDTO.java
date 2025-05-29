package nl.inholland.bankAppBackEnd.DTOs;

public class TransactionDTO {
    private Long id;
    private String fromIban;
    private String toIban;
    private Double amount;
    private String date;
    private String description;
    private String initiatedBy;
    private String direction;
    private Double signedAmount;

    // Default constructor
    public TransactionDTO() {
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFromIban() {
        return fromIban;
    }

    public void setFromIban(String fromIban) {
        this.fromIban = fromIban;
    }

    public String getToIban() {
        return toIban;
    }

    public void setToIban(String toIban) {
        this.toIban = toIban;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Double getSignedAmount() {
        return signedAmount;
    }

    public void setSignedAmount(Double signedAmount) {
        this.signedAmount = signedAmount;
    }
}
