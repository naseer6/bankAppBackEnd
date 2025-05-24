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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
public class BankAccountController {

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransferLimitService transferLimitService;

    @PostMapping("/create")
    public Object createAccount(@RequestParam Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            return "❌ User not found.";
        }

        User user = userOpt.get();

        // Check if user is approved
        if (!user.isApproved()) {
            return "❌ Account creation is only allowed for approved users.";
        }

        if (!user.getRole().equals(User.Role.USER)) {
            return "❌ Account can only be created for users with USER role.";
        }

        // If the user is approved, create the bank account
        BankAccount account = bankAccountService.createAccountForUser(user);
        return account;
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestParam Long userId,
                                     @RequestParam double amount,
                                     @RequestParam BankAccount.AccountType type) {
        if (amount <= 0) {
            return ResponseEntity.badRequest().body("❌ Deposit amount must be greater than zero.");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ User not found.");
        }

        User owner = userOpt.get();
        Optional<BankAccount> accOpt = bankAccountRepository.findByOwnerAndType(owner, type);
        if (accOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Bank account not found.");
        }

        BankAccount account = accOpt.get();

        // For deposits, we only check daily limits (not absolute limits since it's adding money)
        TransferLimitService.TransferValidationResult validation =
                transferLimitService.validateTransfer(account, amount, false);

        if (!validation.isValid()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validation.getErrorMessage());
        }

        // Process deposit
        account.setBalance(account.getBalance() + amount);
        bankAccountService.save(account);

        // Record the transfer for daily tracking
        transferLimitService.recordTransfer(account, amount);

        return ResponseEntity.ok("✅ Deposit successful to " + type + ". New balance: €" +
                String.format("%.2f", account.getBalance()));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestParam Long userId,
                                      @RequestParam double amount,
                                      @RequestParam BankAccount.AccountType type) {
        if (amount <= 0) {
            return ResponseEntity.badRequest().body("❌ Withdrawal amount must be greater than zero.");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ User not found.");
        }

        User owner = userOpt.get();
        Optional<BankAccount> accOpt = bankAccountRepository.findByOwnerAndType(owner, type);
        if (accOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Bank account not found.");
        }

        BankAccount account = accOpt.get();

        // Basic balance check
        if (account.getBalance() < amount) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("❌ Insufficient funds.");
        }

        // Check both absolute and daily limits
        TransferLimitService.TransferValidationResult validation =
                transferLimitService.validateTransfer(account, amount, true);

        if (!validation.isValid()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validation.getErrorMessage());
        }

        // Process withdrawal
        account.setBalance(account.getBalance() - amount);
        bankAccountService.save(account);

        // Record the transfer for daily tracking
        transferLimitService.recordTransfer(account, amount);

        return ResponseEntity.ok("✅ Withdrawal successful from " + type + ". New balance: €" +
                String.format("%.2f", account.getBalance()) +
                ". Available balance: €" + String.format("%.2f", account.getAvailableBalance()));
    }

    @PostMapping("/{accountId}/limits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateLimits(@PathVariable Long accountId,
                                          @RequestParam Double absoluteLimit,
                                          @RequestParam Double dailyLimit) {
        Optional<BankAccount> accountOpt = bankAccountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Account not found.");
        }

        BankAccount account = accountOpt.get();

        // Validate limits
        if (absoluteLimit < 0) {
            return ResponseEntity.badRequest().body("❌ Absolute limit cannot be negative.");
        }

        if (dailyLimit <= 0) {
            return ResponseEntity.badRequest().body("❌ Daily limit must be greater than zero.");
        }

        if (absoluteLimit > account.getBalance()) {
            return ResponseEntity.badRequest().body("❌ Absolute limit cannot be higher than current balance.");
        }

        account.setAbsoluteLimit(absoluteLimit);
        account.setDailyLimit(dailyLimit);
        bankAccountService.save(account);

        return ResponseEntity.ok("✅ Limits updated successfully. Absolute limit: €" +
                String.format("%.2f", absoluteLimit) + ", Daily limit: €" +
                String.format("%.2f", dailyLimit));
    }

    @GetMapping("/{accountId}/limits")
    public ResponseEntity<?> getLimits(@PathVariable Long accountId) {
        Optional<BankAccount> accountOpt = bankAccountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Account not found.");
        }

        BankAccount account = accountOpt.get();
        Double remainingDaily = transferLimitService.getRemainingDailyLimit(account);

        Map<String, Object> limits = new HashMap<>();
        limits.put("absoluteLimit", account.getAbsoluteLimit());
        limits.put("dailyLimit", account.getDailyLimit());
        limits.put("remainingDailyLimit", remainingDaily);
        limits.put("availableBalance", account.getAvailableBalance());
        limits.put("currentBalance", account.getBalance());

        return ResponseEntity.ok(limits);
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalances(@RequestParam Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ User not found.");
        }

        List<BankAccount> accounts = bankAccountRepository.findAllByOwner(userOpt.get());
        Map<String, Object> balances = accounts.stream()
                .collect(Collectors.toMap(
                        acc -> acc.getType().name().toLowerCase(),
                        acc -> {
                            Map<String, Object> accountInfo = new HashMap<>();
                            accountInfo.put("balance", acc.getBalance());
                            accountInfo.put("availableBalance", acc.getAvailableBalance());
                            accountInfo.put("absoluteLimit", acc.getAbsoluteLimit());
                            accountInfo.put("dailyLimit", acc.getDailyLimit());
                            accountInfo.put("remainingDailyLimit", transferLimitService.getRemainingDailyLimit(acc));
                            return accountInfo;
                        }
                ));

        return ResponseEntity.ok(balances);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getAccountsByUserId(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ User not found.");
        }

        List<BankAccount> accounts = bankAccountRepository.findAllByOwner(userOpt.get());

        if (accounts.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Map<String, Object>> result = accounts.stream().map(acc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", acc.getId());
            map.put("iban", acc.getIban());
            map.put("balance", acc.getBalance());
            map.put("type", acc.getType());
            map.put("absoluteLimit", acc.getAbsoluteLimit());
            map.put("dailyLimit", acc.getDailyLimit());
            map.put("availableBalance", acc.getAvailableBalance());
            map.put("remainingDailyLimit", transferLimitService.getRemainingDailyLimit(acc));
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/find-by-name")
    public ResponseEntity<?> findAccountsByOwnerName(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Name parameter cannot be empty.");
        }

        List<User> matchingUsers = userRepository.findByNameContainingIgnoreCase(name);

        if (matchingUsers.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ No users found with name: " + name);
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (User user : matchingUsers) {
            List<BankAccount> userAccounts = bankAccountRepository.findAllByOwner(user);

            for (BankAccount account : userAccounts) {
                Map<String, Object> accountDetails = new HashMap<>();
                accountDetails.put("ownerName", user.getName());
                accountDetails.put("iban", account.getIban());
                accountDetails.put("accountType", account.getType().toString());
                accountDetails.put("absoluteLimit", account.getAbsoluteLimit());
                accountDetails.put("dailyLimit", account.getDailyLimit());

                results.add(accountDetails);
            }
        }

        if (results.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ No accounts found for users with name: " + name);
        }

        return ResponseEntity.ok(results);
    }
}