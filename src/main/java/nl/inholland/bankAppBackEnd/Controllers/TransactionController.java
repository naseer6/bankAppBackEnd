package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.DTOs.TransactionDTO;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String initiatedBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        // Create pageable object with page number, size, and sorting by timestamp (descending)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        // Let service handle all the logic and return paginated DTOs
        Page<TransactionDTO> transactionsPage = transactionService.getFilteredTransactionsWithDirection(
                currentUser, iban, ibanType, amount, comparator, start, end, pageable);
        
        // Create a response that includes pagination metadata
        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactionsPage.getContent());
        response.put("currentPage", transactionsPage.getNumber());
        response.put("totalItems", transactionsPage.getTotalElements());
        response.put("totalPages", transactionsPage.getTotalPages());
        
        return ResponseEntity.ok(response);
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
    public ResponseEntity<?> getMyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<TransactionDTO> transactionsPage = transactionService.getTransactionsWithDirectionByUser(currentUser, pageable);
        
        // Create a response that includes pagination metadata
        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactionsPage.getContent());
        response.put("currentPage", transactionsPage.getNumber());
        response.put("totalItems", transactionsPage.getTotalElements());
        response.put("totalPages", transactionsPage.getTotalPages());
        
        return ResponseEntity.ok(response);
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

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllTransactions(
            @RequestParam(required = false) String iban,
            @RequestParam(required = false) String ibanType,
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String comparator,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String initiatedBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        // Create pageable object with page number, size, and sorting by timestamp (descending)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        // Get all transactions with optional filtering and pagination
        Page<Transaction> transactionsPage = transactionService.getFilteredTransactions(
                iban, ibanType, amount, comparator, start, end, initiatedBy, pageable);
        
        // Convert all transactions to DTOs
        List<TransactionDTO> transactionDTOs = transactionsPage.getContent().stream()
                .map(tx -> transactionService.convertToAdminDTO(tx))
                .toList();
        
        // Create a response that includes pagination metadata
        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactionDTOs);
        response.put("currentPage", transactionsPage.getNumber());
        response.put("totalItems", transactionsPage.getTotalElements());
        response.put("totalPages", transactionsPage.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
}
