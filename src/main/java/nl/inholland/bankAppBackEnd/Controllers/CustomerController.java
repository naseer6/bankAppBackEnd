package nl.inholland.bankAppBackEnd.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.inholland.bankAppBackEnd.DTOs.AccountSearchResultDTO;
import nl.inholland.bankAppBackEnd.DTOs.TransactionDTO;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for customer operations including account viewing, transfers, and transaction history.
 * All endpoints are accessible to authenticated customers.
 */
@RestController
@RequestMapping("/api/customer")
@Tag(name = "Customer API", description = "Endpoints for customer operations")
public class CustomerController {

    private final UserService userService;
    private final BankAccountService bankAccountService;
    private final TransactionService transactionService;

    @Autowired
    public CustomerController(UserService userService, BankAccountService bankAccountService,
                             TransactionService transactionService) {
        this.userService = userService;
        this.bankAccountService = bankAccountService;
        this.transactionService = transactionService;
    }

    /**
     * Gets all accounts owned by the authenticated customer.
     * @param authentication Current authenticated user
     * @return List of customer's bank accounts
     */
    @Operation(summary = "Get customer accounts",
              description = "Returns all accounts owned by the authenticated customer")
    @GetMapping("/accounts")
    public ResponseEntity<List<BankAccount>> getCustomerAccounts(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<BankAccount> accounts = bankAccountService.getAccountsByUser(user);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Searches for customer IBANs by name.
     * @param name Name to search for
     * @return List of matching accounts with owner and IBAN information
     */
    @Operation(summary = "Search customer IBANs by name",
              description = "Returns IBANs for customers matching the provided name")
    @GetMapping("/search")
    public ResponseEntity<List<AccountSearchResultDTO>> searchCustomersByName(@RequestParam String name) {
        List<AccountSearchResultDTO> results = bankAccountService.searchAccountsByName(name);
        return ResponseEntity.ok(results);
    }

    /**
     * Transfers funds between customer's own accounts.
     * @param authentication Current authenticated user
     * @param transactionDTO Transfer details
     * @return Created transaction
     */
    @Operation(summary = "Transfer between own accounts",
              description = "Transfers funds between the customer's own accounts")
    @PostMapping("/transfer/internal")
    public ResponseEntity<Transaction> transferBetweenOwnAccounts(
            Authentication authentication,
            @RequestBody TransactionDTO transactionDTO) {

        User user = (User) authentication.getPrincipal();
        transactionDTO.setInitiatedBy(user.getUsername());

        // Validate that both accounts belong to the customer
        BankAccount fromAccount = bankAccountService.getAccountByIban(transactionDTO.getFromIban());
        BankAccount toAccount = bankAccountService.getAccountByIban(transactionDTO.getToIban());

        if (!fromAccount.getOwner().getId().equals(user.getId()) ||
            !toAccount.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Transaction transaction = transactionService.createTransaction(transactionDTO);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Transfers funds to another customer's account.
     * @param authentication Current authenticated user
     * @param transactionDTO Transfer details
     * @return Created transaction
     */
    @Operation(summary = "Transfer to another customer",
              description = "Transfers funds from customer's account to another customer's account")
    @PostMapping("/transfer/external")
    public ResponseEntity<Transaction> transferToExternalAccount(
            Authentication authentication,
            @RequestBody TransactionDTO transactionDTO) {

        User user = (User) authentication.getPrincipal();
        transactionDTO.setInitiatedBy(user.getUsername());

        // Validate that source account belongs to the customer
        BankAccount fromAccount = bankAccountService.getAccountByIban(transactionDTO.getFromIban());

        if (!fromAccount.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().build();
        }

        Transaction transaction = transactionService.createTransaction(transactionDTO);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Gets transaction history for the authenticated customer.
     * @param authentication Current authenticated user
     * @return List of customer's transactions
     */
    @Operation(summary = "Get transaction history",
              description = "Returns transaction history for the authenticated customer")
    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getTransactionHistory(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Transaction> transactions = transactionService.getTransactionsByUser(user);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Filters transactions by various criteria.
     * @param authentication Current authenticated user
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @param minAmount Optional minimum amount for filtering
     * @param maxAmount Optional maximum amount for filtering
     * @param iban Optional IBAN for filtering
     * @return Filtered list of transactions
     */
    @Operation(summary = "Filter transactions",
              description = "Returns filtered transactions based on provided criteria")
    @GetMapping("/transactions/filter")
    public ResponseEntity<List<Transaction>> filterTransactions(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String iban) {

        User user = (User) authentication.getPrincipal();
        List<Transaction> transactions = transactionService.filterTransactions(
                user, startDate, endDate, minAmount, maxAmount, iban);

        return ResponseEntity.ok(transactions);
    }

    /**
     * Gets account details for a specific account.
     * @param authentication Current authenticated user
     * @param accountId ID of the account to view
     * @return Account details if owned by the customer
     */
    @Operation(summary = "Get account details",
              description = "Returns details for a specific account owned by the customer")
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<BankAccount> getAccountDetails(
            Authentication authentication,
            @PathVariable Long accountId) {

        User user = (User) authentication.getPrincipal();
        BankAccount account = bankAccountService.getAccountById(accountId);

        // Verify ownership
        if (!account.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(account);
    }
}
