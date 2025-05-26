package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
import nl.inholland.bankAppBackEnd.services.TransactionService;
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
    private UserRepository userRepository;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private TransactionService transactionService;


    @PostMapping("/approve/{userId}")
    public ResponseEntity<String> approveUser(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.getRole() == User.Role.USER && !user.isApproved()) {
                user.setApproved(true);
                userRepository.save(user);

                // 🏦 Create bank account after approval
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
        List<User> unapprovedUsers = userRepository.findAll()
                .stream()
                .filter(user -> !user.isApproved() && user.getRole() == User.Role.USER)
                .toList();

        return ResponseEntity.ok(unapprovedUsers);
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<BankAccount>> getAllAccounts() {
        List<BankAccount> accounts = bankAccountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<?> getAccountDetails(@PathVariable Long accountId) {
        Optional<BankAccount> accountOpt = bankAccountService.getAccountById(accountId);

        if (accountOpt.isPresent()) {
            return ResponseEntity.ok(accountOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found.");
        }
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<?> getAccountTransactions(@PathVariable Long accountId) {
        Optional<BankAccount> accountOpt = bankAccountService.getAccountById(accountId);

        if (accountOpt.isPresent()) {
            List<Transaction> transactions = transactionService.getTransactionsByAccountId(accountId);
            return ResponseEntity.ok(transactions);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found.");
        }
    }

    @PostMapping("/accounts/{accountId}/close")
    public ResponseEntity<?> closeAccount(@PathVariable Long accountId) {
        boolean success = bankAccountService.closeAccount(accountId);

        if (success) {
            return ResponseEntity.ok("Account closed successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to close account. Make sure the account exists and has zero balance.");
        }
    }

    // Additional endpoint for dashboard statistics
    @GetMapping("/dashboard-stats")
    public ResponseEntity<?> getDashboardStats() {
        int pendingApprovals = userRepository.findAll()
                .stream()
                .filter(user -> !user.isApproved() && user.getRole() == User.Role.USER)
                .toList()
                .size();

        int totalUsers = userRepository.findAll().size();
        int activeAccounts = bankAccountService.getActiveAccountsCount();
        int todayTransactions = transactionService.getTodayTransactionsCount();

        // Create a response object with all statistics
        var stats = new java.util.HashMap<String, Integer>();
        stats.put("pendingApprovals", pendingApprovals);
        stats.put("totalUsers", totalUsers);
        stats.put("activeAccounts", activeAccounts);
        stats.put("todayTransactions", todayTransactions);

        return ResponseEntity.ok(stats);
    }



}
