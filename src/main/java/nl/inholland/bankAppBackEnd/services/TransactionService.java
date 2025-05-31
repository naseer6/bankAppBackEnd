package nl.inholland.bankAppBackEnd.services;

import nl.inholland.bankAppBackEnd.DTOs.TransactionDTO;
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
import java.util.stream.Collectors;

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

    // Convert Transaction to TransactionDTO
    private TransactionDTO convertToDTO(Transaction transaction, List<String> userIbans) {
        TransactionDTO dto = new TransactionDTO();
        
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setDescription(transaction.getTransactionType());
        
        // Set IBANs
        dto.setFromIban(transaction.getFromAccount() != null ? transaction.getFromAccount().getIban() : null);
        dto.setToIban(transaction.getToAccount() != null ? transaction.getToAccount().getIban() : null);
        
        // Set timestamp with both date and time
        dto.setDate(transaction.getTimestamp() != null ? 
                transaction.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME) : null);
        
        // Set initiated by
        dto.setInitiatedBy(transaction.getInitiatedByUser() != null ? 
                transaction.getInitiatedByUser().getUsername() : null);
        
        // Add direction information
        String direction = determineDirection(transaction, userIbans);
        dto.setDirection(direction);
        
        // Add signed amount
        Double signedAmount = determineSignedAmount(direction, transaction.getAmount());
        dto.setSignedAmount(signedAmount);
        
        return dto;
    }
    
    // Convert Transaction to TransactionDTO for admin view (all transactions)
    public TransactionDTO convertToAdminDTO(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setDescription(transaction.getTransactionType());
        
        // Set IBANs
        dto.setFromIban(transaction.getFromAccount() != null ? transaction.getFromAccount().getIban() : null);
        dto.setToIban(transaction.getToAccount() != null ? transaction.getToAccount().getIban() : null);
        
        // Set timestamp with both date and time
        dto.setDate(transaction.getTimestamp() != null ? 
                transaction.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME) : null);
        
        // Set initiated by
        dto.setInitiatedBy(transaction.getInitiatedByUser() != null ? 
                transaction.getInitiatedByUser().getUsername() : null);
        
        // For admin view, we don't calculate direction or signed amount
        // as admins need to see the raw data
        dto.setDirection("Admin View");
        dto.setSignedAmount(transaction.getAmount());
        
        return dto;
    }

    // Transaction with direction info - for better display in UI
    public List<TransactionDTO> getFilteredTransactionsWithDirection(User user, String iban, String ibanType,
                                                                     Double amount, String comparator,
                                                                     String start, String end) {
        List<String> userIbans = getUserIbans(user);
        List<Transaction> transactions = getFilteredTransactionsByUser(user, iban, ibanType, amount, comparator, start, end);
        return transactions.stream()
                .map(tx -> convertToDTO(tx, userIbans))
                .collect(Collectors.toList());
    }

    public TransactionDTO getTransactionWithDirectionById(Long id, User user) {
        Optional<Transaction> txOpt = findById(id);
        if (txOpt.isEmpty()) {
            return null;
        }

        List<String> userIbans = getUserIbans(user);
        return convertToDTO(txOpt.get(), userIbans);
    }

    public List<TransactionDTO> getTransactionsWithDirectionByUser(User user) {
        List<String> userIbans = getUserIbans(user);
        List<Transaction> transactions = getTransactionsByUser(user);
        return transactions.stream()
                .map(tx -> convertToDTO(tx, userIbans))
                .collect(Collectors.toList());
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

    private String determineDirection(Transaction tx, List<String> userIbans) {
        boolean isFromUserAccount = tx.getFromAccount() != null && 
                userIbans.contains(tx.getFromAccount().getIban());
        boolean isToUserAccount = tx.getToAccount() != null && 
                userIbans.contains(tx.getToAccount().getIban());

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
                // Don't set internal transfers to 0.0
                // For internal transfers, we still want to show the actual amount
                // Use positive value since it's moving between your own accounts
                return Math.abs(amount);
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
            if (matchesFilters(tx, iban, ibanType, amount, comparator, start, end, null)) {
                filteredTransactions.add(tx);
            }
        }

        return filteredTransactions;
    }

    public List<Transaction> getFilteredTransactions(String iban, String ibanType, Double amount,
                                                     String comparator, String start, String end) {
        return getFilteredTransactions(iban, ibanType, amount, comparator, start, end, null);
    }
    
    public List<Transaction> getFilteredTransactions(String iban, String ibanType, Double amount,
                                                     String comparator, String start, String end,
                                                     String initiatedBy) {
        List<Transaction> allTransactions = transactionRepository.findAll();
        List<Transaction> filteredTransactions = new ArrayList<>();

        for (Transaction tx : allTransactions) {
            if (matchesFilters(tx, iban, ibanType, amount, comparator, start, end, initiatedBy)) {
                filteredTransactions.add(tx);
            }
        }
        
        // Sort by timestamp in descending order to show most recent transactions first
        filteredTransactions.sort(Comparator.comparing(Transaction::getTimestamp).reversed());
        
        return filteredTransactions;
    }

    private boolean matchesFilters(Transaction tx, String iban, String ibanType,
                                   Double amount, String comparator,
                                   String start, String end) {
        return matchesFilters(tx, iban, ibanType, amount, comparator, start, end, null);
    }

    private boolean matchesFilters(Transaction tx, String iban, String ibanType,
                                   Double amount, String comparator,
                                   String start, String end, String initiatedBy) {
        return matchesIbanFilter(tx, iban, ibanType) &&
                matchesAmountFilter(tx, amount, comparator) &&
                matchesDateFilter(tx, start, end) &&
                matchesInitiatedByFilter(tx, initiatedBy);
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

    private boolean matchesInitiatedByFilter(Transaction tx, String initiatedBy) {
        if (initiatedBy == null || initiatedBy.isEmpty()) {
            return true;
        }
        
        return tx.getInitiatedByUser() != null &&
                tx.getInitiatedByUser().getUsername().equalsIgnoreCase(initiatedBy);
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

