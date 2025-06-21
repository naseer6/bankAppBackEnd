package nl.inholland.bankAppBackEnd.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.inholland.bankAppBackEnd.DTOs.BankAccountDTO;
import nl.inholland.bankAppBackEnd.DTOs.DashboardStatsDTO;
import nl.inholland.bankAppBackEnd.DTOs.TransactionDTO;
import nl.inholland.bankAppBackEnd.exceptions.ResourceNotFoundException;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for employee operations including customer approval, account management, and system oversight.
 * All endpoints are restricted to users with EMPLOYEE role.
 */
@RestController
@RequestMapping("/api/employee")
@Tag(name = "Employee API", description = "Endpoints for employee operations")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final BankAccountService bankAccountService;
    private final TransactionService transactionService;
    private final UserService userService;

    @Autowired
    public AdminController(UserService userService, BankAccountService bankAccountService,
                          TransactionService transactionService, UserRepository userRepository) {
        this.userService = userService;
        this.bankAccountService = bankAccountService;
        this.transactionService = transactionService;
        this.userRepository = userRepository;
    }

    /**
     * Approves a customer account and creates both checking and savings accounts.
     * Sets absolute and daily transfer limits during account creation.
     * @param userId ID of the customer to approve
     * @param accountDetails Contains limits for the created accounts
     * @return Success message or error
     */
    @Operation(summary = "Approve customer registration and create accounts",
              description = "Approves a customer and creates checking and savings accounts with specified limits")
    @PostMapping("/approve/{userId}")
    public ResponseEntity<String> approveCustomer(
            @PathVariable Long userId,
            @RequestBody BankAccountDTO accountDetails) {

        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.getRole() == User.Role.CUSTOMER && !user.isApproved()) {
                // Approve the customer
                user.setApproved(true);
                userRepository.save(user);

                // Create checking and savings accounts with specified limits
                BankAccount checkingAccount = bankAccountService.createCheckingAccount(
                    user,
                    accountDetails.getAbsoluteLimit(),
                    accountDetails.getDailyLimit()
                );

                BankAccount savingsAccount = bankAccountService.createSavingsAccount(user);

                return ResponseEntity.ok("Customer approved and accounts created successfully. " +
                        "Checking account IBAN: " + checkingAccount.getIban() +
                        ", Savings account IBAN: " + savingsAccount.getIban());
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("User is already approved or not a customer.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
    }

    /**
     * Retrieves a list of all customers who don't yet have approved accounts.
     * @return List of unapproved customers
     */
    @Operation(summary = "Get unapproved customers",
              description = "Returns a list of customers who have registered but haven't been approved yet")
    @GetMapping("/unapproved-customers")
    public ResponseEntity<?> getUnapprovedCustomers() {
        List<User> unapprovedCustomers = userService.findUnapprovedCustomers();
        return ResponseEntity.ok(unapprovedCustomers);
    }

    /**
     * Gets all bank accounts in the system.
     * @return List of all bank accounts
     */
    @Operation(summary = "Get all bank accounts",
              description = "Returns a list of all bank accounts in the system")
    @GetMapping("/accounts")
    public ResponseEntity<List<BankAccount>> getAllAccounts() {
        return ResponseEntity.ok(bankAccountService.getAllAccounts());
    }

    /**
     * Gets all transactions in the system.
     * @return List of all transactions
     */
    @Operation(summary = "Get all transactions",
              description = "Returns a list of all transactions in the system")
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    /**
     * Gets transactions for a specific customer.
     * @param userId ID of the customer
     * @return List of transactions for the customer
     */
    @Operation(summary = "Get customer transactions",
              description = "Returns all transactions for a specific customer")
    @GetMapping("/customer/{userId}/transactions")
    public ResponseEntity<List<Transaction>> getCustomerTransactions(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ResponseEntity.ok(transactionService.getTransactionsByUser(user));
    }

    /**
     * Creates a transfer between customer accounts.
     * @param transactionDTO Transfer details
     * @return Created transaction
     */
    @Operation(summary = "Transfer between customer accounts",
              description = "Creates a transfer between customer accounts initiated by an employee")
    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transferBetweenAccounts(@RequestBody TransactionDTO transactionDTO) {
        Transaction transaction = transactionService.createTransaction(transactionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    /**
     * Updates the absolute transfer limit on a customer account.
     * @param accountId ID of the account
     * @param limit New absolute limit
     * @return Updated account
     */
    @Operation(summary = "Set absolute transfer limit",
              description = "Updates the absolute transfer limit on a customer account")
    @PutMapping("/account/{accountId}/absolute-limit")
    public ResponseEntity<BankAccount> setAbsoluteLimit(
            @PathVariable Long accountId,
            @RequestParam double limit) {

        BankAccount account = bankAccountService.setAbsoluteLimit(accountId, limit);
        return ResponseEntity.ok(account);
    }

    /**
     * Updates the daily transfer limit on a customer account.
     * @param accountId ID of the account
     * @param limit New daily limit
     * @return Updated account
     */
    @Operation(summary = "Set daily transfer limit",
              description = "Updates the daily transfer limit on a customer account")
    @PutMapping("/account/{accountId}/daily-limit")
    public ResponseEntity<BankAccount> setDailyLimit(
            @PathVariable Long accountId,
            @RequestParam double limit) {

        BankAccount account = bankAccountService.setDailyLimit(accountId, limit);
        return ResponseEntity.ok(account);
    }

    /**
     * Closes a customer account.
     * @param accountId ID of the account to close
     * @return Success message
     */
    @Operation(summary = "Close customer account",
              description = "Closes a customer's bank account")
    @DeleteMapping("/account/{accountId}")
    public ResponseEntity<String> closeAccount(@PathVariable Long accountId) {
        bankAccountService.closeAccount(accountId);
        return ResponseEntity.ok("Account closed successfully");
    }
}
