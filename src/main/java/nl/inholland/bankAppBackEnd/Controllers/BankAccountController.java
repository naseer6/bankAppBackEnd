package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.DTOs.AccountSearchResultDTO;
import nl.inholland.bankAppBackEnd.exceptions.ResourceNotFoundException;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private UserService userService;

    @Autowired
    private BankAccountRepository bankAccountRepository;
    @Autowired
    private TransactionService transactionService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userService.getUserByUsername(auth.getName()).orElse(null);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createAccount(@RequestParam Long userId,
                                           @RequestParam(required = false, defaultValue = "0.0") Double absoluteLimit,
                                           @RequestParam(required = false, defaultValue = "1000.0") Double dailyLimit) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ Not authenticated");
        }

        // Only admins can create accounts for others
        if (currentUser.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ Only administrators can create accounts");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ User not found");
        }

        User user = userOpt.get();

        // Check if user is approved
        if (!user.isApproved()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("❌ Account creation is only allowed for approved users");
        }

        if (!user.getRole().equals(User.Role.USER)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("❌ Account can only be created for users with USER role");
        }

        // Create accounts with specified limits
        List<BankAccount> accounts = bankAccountService.createAccountsForUserWithLimits(user, absoluteLimit, dailyLimit);
        return ResponseEntity.ok(accounts);
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transferFunds(@RequestParam String fromIban,
                                           @RequestParam String toIban,
                                           @RequestParam Double amount) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ Not authenticated");
        }

        TransactionService.TransferResult result = transactionService.transferFunds(fromIban, toIban, amount, currentUser);

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.getMessage(),
                    "transaction", result.getTransaction()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", result.getMessage()
            ));
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestParam String iban,
                                     @RequestParam Double amount) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ Not authenticated");
        }

        TransactionService.TransferResult result = transactionService.deposit(iban, amount, currentUser);

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.getMessage(),
                    "transaction", result.getTransaction()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", result.getMessage()
            ));
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestParam String iban,
                                      @RequestParam Double amount) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ Not authenticated");
        }

        TransactionService.TransferResult result = transactionService.withdraw(iban, amount, currentUser);

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.getMessage(),
                    "transaction", result.getTransaction()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", result.getMessage()
            ));
        }
    }



    @PostMapping("/update-limits")
    public ResponseEntity<?> updateLimits(@RequestParam String iban,
                                          @RequestParam(required = false) Double absoluteLimit,
                                          @RequestParam(required = false) Double dailyLimit) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ Not authenticated");
        }

        // Only admins can update limits
        if (currentUser.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ Only administrators can set account limits");
        }

        TransactionService.TransferResult result = transactionService.updateAccountLimits(iban, absoluteLimit, dailyLimit, currentUser);

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.getMessage()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", result.getMessage()
            ));
        }
    }





    @GetMapping("/balance")
    public ResponseEntity<?> getBalances(@RequestParam Long userId) {
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ User not found");
        }

        List<BankAccount> accounts = bankAccountService.getAccountsByOwner(userOpt.get());
        Map<String, Double> balances = accounts.stream()
                .collect(Collectors.toMap(
                        acc -> acc.getType().name().toLowerCase(),
                        BankAccount::getBalance
                ));

        return ResponseEntity.ok(balances);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getAccountsByUserId(@PathVariable Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ User not found");
        }

        List<BankAccount> accounts = bankAccountRepository.findAllByOwner(userOpt.get());

        if (accounts.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Enhanced account details including limits
        List<Map<String, Object>> result = accounts.stream().map(acc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", acc.getId());
            map.put("iban", acc.getIban());
            map.put("balance", acc.getBalance());
            map.put("type", acc.getType());
            map.put("absoluteLimit", acc.getAbsoluteLimit());
            map.put("dailyLimit", acc.getDailyLimit());
            map.put("dailySpent", acc.getDailySpent());
            map.put("remainingDailyLimit", acc.getRemainingDailyLimit());
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/find-by-name")
    public ResponseEntity<?> findAccountsByOwnerName(@RequestParam String name) {
        try {
            List<AccountSearchResultDTO> results = bankAccountService.findAccountsByOwnerName(name);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("❌ " + e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ " + e.getMessage());
        }
    }

    @GetMapping("/search-iban")
    public ResponseEntity<?> searchByIban(@RequestParam String iban) {
        if (iban == null || iban.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ IBAN parameter cannot be empty");
        }

        List<BankAccount> accounts = bankAccountRepository.findByIbanContainingIgnoreCase(iban);

        if (accounts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ No accounts found with IBAN: " + iban);
        }

        List<Map<String, Object>> results = accounts.stream().map(account -> {
            Map<String, Object> accountDetails = new HashMap<>();
            accountDetails.put("iban", account.getIban());
            accountDetails.put("ownerName", account.getOwner().getName());
            accountDetails.put("accountType", account.getType().toString());
            return accountDetails;
        }).toList();

        return ResponseEntity.ok(results);
    }

    @GetMapping("/limits/{iban}")
    public ResponseEntity<?> getAccountLimits(@PathVariable String iban) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ Not authenticated");
        }

        Optional<BankAccount> accountOpt = bankAccountRepository.findByIban(iban);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Account not found");
        }

        BankAccount account = accountOpt.get();

        // Users can only view their own account limits, admins can view any
        if (currentUser.getRole() == User.Role.USER && !account.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ You can only view your own account limits");
        }

        Map<String, Object> limits = new HashMap<>();
        limits.put("iban", account.getIban());
        limits.put("absoluteLimit", account.getAbsoluteLimit());
        limits.put("dailyLimit", account.getDailyLimit());
        limits.put("dailySpent", account.getDailySpent());
        limits.put("remainingDailyLimit", account.getRemainingDailyLimit());
        limits.put("balance", account.getBalance());
        limits.put("availableBalance", Math.max(0, account.getBalance() - account.getAbsoluteLimit()));

        return ResponseEntity.ok(limits);
    }
}