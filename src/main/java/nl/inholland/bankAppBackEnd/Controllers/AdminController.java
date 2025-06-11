package nl.inholland.bankAppBackEnd.Controllers;

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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {


    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private TransactionService transactionService;

    private UserService userService;

    @Autowired
    public AdminController(UserService userService, BankAccountService bankAccountService, TransactionService transactionService) {
        this.userService = userService;
        this.bankAccountService = bankAccountService;
        this.transactionService = transactionService;
    }

    @PostMapping("/approve/{userId}")
    public ResponseEntity<String> approveUser(@PathVariable Long userId) {
        Optional<User> userOpt = userService.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.getRole() == User.Role.USER && !user.isApproved()) {
                userService.approveUser(user);
                bankAccountService.createAccountsForUser(user);
                return ResponseEntity.ok("User approved and bank account created.");
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("User is already approved or not eligible.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
    }

    @GetMapping("/unapproved-users")
    public ResponseEntity<?> getUnapprovedUsers() {
        List<User> unapprovedUsers = userService.findUnapprovedUsers();
        return ResponseEntity.ok(unapprovedUsers);
    }





    /**
     * Get all bank accounts in the system
     * @return List of bank account DTOs
     */
    @GetMapping("/accounts")  // Maps HTTP GET requests to /accounts URL to this method
    public ResponseEntity<List<BankAccountDTO>> getAllAccounts() {  // Method that returns a list of account DTOs wrapped in ResponseEntity
        List<BankAccountDTO> accountDTOs = bankAccountService.getAllAccountDTOs();  // Call service layer to get all accounts as DTOs
        return ResponseEntity.ok(accountDTOs);  // Return HTTP 200 OK status with the list of accounts in the response body
    }

    /**
     * Get details for a specific account by ID
     * @param accountId The ID of the account to retrieve
     * @return Account details
     */
    @GetMapping("/accounts/{accountId}")  // Maps GET requests to /accounts/[some-number] - {accountId} captures the number from URL
    public ResponseEntity<BankAccountDTO> getAccountDetails(@PathVariable Long accountId) {  // @PathVariable extracts accountId from URL path
        BankAccountDTO accountDTO = bankAccountService.getAccountDTOById(accountId)  // Call service to find account by ID (returns Optional)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));  // If Optional is empty, throw custom exception
        return ResponseEntity.ok(accountDTO);  // Return HTTP 200 OK status with the found account in response body
    }

    /**
     * Get all transactions for a specific account
     * @param accountId The ID of the account
     * @return List of transaction DTOs
     */
    @GetMapping("/accounts/{accountId}/transactions")  // Maps GET requests to /accounts/[number]/transactions URL pattern
    public ResponseEntity<List<TransactionDTO>> getAccountTransactions(@PathVariable Long accountId) {  // Extract accountId from URL path
        // Verify account exists
        if (!bankAccountService.accountExists(accountId)) {  // Call service method to check if account exists in database
            throw new ResourceNotFoundException("Account not found with ID: " + accountId);  // If account doesn't exist, throw exception
        }

        List<TransactionDTO> transactionDTOs = transactionService.getTransactionDTOsByAccountId(accountId);  // Call transaction service to get all transactions for this account
        return ResponseEntity.ok(transactionDTOs);  // Return HTTP 200 OK with list of transactions in response body
    }

    /**
     * Close a bank account
     * @param accountId The ID of the account to close
     * @return Success or failure message
     */
    @PostMapping("/accounts/{accountId}/close")  // Maps HTTP POST requests to /accounts/[number]/close - POST used for actions that change data
    public ResponseEntity<String> closeAccount(@PathVariable Long accountId) {  // Method returns String message wrapped in ResponseEntity
        try {  // Start try block to handle potential exceptions
            bankAccountService.closeAccount(accountId);  // Call service method to close the account (may throw exceptions)
            return ResponseEntity.ok("Account closed successfully.");  // If no exception thrown, return HTTP 200 OK with success message
        } catch (ResourceNotFoundException e) {  // Catch exception if account doesn't exist
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());  // Return HTTP 404 Not Found with error message
        } catch (IllegalStateException e) {  // Catch exception if account can't be closed (e.g., has remaining balance)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());  // Return HTTP 400 Bad Request with error message
        }
    }

    /**
     * Get statistics for the admin dashboard
     * @return Dashboard statistics
     */
    @GetMapping("/dashboard-stats")  // Maps HTTP GET requests to /dashboard-stats URL to this method
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {  // Method returns dashboard statistics wrapped in ResponseEntity
        DashboardStatsDTO stats = bankAccountService.getDashboardStats();  // Call service to calculate and return dashboard statistics
        return ResponseEntity.ok(stats);  // Return HTTP 200 OK status with statistics object in response body
    }



}
