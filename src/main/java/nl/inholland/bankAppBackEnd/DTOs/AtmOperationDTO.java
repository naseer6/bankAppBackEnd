package nl.inholland.bankAppBackEnd.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for ATM operations (deposit and withdraw).
 * Used to transfer data between the ATM interface and the backend.
 */
@Schema(description = "Data transfer object for ATM operations")
public class AtmOperationDTO {

    @Schema(description = "IBAN of the account for the ATM operation", example = "NL91INHO0123456789")
    private String iban;

    @Schema(description = "Amount to deposit or withdraw in EUR", example = "100.00")
    private Double amount;

    @Schema(description = "Description of the operation", example = "Withdrawal for groceries")
    private String description;

    // Constructors
    public AtmOperationDTO() {}

    public AtmOperationDTO(String iban, Double amount, String description) {
        this.iban = iban;
        this.amount = amount;
        this.description = description;
    }

    // Getters and Setters
    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
