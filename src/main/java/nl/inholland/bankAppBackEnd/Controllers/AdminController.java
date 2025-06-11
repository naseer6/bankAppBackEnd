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
    @GetMapping("/accounts")
    public ResponseEntity<List<BankAccountDTO>> getAllAccounts() {
        List<BankAccountDTO> accountDTOs = bankAccountService.getAllAccountDTOs();
        return ResponseEntity.ok(accountDTOs);
    }

    /**
     * Get details for a specific account by ID
     * @param accountId The ID of the account to retrieve
     * @return Account details
     */
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<BankAccountDTO> getAccountDetails(@PathVariable Long accountId) {
        BankAccountDTO accountDTO = bankAccountService.getAccountDTOById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));
        return ResponseEntity.ok(accountDTO);
    }

    /**
     * Get all transactions for a specific account
     * @param accountId The ID of the account
     * @return List of transaction DTOs
     */
    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<TransactionDTO>> getAccountTransactions(@PathVariable Long accountId) {
        // Verify account exists
        if (!bankAccountService.accountExists(accountId)) {
            throw new ResourceNotFoundException("Account not found with ID: " + accountId);
        }

        List<TransactionDTO> transactionDTOs = transactionService.getTransactionDTOsByAccountId(accountId);
        return ResponseEntity.ok(transactionDTOs);
    }

    /**
     * Close a bank account
     * @param accountId The ID of the account to close
     * @return Success or failure message
     */
    @PostMapping("/accounts/{accountId}/close")
    public ResponseEntity<String> closeAccount(@PathVariable Long accountId) {
        try {
            bankAccountService.closeAccount(accountId);
            return ResponseEntity.ok("Account closed successfully.");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Get statistics for the admin dashboard
     * @return Dashboard statistics
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        DashboardStatsDTO stats = bankAccountService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }



}
