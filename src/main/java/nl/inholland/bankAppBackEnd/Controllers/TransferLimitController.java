package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
import nl.inholland.bankAppBackEnd.services.TransferLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/limits")
@PreAuthorize("hasRole('ADMIN')")
public class TransferLimitController {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private TransferLimitService transferLimitService;

    /**
     * Set limits for a specific account during or after account creation
     */
    @PostMapping("/account/{accountId}")
    public ResponseEntity<?> setAccountLimits(@PathVariable Long accountId,
                                              @RequestBody TransferLimitRequest request) {
        Optional<BankAccount> accountOpt = bankAccountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Account not found.");
        }

        BankAccount account = accountOpt.get();

        // Validate limits
        if (request.getAbsoluteLimit() < 0) {
            return ResponseEntity.badRequest().body("❌ Absolute limit cannot be negative.");
        }

        if (request.getDailyLimit() <= 0) {
            return ResponseEntity.badRequest().body("❌ Daily limit must be greater than zero.");
        }

        if (request.getAbsoluteLimit() > account.getBalance()) {
            return ResponseEntity.badRequest().body("❌ Absolute limit cannot be higher than current balance: €" +
                    String.format("%.2f", account.getBalance()));
        }

        // Update limits
        bankAccountService.updateLimits(account, request.getAbsoluteLimit(), request.getDailyLimit());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "✅ Transfer limits updated successfully");
        response.put("accountId", accountId);
        response.put("iban", account.getIban());
        response.put("absoluteLimit", account.getAbsoluteLimit());
        response.put("dailyLimit", account.getDailyLimit());
        response.put("currentBalance", account.getBalance());
        response.put("availableBalance", account.getAvailableBalance());

        return ResponseEntity.ok(response);
    }

    /**
     * Set limits for all accounts of a specific user
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<?> setUserAccountLimits(@PathVariable Long userId,
                                                  @RequestBody TransferLimitRequest request) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ User not found.");
        }

        User user = userOpt.get();
        List<BankAccount> accounts = bankAccountRepository.findAllByOwner(user);

        if (accounts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ No accounts found for this user.");
        }

        // Validate limits
        if (request.getAbsoluteLimit() < 0) {
            return ResponseEntity.badRequest().body("❌ Absolute limit cannot be negative.");
        }

        if (request.getDailyLimit() <= 0) {
            return ResponseEntity.badRequest().body("❌ Daily limit must be greater than zero.");
        }

        List<Map<String, Object>> updatedAccounts = new ArrayList<>();

        for (BankAccount account : accounts) {
            // Check if absolute limit is valid for this account
            if (request.getAbsoluteLimit() > account.getBalance()) {
                return ResponseEntity.badRequest().body("❌ Absolute limit cannot be higher than current balance for account " +
                        account.getIban() + ": €" + String.format("%.2f", account.getBalance()));
            }

            bankAccountService.updateLimits(account, request.getAbsoluteLimit(), request.getDailyLimit());

            Map<String, Object> accountInfo = new HashMap<>();
            accountInfo.put("accountId", account.getId());
            accountInfo.put("iban", account.getIban());
            accountInfo.put("type", account.getType());
            accountInfo.put("absoluteLimit", account.getAbsoluteLimit());
            accountInfo.put("dailyLimit", account.getDailyLimit());
            accountInfo.put("currentBalance", account.getBalance());
            accountInfo.put("availableBalance", account.getAvailableBalance());

            updatedAccounts.add(accountInfo);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "✅ Transfer limits updated for all user accounts");
        response.put("userId", userId);
        response.put("userName", user.getName());
        response.put("updatedAccounts", updatedAccounts);

        return ResponseEntity.ok(response);
    }

    /**
     * Create account with specific limits
     */
    @PostMapping("/create-account")
    public ResponseEntity<?> createAccountWithLimits(@RequestBody CreateAccountWithLimitsRequest request) {
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ User not found.");
        }

        User user = userOpt.get();

        if (!user.isApproved()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("❌ Cannot create account for unapproved user.");
        }

        // Validate limits
        if (request.getAbsoluteLimit() < 0) {
            return ResponseEntity.badRequest().body("❌ Absolute limit cannot be negative.");
        }

        if (request.getDailyLimit() <= 0) {
            return ResponseEntity.badRequest().body("❌ Daily limit must be greater than zero.");
        }

        BankAccount account = bankAccountService.createAccountWithLimits(
                user,
                request.getAccountType(),
                request.getAbsoluteLimit(),
                request.getDailyLimit()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("message", "✅ Account created successfully with custom limits");
        response.put("accountId", account.getId());
        response.put("iban", account.getIban());
        response.put("type", account.getType());
        response.put("absoluteLimit", account.getAbsoluteLimit());
        response.put("dailyLimit", account.getDailyLimit());
        response.put("balance", account.getBalance());

        return ResponseEntity.ok(response);
    }

    /**
     * Get transfer limits and daily usage for an account
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getAccountLimits(@PathVariable Long accountId) {
        Optional<BankAccount> accountOpt = bankAccountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Account not found.");
        }

        BankAccount account = accountOpt.get();
        Double remainingDaily = transferLimitService.getRemainingDailyLimit(account);
        Double totalTransferredToday = transferLimitService.getTotalTransferredToday(account, java.time.LocalDate.now());

        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("iban", account.getIban());
        response.put("ownerName", account.getOwner().getName());
        response.put("accountType", account.getType());
        response.put("currentBalance", account.getBalance());
        response.put("absoluteLimit", account.getAbsoluteLimit());
        response.put("availableBalance", account.getAvailableBalance());
        response.put("dailyLimit", account.getDailyLimit());
        response.put("totalTransferredToday", totalTransferredToday);
        response.put("remainingDailyLimit", remainingDaily);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all accounts with their limits for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserAccountLimits(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ User not found.");
        }

        User user = userOpt.get();
        List<BankAccount> accounts = bankAccountRepository.findAllByOwner(user);

        List<Map<String, Object>> accountsInfo = new ArrayList<>();

        for (BankAccount account : accounts) {
            Double remainingDaily = transferLimitService.getRemainingDailyLimit(account);
            Double totalTransferredToday = transferLimitService.getTotalTransferredToday(account, java.time.LocalDate.now());

            Map<String, Object> accountInfo = new HashMap<>();
            accountInfo.put("accountId", account.getId());
            accountInfo.put("iban", account.getIban());
            accountInfo.put("type", account.getType());
            accountInfo.put("currentBalance", account.getBalance());
            accountInfo.put("absoluteLimit", account.getAbsoluteLimit());
            accountInfo.put("availableBalance", account.getAvailableBalance());
            accountInfo.put("dailyLimit", account.getDailyLimit());
            accountInfo.put("totalTransferredToday", totalTransferredToday);
            accountInfo.put("remainingDailyLimit", remainingDaily);

            accountsInfo.add(accountInfo);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("userName", user.getName());
        response.put("accounts", accountsInfo);

        return ResponseEntity.ok(response);
    }

    // Request DTOs
    public static class TransferLimitRequest {
        private Double absoluteLimit;
        private Double dailyLimit;

        public Double getAbsoluteLimit() { return absoluteLimit; }
        public void setAbsoluteLimit(Double absoluteLimit) { this.absoluteLimit = absoluteLimit; }

        public Double getDailyLimit() { return dailyLimit; }
        public void setDailyLimit(Double dailyLimit) { this.dailyLimit = dailyLimit; }
    }

    public static class CreateAccountWithLimitsRequest {
        private Long userId;
        private BankAccount.AccountType accountType;
        private Double absoluteLimit;
        private Double dailyLimit;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public BankAccount.AccountType getAccountType() { return accountType; }
        public void setAccountType(BankAccount.AccountType accountType) { this.accountType = accountType; }

        public Double getAbsoluteLimit() { return absoluteLimit; }
        public void setAbsoluteLimit(Double absoluteLimit) { this.absoluteLimit = absoluteLimit; }

        public Double getDailyLimit() { return dailyLimit; }
        public void setDailyLimit(Double dailyLimit) { this.dailyLimit = dailyLimit; }
    }
}