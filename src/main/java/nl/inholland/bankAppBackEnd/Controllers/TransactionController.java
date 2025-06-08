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
        
        return createPaginatedResponse(transactionsPage);
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
        
        return createPaginatedResponse(transactionsPage);
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

    // Consolidated helper methods for transfer endpoints
    private User validateUser(boolean requiresAdmin, boolean requiresApproval) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return null;
        }
        
        if (requiresAdmin && currentUser.getRole() != User.Role.ADMIN) {
            return null;
        }
        
        if (requiresApproval && currentUser.getRole() == User.Role.USER && !currentUser.isApproved()) {
            return null;
        }
        
        return currentUser;
    }
    
    private ResponseEntity<?> handleTransferRequest(Map<String, Object> requestBody, String transferType) {
        boolean requiresAdmin = "admin".equals(transferType);
        boolean requiresApproval = !"deposit".equals(transferType);
        
        User currentUser = validateUser(requiresAdmin, requiresApproval);
        if (currentUser == null) {
            String message = requiresAdmin ? 
                    "❌ Only administrators can perform admin transfers" :
                    (requiresApproval ? "❌ Your account is not yet approved for transfers" : "❌ Not authenticated");
            
            return ResponseEntity.status(currentUser == null ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", message));
        }
        
        try {
            String fromIban = null;
            String toIban = null;
            Double amount = null;
            
            // Extract parameters based on transfer type
            if (transferType.equals("deposit")) {
                toIban = (String) requestBody.get("iban");
                amount = Double.valueOf(requestBody.get("amount").toString());
            } else if (transferType.equals("withdraw")) {
                fromIban = (String) requestBody.get("iban");
                amount = Double.valueOf(requestBody.get("amount").toString());
            } else {
                fromIban = (String) requestBody.get("fromIban");
                toIban = (String) requestBody.get("toIban");
                amount = Double.valueOf(requestBody.get("amount").toString());
            }
            
            // Validate required parameters
            if ((transferType.equals("deposit") && (toIban == null || amount == null)) ||
                (transferType.equals("withdraw") && (fromIban == null || amount == null)) ||
                (!transferType.equals("deposit") && !transferType.equals("withdraw") && 
                 (fromIban == null || toIban == null || amount == null))) {
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "message", "❌ Missing required fields")
                );
            }
            
            TransactionService.TransferResult result;
            
            // Call appropriate service method based on transfer type
            switch (transferType) {
                case "deposit":
                    result = transactionService.deposit(toIban, amount, currentUser);
                    break;
                case "withdraw":
                    result = transactionService.withdraw(fromIban, amount, currentUser);
                    break;
                case "internal":
                    result = transactionService.internalTransfer(fromIban, toIban, amount, currentUser);
                    break;
                default: // admin or regular transfer
                    result = transactionService.transferFunds(fromIban, toIban, amount, currentUser);
                    break;
            }
            
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
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Invalid request format: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/customer")
    public ResponseEntity<?> customerTransfer(@RequestBody Map<String, Object> requestBody) {
        return handleTransferRequest(requestBody, "regular");
    }

    @PostMapping("/admin")
    public ResponseEntity<?> adminTransfer(@RequestBody Map<String, Object> requestBody) {
        return handleTransferRequest(requestBody, "admin");
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> makeDeposit(@RequestBody Map<String, Object> requestBody) {
        return handleTransferRequest(requestBody, "deposit");
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> makeWithdrawal(@RequestBody Map<String, Object> requestBody) {
        return handleTransferRequest(requestBody, "withdraw");
    }

    @PostMapping("/internal")
    public ResponseEntity<?> internalTransfer(@RequestBody Map<String, Object> requestBody) {
        return handleTransferRequest(requestBody, "internal");
    }

    private ResponseEntity<?> createPaginatedResponse(Page<TransactionDTO> transactionsPage) {
        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactionsPage.getContent());
        response.put("currentPage", transactionsPage.getNumber());
        response.put("totalItems", transactionsPage.getTotalElements());
        response.put("totalPages", transactionsPage.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
}
