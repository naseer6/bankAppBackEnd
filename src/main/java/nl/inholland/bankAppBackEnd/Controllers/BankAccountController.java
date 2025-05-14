package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        account.setBalance(account.getBalance() + amount);
        bankAccountService.save(account);
        return ResponseEntity.ok("✅ Deposit successful to " + type + ". New balance: " + account.getBalance());
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

        if (account.getBalance() < amount) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("❌ Insufficient funds.");
        }

        account.setBalance(account.getBalance() - amount);
        bankAccountService.save(account);
        return ResponseEntity.ok("✅ Withdrawal successful from " + type + ". New balance: " + account.getBalance());
    }



    @GetMapping("/balance")
    public ResponseEntity<?> getBalances(@RequestParam Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ User not found.");
        }

        List<BankAccount> accounts = bankAccountRepository.findAllByOwner(userOpt.get());
        Map<String, Double> balances = accounts.stream()
                .collect(Collectors.toMap(
                        acc -> acc.getType().name().toLowerCase(),
                        BankAccount::getBalance
                ));

        return ResponseEntity.ok(balances); // e.g. { "checking": 100.0, "savings": 200.0 }
    }



}

