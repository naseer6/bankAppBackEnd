package nl.inholland.bankAppBackEnd.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountSearchResultDTO {
    private String ownerName;
    private String iban;
    private String accountType;
}