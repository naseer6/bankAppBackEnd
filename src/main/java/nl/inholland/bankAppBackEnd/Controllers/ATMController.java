package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.services.ATMService;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/atm")
public class ATMController {

    @Autowired
    private ATMService atmService;

    @Autowired
    private UserService userService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userService.getUserByUsername(auth.getName()).orElse(null);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> atmWithdraw(@RequestBody Map<String, Object> requestBody) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    createErrorResponse("❌ Authentication required for ATM access")
            );
        }

        try {
            if (!requestBody.containsKey("iban") || !requestBody.containsKey("amount") ||
                    requestBody.get("iban") == null || requestBody.get("amount") == null) {
                return ResponseEntity.badRequest().body(
                        createErrorResponse("❌ Missing required fields: iban, amount")
                );
            }

            String iban = requestBody.get("iban").toString();
            Object amountObj = requestBody.get("amount");

            Double amount;
            try {
                amount = Double.valueOf(amountObj.toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(
                        createErrorResponse("❌ Invalid amount format")
                );
            }

            ATMService.ATMResult result = atmService.atmWithdraw(iban, amount, currentUser);

            if (result.isSuccess()) {
                return ResponseEntity.ok(createSuccessResponse(result));
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse(result.getMessage()));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    createErrorResponse("❌ Invalid request format: " + e.getMessage())
            );
        }
    }




    @PostMapping("/deposit")
    public ResponseEntity<?> atmDeposit(@RequestBody Map<String, Object> requestBody) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    createErrorResponse("❌ Authentication required for ATM access")
            );
        }

        try {
            String iban = (String) requestBody.get("iban");
            Double amount = Double.valueOf(requestBody.get("amount").toString());

            if (iban == null || amount == null) {
                return ResponseEntity.badRequest().body(
                        createErrorResponse("❌ Missing required fields: iban, amount")
                );
            }

            ATMService.ATMResult result = atmService.atmDeposit(iban, amount, currentUser);

            if (result.isSuccess()) {
                return ResponseEntity.ok(createSuccessResponse(result));
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse(result.getMessage()));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    createErrorResponse("❌ Invalid request format: " + e.getMessage())
            );
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> atmTransfer(@RequestBody Map<String, Object> requestBody) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    createErrorResponse("❌ Authentication required for ATM access")
            );
        }

        // Check if user is approved for transfers
        if (!currentUser.isApproved()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    createErrorResponse("❌ Your account is not approved for ATM transfers")
            );
        }

        try {
            String fromIban = (String) requestBody.get("fromIban");
            String toIban = (String) requestBody.get("toIban");
            Double amount = Double.valueOf(requestBody.get("amount").toString());

            if (fromIban == null || toIban == null || amount == null) {
                return ResponseEntity.badRequest().body(
                        createErrorResponse("❌ Missing required fields: fromIban, toIban, amount")
                );
            }

            ATMService.ATMResult result = atmService.atmTransfer(fromIban, toIban, amount, currentUser);

            if (result.isSuccess()) {
                return ResponseEntity.ok(createSuccessResponse(result));
            } else {
                return ResponseEntity.badRequest().body(createErrorResponse(result.getMessage()));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    createErrorResponse("❌ Invalid request format: " + e.getMessage())
            );
        }
    }

    @GetMapping("/account-summary/{iban}")
    public ResponseEntity<?> getAccountSummary(@PathVariable String iban) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    createErrorResponse("❌ Authentication required for ATM access")
            );
        }

        ATMService.ATMResult result = atmService.getAccountSummary(iban, currentUser);

        if (result.isSuccess()) {
            return ResponseEntity.ok(createSuccessResponse(result));
        } else {
            return ResponseEntity.badRequest().body(createErrorResponse(result.getMessage()));
        }
    }

    @GetMapping("/recent-transactions")
    public ResponseEntity<?> getRecentTransactions(@RequestParam(defaultValue = "5") int limit) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    createErrorResponse("❌ Authentication required for ATM access")
            );
        }

        try {
            List<Transaction> transactions = atmService.getRecentATMTransactions(currentUser, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Recent transactions retrieved");
            response.put("transactions", transactions);
            response.put("count", transactions.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    createErrorResponse("❌ Failed to retrieve transactions: " + e.getMessage())
            );
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getATMStatus() {
        try {
            String status = atmService.getATMStatus();
            boolean operational = atmService.isATMOperational();

            Map<String, Object> response = new HashMap<>();
            response.put("operational", operational);
            response.put("status", status);
            response.put("timestamp", java.time.LocalDateTime.now());
            response.put("services", Map.of(
                    "withdrawal", operational,
                    "deposit", operational,
                    "transfer", operational,
                    "balance_inquiry", operational
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    createErrorResponse("❌ Unable to determine ATM status")
            );
        }
    }

    @GetMapping("/limits/{iban}")
    public ResponseEntity<?> getATMAccountLimits(@PathVariable String iban) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    createErrorResponse("❌ Authentication required for ATM access")
            );
        }

        ATMService.ATMResult result = atmService.getAccountSummary(iban, currentUser);

        if (result.isSuccess()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("iban", iban);
            response.put("balance", result.getNewBalance());
            response.put("availableBalance", result.getAvailableBalance());
            response.put("remainingDailyLimit", result.getRemainingDailyLimit());
            response.put("atmLimits", Map.of(
                    "maxWithdrawalPerTransaction", 500.0,
                    "maxDepositPerTransaction", 2000.0,
                    "maxTransferPerTransaction", 1000.0
            ));

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(createErrorResponse(result.getMessage()));
        }
    }

    // Helper methods for consistent response formatting
    private Map<String, Object> createSuccessResponse(ATMService.ATMResult result) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", result.getMessage());

        if (result.getNewBalance() != null) {
            response.put("newBalance", result.getNewBalance());
        }
        if (result.getRemainingDailyLimit() != null) {
            response.put("remainingDailyLimit", result.getRemainingDailyLimit());
        }
        if (result.getAvailableBalance() != null) {
            response.put("availableBalance", result.getAvailableBalance());
        }
        if (result.getTransaction() != null) {
            response.put("transaction", result.getTransaction());
        }

        return response;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}