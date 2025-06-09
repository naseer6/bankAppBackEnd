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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    // ===================== AUTHENTICATION & AUTHORIZATION =====================

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userService.getUserByUsername(auth.getName()).orElse(null);
    }

    private ResponseEntity<?> withAuthenticatedUser(java.util.function.Function<User, ResponseEntity<?>> action) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return createErrorResponse(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return action.apply(currentUser);
    }

    private ResponseEntity<?> withApprovedUser(java.util.function.Function<User, ResponseEntity<?>> action) {
        return withAuthenticatedUser(user -> {
            if (!user.isApproved()) {
                return createErrorResponse(HttpStatus.FORBIDDEN, "❌ Your account is not yet approved for transfers");
            }
            return action.apply(user);
        });
    }

    private ResponseEntity<?> withAdminUser(java.util.function.Function<User, ResponseEntity<?>> action) {
        return withAuthenticatedUser(user -> {
            if (user.getRole() != User.Role.ADMIN) {
                return createErrorResponse(HttpStatus.FORBIDDEN, "❌ Only administrators can perform this action");
            }
            return action.apply(user);
        });
    }

    // ===================== RESPONSE HELPERS =====================

    private ResponseEntity<?> createErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }

    private ResponseEntity<?> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        if (data != null) {
            if (data instanceof Map) {
                response.putAll((Map<String, Object>) data);
            } else {
                response.put("data", data);
            }
        }
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> createPaginatedResponse(Page<TransactionDTO> transactionsPage) {
        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactionsPage.getContent());
        response.put("currentPage", transactionsPage.getNumber());
        response.put("totalItems", transactionsPage.getTotalElements());
        response.put("totalPages", transactionsPage.getTotalPages());
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> handleServiceResult(Supplier<TransactionService.TransferResult> serviceCall) {
        try {
            TransactionService.TransferResult result = serviceCall.get();
            if (result.isSuccess()) {
                return createSuccessResponse(result.getMessage(),
                        Map.of("transaction", result.getTransaction()));
            } else {
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "message", result.getMessage()));
            }
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, "❌ Invalid request: " + e.getMessage());
        }
    }

    private ResponseEntity<?> handleATMResult(Supplier<TransactionService.ATMResult> serviceCall) {
        try {
            TransactionService.ATMResult result = serviceCall.get();
            if (result.isSuccess()) {
                return createSuccessResponse(result.getMessage(), createATMResponseData(result));
            } else {
                return createErrorResponse(HttpStatus.BAD_REQUEST, result.getMessage());
            }
        } catch (Exception e) {
            // Improve error logging and messaging with specific error detection
            e.printStackTrace(); // Log the full stack trace
            String errorMessage;
            
            // Check for specific error types and provide more helpful messages
            if (e.getMessage() != null) {
                if (e.getMessage().contains("absolute limit")) {
                    errorMessage = "❌ Transaction would exceed your account's minimum balance limit";
                } else if (e.getMessage().contains("daily limit")) {
                    errorMessage = "❌ You've reached your daily transaction limit for this account";
                } else if (e.getMessage().contains("CHECKING") || e.getMessage().toLowerCase().contains("savings")) {
                    errorMessage = "❌ This transaction is not allowed for this account type";
                } else {
                    errorMessage = "❌ " + e.getMessage();
                }
            } else {
                // Default message with current timestamp for tracing
                errorMessage = "❌ Transaction failed. Please try again or contact support. (Error ID: " + 
                               System.currentTimeMillis() % 10000 + ")";
            }
            
            return createErrorResponse(HttpStatus.BAD_REQUEST, errorMessage);
        }
    }

    private Map<String, Object> createATMResponseData(TransactionService.ATMResult result) {
        Map<String, Object> data = new HashMap<>();
        if (result.getNewBalance() != null) {
            data.put("newBalance", result.getNewBalance());
        }
        if (result.getRemainingDailyLimit() != null) {
            data.put("remainingDailyLimit", result.getRemainingDailyLimit());
        }
        if (result.getAvailableBalance() != null) {
            data.put("availableBalance", result.getAvailableBalance());
        }
        if (result.getTransaction() != null) {
            data.put("transaction", result.getTransaction());
        }
        return data;
    }

    // ===================== VALIDATION HELPERS =====================

    private ResponseEntity<?> validateTransferRequest(Map<String, Object> requestBody,
                                                      String... requiredFields) {
        for (String field : requiredFields) {
            if (!requestBody.containsKey(field) || requestBody.get(field) == null) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "❌ Missing required field: " + field);
            }
        }
        return null; // No error
    }

    private Double parseAmount(Object amountObj) throws NumberFormatException {
        return Double.valueOf(amountObj.toString());
    }

    // ===================== TRANSACTION ENDPOINTS =====================

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
            @RequestParam(defaultValue = "10") int size) {

        return withAuthenticatedUser(user -> {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<TransactionDTO> transactionsPage = transactionService.getFilteredTransactionsWithDirection(
                    user, iban, ibanType, amount, comparator, start, end, pageable);
            return createPaginatedResponse(transactionsPage);
        });
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransactionById(@PathVariable Long id) {
        return withAuthenticatedUser(user -> {
            TransactionDTO transaction = transactionService.getTransactionWithDirectionById(id, user);
            return transaction != null ? ResponseEntity.ok(transaction) : ResponseEntity.notFound().build();
        });
    }

    @GetMapping("/my-transactions")
    public ResponseEntity<?> getMyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return withAuthenticatedUser(user -> {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<TransactionDTO> transactionsPage = transactionService.getTransactionsWithDirectionByUser(user, pageable);
            return createPaginatedResponse(transactionsPage);
        });
    }

    @GetMapping("/user-ibans")
    public ResponseEntity<?> getUserIbans() {
        return withAuthenticatedUser(user -> {
            List<String> userIbans = transactionService.getUserIbans(user);
            return ResponseEntity.ok(Map.of("ibans", userIbans));
        });
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
            @RequestParam(defaultValue = "10") int size) {

        return withAuthenticatedUser(user -> {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<Transaction> transactionsPage = transactionService.getFilteredTransactions(
                    iban, ibanType, amount, comparator, start, end, initiatedBy, pageable);

            List<TransactionDTO> transactionDTOs = transactionsPage.getContent().stream()
                    .map(transactionService::convertToAdminDTO)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("transactions", transactionDTOs);
            response.put("currentPage", transactionsPage.getNumber());
            response.put("totalItems", transactionsPage.getTotalElements());
            response.put("totalPages", transactionsPage.getTotalPages());

            return ResponseEntity.ok(response);
        });
    }

    // ===================== TRANSFER ENDPOINTS =====================

    @PostMapping("/customer")
    public ResponseEntity<?> customerTransfer(@RequestBody Map<String, Object> requestBody) {
        return withApprovedUser(user -> {
            ResponseEntity<?> validationError = validateTransferRequest(requestBody, "fromIban", "toIban", "amount");
            if (validationError != null) return validationError;

            try {
                String fromIban = (String) requestBody.get("fromIban");
                String toIban = (String) requestBody.get("toIban");
                Double amount = parseAmount(requestBody.get("amount"));

                return handleServiceResult(() ->
                        transactionService.transferFunds(fromIban, toIban, amount, user));
            } catch (NumberFormatException e) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "❌ Invalid amount format");
            }
        });
    }

    @PostMapping("/admin")
    public ResponseEntity<?> adminTransfer(@RequestBody Map<String, Object> requestBody) {
        return withAdminUser(user -> {
            ResponseEntity<?> validationError = validateTransferRequest(requestBody, "fromIban", "toIban", "amount");
            if (validationError != null) return validationError;

            try {
                String fromIban = (String) requestBody.get("fromIban");
                String toIban = (String) requestBody.get("toIban");
                Double amount = parseAmount(requestBody.get("amount"));

                return handleServiceResult(() ->
                        transactionService.transferFunds(fromIban, toIban, amount, user));
            } catch (NumberFormatException e) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "❌ Invalid amount format");
            }
        });
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> makeDeposit(@RequestBody Map<String, Object> requestBody) {
        return withAuthenticatedUser(user -> {
            ResponseEntity<?> validationError = validateTransferRequest(requestBody, "iban", "amount");
            if (validationError != null) return validationError;

            try {
                String iban = (String) requestBody.get("iban");
                Double amount = parseAmount(requestBody.get("amount"));

                return handleServiceResult(() ->
                        transactionService.deposit(iban, amount, user));
            } catch (NumberFormatException e) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "❌ Invalid amount format");
            }
        });
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> makeWithdrawal(@RequestBody Map<String, Object> requestBody) {
        return withApprovedUser(user -> {
            ResponseEntity<?> validationError = validateTransferRequest(requestBody, "iban", "amount");
            if (validationError != null) return validationError;

            try {
                String iban = (String) requestBody.get("iban");
                Double amount = parseAmount(requestBody.get("amount"));

                return handleServiceResult(() ->
                        transactionService.withdraw(iban, amount, user));
            } catch (NumberFormatException e) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "❌ Invalid amount format");
            }
        });
    }

    @PostMapping("/internal")
    public ResponseEntity<?> internalTransfer(@RequestBody Map<String, Object> requestBody) {
        return withApprovedUser(user -> {
            ResponseEntity<?> validationError = validateTransferRequest(requestBody, "fromIban", "toIban", "amount");
            if (validationError != null) return validationError;

            try {
                String fromIban = (String) requestBody.get("fromIban");
                String toIban = (String) requestBody.get("toIban");
                Double amount = parseAmount(requestBody.get("amount"));

                return handleServiceResult(() ->
                        transactionService.internalTransfer(fromIban, toIban, amount, user));
            } catch (NumberFormatException e) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "❌ Invalid amount format");
            }
        });
    }

    // ===================== ATM ENDPOINTS =====================

    @PostMapping("/atm/withdraw")
    public ResponseEntity<?> atmWithdraw(@RequestBody Map<String, Object> requestBody) {
        return withAuthenticatedUser(user -> {
            ResponseEntity<?> validationError = validateTransferRequest(requestBody, "iban", "amount");
            if (validationError != null) return validationError;

            try {
                String iban = (String) requestBody.get("iban");
                Double amount = parseAmount(requestBody.get("amount"));

                return handleATMResult(() ->
                        transactionService.atmWithdraw(iban, amount, user));
            } catch (NumberFormatException e) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "❌ Invalid amount format");
            }
        });
    }

    @PostMapping("/atm/deposit")
    public ResponseEntity<?> atmDeposit(@RequestBody Map<String, Object> requestBody) {
        return withAuthenticatedUser(user -> {
            ResponseEntity<?> validationError = validateTransferRequest(requestBody, "iban", "amount");
            if (validationError != null) return validationError;

            try {
                String iban = (String) requestBody.get("iban");
                Double amount = parseAmount(requestBody.get("amount"));

                return handleATMResult(() ->
                        transactionService.atmDeposit(iban, amount, user));
            } catch (NumberFormatException e) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "❌ Invalid amount format");
            }
        });
    }

    @PostMapping("/atm/transfer")
    public ResponseEntity<?> atmTransfer(@RequestBody Map<String, Object> requestBody) {
        return withApprovedUser(user -> {
            ResponseEntity<?> validationError = validateTransferRequest(requestBody, "fromIban", "toIban", "amount");
            if (validationError != null) return validationError;

            try {
                String fromIban = (String) requestBody.get("fromIban");
                String toIban = (String) requestBody.get("toIban");
                Double amount = parseAmount(requestBody.get("amount"));

                return handleATMResult(() ->
                        transactionService.atmTransfer(fromIban, toIban, amount, user));
            } catch (NumberFormatException e) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "❌ Invalid amount format");
            }
        });
    }

    @GetMapping("/atm/account-summary/{iban}")
    public ResponseEntity<?> getAccountSummary(@PathVariable String iban) {
        return withAuthenticatedUser(user ->
                handleATMResult(() -> transactionService.getAccountSummary(iban, user))
        );
    }

    @GetMapping("/atm/recent-transactions")
    public ResponseEntity<?> getRecentTransactions(@RequestParam(defaultValue = "5") int limit) {
        return withAuthenticatedUser(user -> {
            try {
                List<Transaction> transactions = transactionService.getRecentATMTransactions(user, limit);
                return createSuccessResponse("Recent transactions retrieved",
                        Map.of("transactions", transactions, "count", transactions.size()));
            } catch (Exception e) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "❌ Failed to retrieve transactions: " + e.getMessage());
            }
        });
    }

    @GetMapping("/atm/status")
    public ResponseEntity<?> getATMStatus() {
        try {
            String status = transactionService.getATMStatus();
            boolean operational = transactionService.isATMOperational();

            Map<String, Object> response = new HashMap<>();
            response.put("operational", operational);
            response.put("status", status);
            response.put("timestamp", LocalDateTime.now());
            response.put("services", Map.of(
                    "withdrawal", operational,
                    "deposit", operational,
                    "transfer", operational,
                    "balance_inquiry", operational
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "❌ Unable to determine ATM status");
        }
    }

    @GetMapping("/atm/limits/{iban}")
    public ResponseEntity<?> getATMAccountLimits(@PathVariable String iban) {
        return withAuthenticatedUser(user -> {
            TransactionService.ATMResult result = transactionService.getAccountSummary(iban, user);

            if (result.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("iban", iban);
                data.put("balance", result.getNewBalance());
                data.put("availableBalance", result.getAvailableBalance());
                data.put("remainingDailyLimit", result.getRemainingDailyLimit());
                data.put("atmLimits", Map.of(
                        "maxWithdrawalPerTransaction", 500.0,
                        "maxDepositPerTransaction", 2000.0,
                        "maxTransferPerTransaction", 1000.0
                ));

                return createSuccessResponse("Account limits retrieved", data);
            } else {
                return createErrorResponse(HttpStatus.BAD_REQUEST, result.getMessage());
            }
        });
    }
}
