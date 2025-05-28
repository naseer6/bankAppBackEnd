package nl.inholland.bankAppBackEnd.Controllers;

import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestBody Transaction transaction) {
        Transaction saved = transactionService.save(transaction);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getFilteredTransactions(
            @RequestParam(required = false) String iban,
            @RequestParam(required = false) String ibanType, // New parameter for IBAN filtering
            @RequestParam(required = false) Double amount,
            @RequestParam(required = false) String comparator,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end
    ) {
        List<Transaction> results;

        // Use the appropriate method based on whether ibanType is provided
        if (ibanType != null && !ibanType.isEmpty()) {
            results = transactionService.getFilteredTransactions(iban, ibanType, amount, comparator, start, end);
        } else {
            results = transactionService.getFilteredTransactions(iban, ibanType, amount, comparator, start, end);
        }

        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransactionById(@PathVariable Long id) {
        Optional<Transaction> transaction = transactionService.findById(id);
        return transaction.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}