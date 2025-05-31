package nl.inholland.bankAppBackEnd.services;

import nl.inholland.bankAppBackEnd.DTOs.TransactionDTO;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    public Page<Transaction> findAll(Pageable pageable) {
        return transactionRepository.findAll(pageable);
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
        dto.setFromIban(transaction.getFromAccount() != null ? transaction.getFromAccount().getIban() : null);
        dto.setToIban(transaction.getToAccount() != null ? transaction.getToAccount().getIban() : null);
        dto.setDate(transaction.getTimestamp() != null ? 
                transaction.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME) : null);
        dto.setInitiatedBy(transaction.getInitiatedByUser() != null ? 
                transaction.getInitiatedByUser().getUsername() : null);
        
        // For admin view, we don't calculate direction or signed amount
        dto.setDirection("Admin View");
        dto.setSignedAmount(transaction.getAmount());
        
        return dto;
    }

    /**
     * Get filtered transactions with direction information for a user with pagination
     */
    public Page<TransactionDTO> getFilteredTransactionsWithDirection(
            User user, String iban, String ibanType, Double amount, String comparator,
            String start, String end, Pageable pageable) {
        
        List<String> userIbans = getUserIbans(user);
        
        // Parse filter parameters
        Double minAmount = null, maxAmount = null, exactAmount = null;
        if (amount != null && comparator != null) {
            switch (comparator) {
                case ">": minAmount = amount; break;
                case "<": maxAmount = amount; break;
                case "=": exactAmount = amount; break;
            }
        }
        
        // Parse dates
        LocalDateTime startDate = null, endDate = null;
        if (start != null && !start.isEmpty()) {
            startDate = LocalDate.parse(start, DateTimeFormatter.ISO_DATE).atStartOfDay();
        }
        if (end != null && !end.isEmpty()) {
            endDate = LocalDate.parse(end, DateTimeFormatter.ISO_DATE).atTime(LocalTime.MAX);
        }
        
        // Get filtered transactions from repository
        Page<Transaction> transactionsPage = transactionRepository.findFilteredByUser(
                user, iban, ibanType, minAmount, maxAmount, exactAmount, startDate, endDate, pageable);
        
        // Convert to DTOs with direction info
        List<TransactionDTO> dtoList = transactionsPage.getContent().stream()
                .map(tx -> convertToDTO(tx, userIbans))
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtoList, pageable, transactionsPage.getTotalElements());
    }

    // Non-paginated version for backward compatibility
    public List<TransactionDTO> getFilteredTransactionsWithDirection(
            User user, String iban, String ibanType, Double amount, String comparator, String start, String end) {
        
        // Use paginated version but get all results
        Page<TransactionDTO> page = getFilteredTransactionsWithDirection(
                user, iban, ibanType, amount, comparator, start, end, Pageable.unpaged());
        
        return page.getContent();
    }

    public TransactionDTO getTransactionWithDirectionById(Long id, User user) {
        Optional<Transaction> txOpt = findById(id);
        if (txOpt.isEmpty()) {
            return null;
        }

        List<String> userIbans = getUserIbans(user);
        return convertToDTO(txOpt.get(), userIbans);
    }

    // Get user transactions with direction info (paginated)
    public Page<TransactionDTO> getTransactionsWithDirectionByUser(User user, Pageable pageable) {
        List<String> userIbans = getUserIbans(user);
        Page<Transaction> transactionsPage = transactionRepository.findByAccountOwner(user, pageable);
        
        List<TransactionDTO> transactionDTOs = transactionsPage.getContent().stream()
                .map(tx -> convertToDTO(tx, userIbans))
                .collect(Collectors.toList());
                
        return new PageImpl<>(transactionDTOs, pageable, transactionsPage.getTotalElements());
    }

    // Non-paginated version for backward compatibility
    public List<TransactionDTO> getTransactionsWithDirectionByUser(User user) {
        List<String> userIbans = getUserIbans(user);
        List<Transaction> transactions = transactionRepository.findByAccountOwner(user);
        return transactions.stream()
                .map(tx -> convertToDTO(tx, userIbans))
                .collect(Collectors.toList());
    }

    // Helper method to get user's IBANs
    public List<String> getUserIbans(User user) {
        List<BankAccount> userAccounts = bankAccountRepository.findAllByOwner(user);
        return userAccounts.stream()
            .map(BankAccount::getIban)
            .collect(Collectors.toList());
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
            case "Outgoing": return -Math.abs(amount);
            case "Incoming": return Math.abs(amount);
            case "Internal": return Math.abs(amount); // Use positive value for internal transfers
            default: return amount;
        }
    }

    // Admin filtered transactions with pagination
    public Page<Transaction> getFilteredTransactions(
            String iban, String ibanType, Double amount, String comparator,
            String start, String end, String initiatedBy, Pageable pageable) {
        
        // Parse filter parameters
        Double minAmount = null, maxAmount = null, exactAmount = null;
        if (amount != null && comparator != null) {
            switch (comparator) {
                case ">": minAmount = amount; break;
                case "<": maxAmount = amount; break;
                case "=": exactAmount = amount; break;
            }
        }
        
        // Parse dates
        LocalDateTime startDate = null, endDate = null;
        if (start != null && !start.isEmpty()) {
            startDate = LocalDate.parse(start, DateTimeFormatter.ISO_DATE).atStartOfDay();
        }
        if (end != null && !end.isEmpty()) {
            endDate = LocalDate.parse(end, DateTimeFormatter.ISO_DATE).atTime(LocalTime.MAX);
        }
        
        return transactionRepository.findFiltered(
                iban, ibanType, minAmount, maxAmount, exactAmount, startDate, endDate, initiatedBy, pageable);
    }

    // Legacy methods maintained for backward compatibility
    public List<Transaction> getFilteredTransactionsByUser(User user, String iban, String ibanType,
                                                          Double amount, String comparator,
                                                          String start, String end) {
        // Use paginated version but get all results
        return getFilteredTransactions(iban, ibanType, amount, comparator, start, end, null, Pageable.unpaged())
                .getContent();
    }

    public List<Transaction> getFilteredTransactions(String iban, String ibanType, Double amount,
                                                    String comparator, String start, String end) {
        return getFilteredTransactions(iban, ibanType, amount, comparator, start, end, null, Pageable.unpaged())
                .getContent();
    }
    
    public List<Transaction> getFilteredTransactions(String iban, String ibanType, Double amount,
                                                    String comparator, String start, String end,
                                                    String initiatedBy) {
        return getFilteredTransactions(iban, ibanType, amount, comparator, start, end, initiatedBy, Pageable.unpaged())
                .getContent();
    }

    // Get transactions for a specific user with pagination
    public Page<Transaction> getTransactionsByUser(User user, Pageable pageable) {
        return transactionRepository.findByAccountOwner(user, pageable);
    }

    // Get transactions for a user
    public List<Transaction> getTransactionsByUser(User user) {
        return transactionRepository.findByAccountOwner(user);
    }

    // Get transactions for a specific bank account
    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        return transactionRepository.findByFromAccountIdOrToAccountIdOrderByTimestampDesc(accountId, accountId);
    }

    // Count transactions for today
    public int getTodayTransactionsCount() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return (int) transactionRepository.countTransactionsByDate(startOfDay, endOfDay);
    }
}
