package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.services.TransferService;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    @Autowired
    private TransferService transferService;

    @Autowired
    private UserService userService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userService.getUserByUsername(auth.getName()).orElse(null);
    }

    @PostMapping("/customer")
    public ResponseEntity<?> customerTransfer(@RequestBody Map<String, Object> requestBody) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("success", false, "message", "❌ Not authenticated")
            );
        }

        // Only approved users can make transfers
        if (currentUser.getRole() == User.Role.USER && !currentUser.isApproved()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of("success", false, "message", "❌ Your account is not yet approved for transfers")
            );
        }

        try {
            String fromIban = (String) requestBody.get("fromIban");
            String toIban = (String) requestBody.get("toIban");
            Double amount = Double.valueOf(requestBody.get("amount").toString());

            if (fromIban == null || toIban == null || amount == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "message", "❌ Missing required fields: fromIban, toIban, amount")
                );
            }

            TransferService.TransferResult result = transferService.transferFunds(fromIban, toIban, amount, currentUser);

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

    @PostMapping("/admin")
    public ResponseEntity<?> adminTransfer(@RequestBody Map<String, Object> requestBody) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("success", false, "message", "❌ Not authenticated")
            );
        }

        // Only admins can use this endpoint
        if (currentUser.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of("success", false, "message", "❌ Only administrators can perform admin transfers")
            );
        }

        try {
            String fromIban = (String) requestBody.get("fromIban");
            String toIban = (String) requestBody.get("toIban");
            Double amount = Double.valueOf(requestBody.get("amount").toString());

            if (fromIban == null || toIban == null || amount == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "message", "❌ Missing required fields: fromIban, toIban, amount")
                );
            }

            TransferService.TransferResult result = transferService.transferFunds(fromIban, toIban, amount, currentUser);

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

    @PostMapping("/deposit")
    public ResponseEntity<?> makeDeposit(@RequestBody Map<String, Object> requestBody) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("success", false, "message", "❌ Not authenticated")
            );
        }

        try {
            String iban = (String) requestBody.get("iban");
            Double amount = Double.valueOf(requestBody.get("amount").toString());

            if (iban == null || amount == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "message", "❌ Missing required fields: iban, amount")
                );
            }

            TransferService.TransferResult result = transferService.deposit(iban, amount, currentUser);

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

    @PostMapping("/withdraw")
    public ResponseEntity<?> makeWithdrawal(@RequestBody Map<String, Object> requestBody) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("success", false, "message", "❌ Not authenticated")
            );
        }

        try {
            String iban = (String) requestBody.get("iban");
            Double amount = Double.valueOf(requestBody.get("amount").toString());

            if (iban == null || amount == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "message", "❌ Missing required fields: iban, amount")
                );
            }

            TransferService.TransferResult result = transferService.withdraw(iban, amount, currentUser);

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
}