package nl.inholland.bankAppBackEnd.services;

import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    // Basic CRUD operations
    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public List<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    // Transaction with direction info - for better display in UI
    public List<Map<String, Object>> getFilteredTransactionsWithDirection(User user, String iban, String ibanType,
                                                                          Double amount, String comparator,
                                                                          String start, String end) {
        List<String> userIbans = getUserIbans(user);
        List<Transaction> transactions = getFilteredTransactionsByUser(user, iban, ibanType, amount, comparator, start, end);
        return enrichTransactionsWithDirection(transactions, userIbans);
    }

    public Map<String, Object> getTransactionWithDirectionById(Long id, User user) {
        Optional<Transaction> txOpt = findById(id);
        if (txOpt.isEmpty()) {
            return null;
        }

        List<String> userIbans = getUserIbans(user);
        return enrichTransactionWithDirection(txOpt.get(), userIbans);
    }

    public List<Map<String, Object>> getTransactionsWithDirectionByUser(User user) {
        List<String> userIbans = getUserIbans(user);
        List<Transaction> transactions = getTransactionsByUser(user);
        return enrichTransactionsWithDirection(transactions, userIbans);
    }

    // Helper method to get user's IBANs
    public List<String> getUserIbans(User user) {
        List<String> userIbans = new ArrayList<>();
        List<BankAccount> userAccounts = bankAccountRepository.findAllByOwner(user);

        for (BankAccount account : userAccounts) {
            userIbans.add(account.getIban());
        }

        return userIbans;
    }

    // Helper methods to enrich transactions with direction info
    private List<Map<String, Object>> enrichTransactionsWithDirection(List<Transaction> transactions, List<String> userIbans) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Transaction tx : transactions) {
            result.add(enrichTransactionWithDirection(tx, userIbans));
        }

        return result;
    }

    private Map<String, Object> enrichTransactionWithDirection(Transaction tx, List<String> userIbans) {
        Map<String, Object> result = new HashMap<>();

        // Basic transaction data
        result.put("id", tx.getId());
        result.put("fromIban", tx.getFromIban());
        result.put("toIban", tx.getToIban());
        result.put("amount", tx.getAmount());
        result.put("date", tx.getFormattedTimestamp());
        result.put("description", tx.getDescription());

        // Add direction information
        String direction = determineDirection(tx, userIbans);
        result.put("direction", direction);

        // Add signed amount (positive for incoming, negative for outgoing, zero for internal)
        Double signedAmount = determineSignedAmount(direction, tx.getAmount());
        result.put("signedAmount", signedAmount);

        return result;
    }

    private String determineDirection(Transaction tx, List<String> userIbans) {
        boolean isFromUserAccount = userIbans.contains(tx.getFromIban());
        boolean isToUserAccount = userIbans.contains(tx.getToIban());

        if (isFromUserAccount && isToUserAccount) return "Internal";
        if (isFromUserAccount) return "Outgoing";
        if (isToUserAccount) return "Incoming";
        return "External";
    }

    private Double determineSignedAmount(String direction, Double amount) {
        switch (direction) {
            case "Outgoing":
                return -Math.abs(amount);
            case "Incoming":
                return Math.abs(amount);
            case "Internal":
                return 0.0;
            default:
                return amount;
        }
    }

    // Transaction filtering methods
    public List<Transaction> getFilteredTransactionsByUser(User user, String iban, String ibanType,
                                                           Double amount, String comparator,
                                                           String start, String end) {
        List<Transaction> userTransactions = transactionRepository.findByAccountOwner(user);
        List<Transaction> filteredTransactions = new ArrayList<>();

        for (Transaction tx : userTransactions) {
            if (matchesFilters(tx, iban, ibanType, amount, comparator, start, end)) {
                filteredTransactions.add(tx);
            }
        }

        return filteredTransactions;
    }

    public List<Transaction> getFilteredTransactions(String iban, String ibanType, Double amount,
                                                     String comparator, String start, String end) {
        List<Transaction> allTransactions = transactionRepository.findAll();
        List<Transaction> filteredTransactions = new ArrayList<>();

        for (Transaction tx : allTransactions) {
            if (matchesFilters(tx, iban, ibanType, amount, comparator, start, end)) {
                filteredTransactions.add(tx);
            }
        }

        return filteredTransactions;
    }

    private boolean matchesFilters(Transaction tx, String iban, String ibanType,
                                   Double amount, String comparator,
                                   String start, String end) {
        return matchesIbanFilter(tx, iban, ibanType) &&
                matchesAmountFilter(tx, amount, comparator) &&
                matchesDateFilter(tx, start, end);
    }

    private boolean matchesIbanFilter(Transaction tx, String iban, String ibanType) {
        if (iban == null || iban.isEmpty()) return true;

        String type = (ibanType == null || ibanType.isEmpty()) ? "both" : ibanType.toLowerCase();

        if ("from".equals(type)) {
            return matchesFromIban(tx, iban);
        } else if ("to".equals(type)) {
            return matchesToIban(tx, iban);
        } else {
            return matchesFromIban(tx, iban) || matchesToIban(tx, iban);
        }
    }

    private boolean matchesFromIban(Transaction tx, String iban) {
        return tx.getFromAccount() != null &&
                iban.equalsIgnoreCase(tx.getFromAccount().getIban());
    }

    private boolean matchesToIban(Transaction tx, String iban) {
        return tx.getToAccount() != null &&
                iban.equalsIgnoreCase(tx.getToAccount().getIban());
    }

    private boolean matchesAmountFilter(Transaction tx, Double amount, String comparator) {
        if (amount == null || comparator == null) return true;

        BigDecimal txAmount = BigDecimal.valueOf(tx.getAmount());
        BigDecimal filterAmount = BigDecimal.valueOf(amount);

        if (">".equals(comparator)) {
            return txAmount.compareTo(filterAmount) > 0;
        } else if ("<".equals(comparator)) {
            return txAmount.compareTo(filterAmount) < 0;
        } else if ("=".equals(comparator)) {
            return txAmount.compareTo(filterAmount) == 0;
        }

        return true; // Default if comparator is invalid
    }

    private boolean matchesDateFilter(Transaction tx, String start, String end) {
        return isAfterStartDate(tx, start) && isBeforeEndDate(tx, end);
    }

    private boolean isAfterStartDate(Transaction tx, String start) {
        if (start == null || start.isEmpty()) return true;

        LocalDate startDate = LocalDate.parse(start, DateTimeFormatter.ISO_DATE);
        LocalDate txDate = tx.getTimestamp().toLocalDate();

        return !txDate.isBefore(startDate); // txDate >= startDate
    }

    private boolean isBeforeEndDate(Transaction tx, String end) {
        if (end == null || end.isEmpty()) return true;

        LocalDate endDate = LocalDate.parse(end, DateTimeFormatter.ISO_DATE);
        LocalDate txDate = tx.getTimestamp().toLocalDate();

        return !txDate.isAfter(endDate); // txDate <= endDate
    }

    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        return transactionRepository.findByFromAccountIdOrToAccountIdOrderByTimestampDesc(accountId, accountId);
    }

    public List<Transaction> getTransactionsByUser(User user) {
        return transactionRepository.findByAccountOwner(user);
    }

    public int getTodayTransactionsCount() {
        LocalDate today = LocalDate.now();
        List<Transaction> allTransactions = transactionRepository.findAll();
        int count = 0;

        for (Transaction tx : allTransactions) {
            if (tx.getTimestamp().toLocalDate().equals(today)) {
                count++;
            }
        }

        return count;
    }
}