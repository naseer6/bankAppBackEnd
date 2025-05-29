package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.DTOs.TransactionDTO;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userService.getUserByUsername(auth.getName()).orElse(null);
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestBody Transaction transaction) {
        Transaction saved = transactionService.save(transaction);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<?> getFilteredTransactions(
            @RequestParam(required = false) String iban,
            @RequestParam(required = false) String ibanType,
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String comparator,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end
    ) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        // Let service handle all the logic and return DTOs
        List<TransactionDTO> transactions = transactionService.getFilteredTransactionsWithDirection(
                currentUser, iban, ibanType, amount, comparator, start, end);

        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransactionById(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        TransactionDTO transaction = transactionService.getTransactionWithDirectionById(id, currentUser);
        if (transaction == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/my-transactions")
    public ResponseEntity<?> getMyTransactions() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        List<TransactionDTO> transactions = transactionService.getTransactionsWithDirectionByUser(currentUser);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/user-ibans")
    public ResponseEntity<?> getUserIbans() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        List<String> userIbans = transactionService.getUserIbans(currentUser);
        return ResponseEntity.ok(Map.of("ibans", userIbans));
    }
}
