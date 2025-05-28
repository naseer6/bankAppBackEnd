package nl.inholland.bankAppBackEnd.services;

import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    public List<Transaction> getFilteredTransactions(String iban, String ibanType, Double amount, String comparator, String start, String end) {
        List<Transaction> allTransactions = transactionRepository.findAll();
        List<Transaction> filteredTransactions = new ArrayList<>();

        for (Transaction transaction : allTransactions) {
            if (matchesFilters(transaction, iban, ibanType, amount, comparator, start, end)) {
                filteredTransactions.add(transaction);
            }
        }

        return filteredTransactions;
    }

    private boolean matchesFilters(Transaction transaction, String iban, String ibanType, Double amount, String comparator, String start, String end) {
        return matchesIbanFilter(transaction, iban, ibanType) &&
                matchesAmountFilter(transaction, amount, comparator) &&
                matchesDateRangeFilter(transaction, start, end);
    }

    private boolean matchesIbanFilter(Transaction transaction, String iban, String ibanType) {
        if (iban == null || iban.isEmpty()) {
            return true; // No filter applied
        }

        // Default to "both" if ibanType is null or invalid
        if (ibanType == null || ibanType.isEmpty()) {
            ibanType = "both";
        }

        return switch (ibanType.toLowerCase()) {
            case "from" -> matchesFromIban(transaction, iban);
            case "to" -> matchesToIban(transaction, iban);
            case "both" -> matchesFromIban(transaction, iban) || matchesToIban(transaction, iban);
            default -> matchesFromIban(transaction, iban) || matchesToIban(transaction, iban); // Default to both
        };
    }

    private boolean matchesFromIban(Transaction transaction, String iban) {
        return transaction.getFromAccount() != null &&
                iban.equalsIgnoreCase(transaction.getFromAccount().getIban());
    }

    private boolean matchesToIban(Transaction transaction, String iban) {
        return transaction.getToAccount() != null &&
                iban.equalsIgnoreCase(transaction.getToAccount().getIban());
    }

    private boolean matchesAmountFilter(Transaction transaction, Double amount, String comparator) {
        if (amount == null || comparator == null) {
            return true; // No filter applied
        }

        BigDecimal transactionAmount = BigDecimal.valueOf(transaction.getAmount());
        BigDecimal filterAmount = BigDecimal.valueOf(amount);

        return switch (comparator) {
            case ">" -> transactionAmount.compareTo(filterAmount) > 0;
            case "<" -> transactionAmount.compareTo(filterAmount) < 0;
            case "=" -> transactionAmount.compareTo(filterAmount) == 0;
            default -> true; // Invalid comparator, don't filter
        };
    }

    private boolean matchesDateRangeFilter(Transaction transaction, String start, String end) {
        return matchesStartDateFilter(transaction, start) && matchesEndDateFilter(transaction, end);
    }

    private boolean matchesStartDateFilter(Transaction transaction, String start) {
        if (start == null || start.isEmpty()) {
            return true; // No filter applied
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
        LocalDate startDate = LocalDate.parse(start, formatter);
        LocalDate transactionDate = transaction.getTimestamp().toLocalDate();

        return transactionDate.compareTo(startDate) >= 0;
    }

    private boolean matchesEndDateFilter(Transaction transaction, String end) {
        if (end == null || end.isEmpty()) {
            return true; // No filter applied
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
        LocalDate endDate = LocalDate.parse(end, formatter);
        LocalDate transactionDate = transaction.getTimestamp().toLocalDate();

        return transactionDate.compareTo(endDate) <= 0;
    }

    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        return transactionRepository.findByFromAccountIdOrToAccountIdOrderByTimestampDesc(accountId, accountId);
    }

    public int getTodayTransactionsCount() {
        LocalDate today = LocalDate.now();
        List<Transaction> allTransactions = transactionRepository.findAll();
        int count = 0;

        for (Transaction transaction : allTransactions) {
            if (transaction.getTimestamp().toLocalDate().equals(today)) {
                count++;
            }
        }

        return count;
    }
}