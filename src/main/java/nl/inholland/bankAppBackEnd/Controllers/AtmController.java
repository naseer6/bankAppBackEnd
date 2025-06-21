package nl.inholland.bankAppBackEnd.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.inholland.bankAppBackEnd.DTOs.AtmOperationDTO;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller for ATM operations including deposits and withdrawals.
 * Implements the ATM user stories from the requirements.
 */
@RestController
@RequestMapping("/api/atm")
@Tag(name = "ATM API", description = "Endpoints for ATM operations")
public class AtmController {

    private final UserService userService;
    private final BankAccountService bankAccountService;
    private final TransactionService transactionService;

    @Autowired
    public AtmController(UserService userService, BankAccountService bankAccountService,
                        TransactionService transactionService) {
        this.userService = userService;
        this.bankAccountService = bankAccountService;
        this.transactionService = transactionService;
    }

    /**
     * Authenticates a customer at the ATM.
     * @param credentials Email and password
     * @return User details if authentication is successful
     */
    @Operation(summary = "ATM Login",
              description = "Authenticates a customer at an ATM")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        User user = userService.findByEmail(email);

        if (user == null || !userService.verifyPassword(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid credentials");
        }

        if (!user.isApproved() || user.getRole() != User.Role.CUSTOMER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only approved customers can use the ATM");
        }

        // Return user details without sensitive information
        Map<String, Object> userDetails = Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "email", user.getEmail()
        );

        return ResponseEntity.ok(userDetails);
    }

    /**
     * Gets a customer's accounts available at the ATM.
     * @param userId Customer ID
     * @return List of checking accounts
     */
    @Operation(summary = "Get customer accounts",
              description = "Returns checking accounts available for ATM operations")
    @GetMapping("/accounts/{userId}")
    public ResponseEntity<?> getAccounts(@PathVariable Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<BankAccount> accounts = bankAccountService.getAccountsByUser(user)
                .stream()
                .filter(account -> account.getType() == BankAccount.AccountType.CHECKING)
                .toList();

        return ResponseEntity.ok(accounts);
    }

    /**
     * Deposits money into an account via ATM.
     * @param atmOperation Deposit details
     * @return Transaction details
     */
    @Operation(summary = "Deposit into ATM",
              description = "Deposits money into a customer's account via ATM")
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody AtmOperationDTO atmOperation) {
        try {
            BankAccount account = bankAccountService.getAccountByIban(atmOperation.getIban());

            // Validate the amount is positive
            if (atmOperation.getAmount() <= 0) {
                return ResponseEntity.badRequest().body("Amount must be positive");
            }

            // Create deposit transaction through service layer
            Transaction transaction = transactionService.createAtmDeposit(
                account,
                atmOperation.getAmount(),
                account.getOwner(),
                atmOperation.getDescription()
            );

            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Deposit failed: " + e.getMessage());
        }
    }

    /**
     * Withdraws money from an account via ATM.
     * Enforces daily and absolute limits.
     * @param atmOperation Withdrawal details
     * @return Transaction details
     */
    @Operation(summary = "Withdraw from ATM",
              description = "Withdraws money from a customer's account via ATM, enforcing limits")
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody AtmOperationDTO atmOperation) {
        try {
            BankAccount account = bankAccountService.getAccountByIban(atmOperation.getIban());

            // Validate the amount is positive
            if (atmOperation.getAmount() <= 0) {
                return ResponseEntity.badRequest().body("Amount must be positive");
            }

            // Create withdrawal transaction through service layer
            Transaction transaction = transactionService.createAtmWithdrawal(
                account,
                atmOperation.getAmount(),
                account.getOwner(),
                atmOperation.getDescription()
            );

            return ResponseEntity.ok(transaction);
        } catch (IllegalArgumentException e) {
            // Return specific error messages for validation failures
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Withdrawal failed: " + e.getMessage());
        }
    }
}
