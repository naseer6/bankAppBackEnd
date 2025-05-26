package nl.inholland.bankAppBackEnd.services;

import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    public List<Transaction> getFilteredTransactions(String iban, Double amount, String comparator, String start, String end) {
        List<Transaction> all = transactionRepository.findAll();
        Stream<Transaction> stream = all.stream();

        if (iban != null && !iban.isEmpty()) {
            stream = stream.filter(tx ->
                    (tx.getFromAccount() != null && iban.equalsIgnoreCase(tx.getFromAccount().getIban())) ||
                            (tx.getToAccount() != null && iban.equalsIgnoreCase(tx.getToAccount().getIban()))
            );
        }

        if (amount != null && comparator != null) {
            BigDecimal amt = BigDecimal.valueOf(amount);
            switch (comparator) {
                case ">" -> stream = stream.filter(tx -> BigDecimal.valueOf(tx.getAmount()).compareTo(amt) > 0);
                case "<" -> stream = stream.filter(tx -> BigDecimal.valueOf(tx.getAmount()).compareTo(amt) < 0);
                case "=" -> stream = stream.filter(tx -> BigDecimal.valueOf(tx.getAmount()).compareTo(amt) == 0);
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
        if (start != null && !start.isEmpty()) {
            LocalDate startDate = LocalDate.parse(start, formatter);
            stream = stream.filter(tx -> tx.getTimestamp().toLocalDate().compareTo(startDate) >= 0);
        }

        if (end != null && !end.isEmpty()) {
            LocalDate endDate = LocalDate.parse(end, formatter);
            stream = stream.filter(tx -> tx.getTimestamp().toLocalDate().compareTo(endDate) <= 0);
        }

        return stream.toList();
    }

    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        return transactionRepository.findByFromAccountIdOrToAccountIdOrderByTimestampDesc(accountId, accountId);
    }

    public int getTodayTransactionsCount() {
        LocalDate today = LocalDate.now();
        return (int) transactionRepository.findAll()
                .stream()
                .filter(tx -> tx.getTimestamp().toLocalDate().equals(today))
                .count();
    }
}