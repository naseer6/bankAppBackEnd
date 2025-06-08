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
import org.springframework.transaction.annotation.Transactional;

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
        List<TransactionDTO> dtoList = new ArrayList<>();
        for (Transaction tx : transactionsPage.getContent()) {
            dtoList.add(convertToDTO(tx, userIbans));
        }

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

        List<TransactionDTO> transactionDTOs = new ArrayList<>();
        for (Transaction tx : transactionsPage.getContent()) {
            transactionDTOs.add(convertToDTO(tx, userIbans));
        }


        return new PageImpl<>(transactionDTOs, pageable, transactionsPage.getTotalElements());
    }

    // Non-paginated version for backward compatibility
    public List<TransactionDTO> getTransactionsWithDirectionByUser(User user) {
        List<String> userIbans = getUserIbans(user);
        List<Transaction> transactions = transactionRepository.findByAccountOwner(user);
        List<TransactionDTO> dtos = new ArrayList<>();
        for (Transaction tx : transactions) {
            dtos.add(convertToDTO(tx, userIbans));
        }
        return dtos;

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
        return switch (direction) {
            case "Outgoing" -> -Math.abs(amount);
            case "Incoming" -> Math.abs(amount);
            case "Internal" -> Math.abs(amount); // Use positive value for internal transfers
            default -> amount;
        };
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

    public static class TransferResult {
        private boolean success;
        private String message;
        private Transaction transaction;

        public TransferResult(boolean success, String message, Transaction transaction) {
            this.success = success;
            this.message = message;
            this.transaction = transaction;
        }

        public TransferResult(boolean success, String message) {
            this.success = success;
            this.message = null;
            this.transaction = null;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Transaction getTransaction() { return transaction; }
    }

    /**
     * Helper method to validate account access for a user
     */
    private TransferResult validateAccountAccess(BankAccount account, User user, String action) {
        if (account == null) {
            return new TransferResult(false, "❌ Account not found");
        }
        
        // Regular users can only access their own accounts
        if (user.getRole() == User.Role.USER && !account.getOwner().getId().equals(user.getId())) {
            return new TransferResult(false, "❌ You can only " + action + " your own accounts");
        }
        
        return new TransferResult(true, "Access validated");
    }

    /**
     * Create and save a transaction record
     */
    private Transaction createTransactionRecord(BankAccount fromAccount, BankAccount toAccount, 
                                               Double amount, String transactionType, User initiatedBy) {
        Transaction transaction = new Transaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setTransactionType(transactionType);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setInitiatedByUser(initiatedBy);
        
        return transactionRepository.save(transaction);
    }

    @Transactional
    public TransferResult transferFunds(String fromIban, String toIban, Double amount, User initiatedBy) {
        // Validate inputs
        if (amount == null || amount <= 0) {
            return new TransferResult(false, "❌ Transfer amount must be greater than zero");
        }

        // Find accounts
        Optional<BankAccount> fromAccountOpt = bankAccountRepository.findByIban(fromIban);
        Optional<BankAccount> toAccountOpt = bankAccountRepository.findByIban(toIban);

        if (fromAccountOpt.isEmpty()) {
            return new TransferResult(false, "❌ Source account not found");
        }
        if (toAccountOpt.isEmpty()) {
            return new TransferResult(false, "❌ Destination account not found");
        }

        BankAccount fromAccount = fromAccountOpt.get();
        BankAccount toAccount = toAccountOpt.get();

        // Validate account types - can only transfer from checking accounts
        if (fromAccount.getType() != BankAccount.AccountType.CHECKING) {
            return new TransferResult(false, "❌ Transfers can only be made from checking accounts");
        }

        // For customer transfers, verify ownership
        if (initiatedBy.getRole() == User.Role.USER) {
            TransferResult accessResult = validateAccountAccess(fromAccount, initiatedBy, "transfer from");
            if (!accessResult.isSuccess()) {
                return accessResult;
            }
        }

        // Check if same account
        if (fromAccount.getId().equals(toAccount.getId())) {
            return new TransferResult(false, "❌ Cannot transfer to the same account");
        }

        // Validate transfer limits
        TransferResult validationResult = validateTransferLimits(fromAccount, amount, initiatedBy);
        if (!validationResult.isSuccess()) {
            return validationResult;
        }

        // Execute the transfer
        fromAccount.setBalance(fromAccount.getBalance() - amount);
        toAccount.setBalance(toAccount.getBalance() + amount);

        // Update daily spent amount for source account
        fromAccount.addToDailySpent(amount);

        // Save accounts
        bankAccountRepository.save(fromAccount);
        bankAccountRepository.save(toAccount);

        // Create transaction record
        Transaction transaction = createTransactionRecord(fromAccount, toAccount, amount, "TRANSFER", initiatedBy);

        return new TransferResult(true,
                String.format("✅ Successfully transferred €%.2f from %s to %s",
                        amount, fromIban, toIban), transaction);
    }

    private TransferResult validateTransferLimits(BankAccount fromAccount, Double amount, User initiatedBy) {
        // Check absolute limit (minimum balance)
        if (fromAccount.wouldViolateAbsoluteLimit(amount)) {
            double availableAmount = fromAccount.getBalance() - fromAccount.getAbsoluteLimit();
            return new TransferResult(false,
                    String.format("❌ Transfer would exceed absolute limit. Available amount: €%.2f",
                            Math.max(0, availableAmount)));
        }

        // Check daily limit (only for customer transfers, admins can override)
        if (initiatedBy.getRole() == User.Role.USER && fromAccount.wouldExceedDailyLimit(amount)) {
            double remainingLimit = fromAccount.getRemainingDailyLimit();
            return new TransferResult(false,
                    String.format("❌ Transfer would exceed daily limit. Remaining daily limit: €%.2f",
                            remainingLimit));
        }

        // Check sufficient balance
        if (fromAccount.getBalance() < amount) {
            return new TransferResult(false, "❌ Insufficient balance");
        }

        return new TransferResult(true, "Validation passed");
    }

    @Transactional
    public TransferResult deposit(String iban, Double amount, User initiatedBy) {
        if (amount == null || amount <= 0) {
            return new TransferResult(false, "❌ Deposit amount must be greater than zero");
        }

        Optional<BankAccount> accountOpt = bankAccountRepository.findByIban(iban);
        if (accountOpt.isEmpty()) {
            return new TransferResult(false, "❌ Account not found");
        }

        BankAccount account = accountOpt.get();

        // For customer deposits, verify ownership
        if (initiatedBy.getRole() == User.Role.USER) {
            TransferResult accessResult = validateAccountAccess(account, initiatedBy, "deposit to");
            if (!accessResult.isSuccess()) {
                return accessResult;
            }
        }

        account.setBalance(account.getBalance() + amount);
        bankAccountRepository.save(account);

        // Create transaction record
        Transaction transaction = createTransactionRecord(null, account, amount, "DEPOSIT", initiatedBy);

        return new TransferResult(true,
                String.format("✅ Successfully deposited €%.2f to %s. New balance: €%.2f",
                        amount, iban, account.getBalance()), transaction);
    }

    @Transactional
    public TransferResult withdraw(String iban, Double amount, User initiatedBy) {
        if (amount == null || amount <= 0) {
            return new TransferResult(false, "❌ Withdrawal amount must be greater than zero");
        }

        Optional<BankAccount> accountOpt = bankAccountRepository.findByIban(iban);
        if (accountOpt.isEmpty()) {
            return new TransferResult(false, "❌ Account not found");
        }

        BankAccount account = accountOpt.get();

        // For customer withdrawals, verify ownership
        if (initiatedBy.getRole() == User.Role.USER) {
            TransferResult accessResult = validateAccountAccess(account, initiatedBy, "withdraw from");
            if (!accessResult.isSuccess()) {
                return accessResult;
            }
        }

        // Validate withdrawal limits
        TransferResult validationResult = validateTransferLimits(account, amount, initiatedBy);
        if (!validationResult.isSuccess()) {
            return validationResult;
        }

        account.setBalance(account.getBalance() - amount);
        account.addToDailySpent(amount);
        bankAccountRepository.save(account);

        // Create transaction record
        Transaction transaction = createTransactionRecord(account, null, amount, "WITHDRAWAL", initiatedBy);

        return new TransferResult(true,
                String.format("✅ Successfully withdrew €%.2f from %s. New balance: €%.2f",
                        amount, iban, account.getBalance()), transaction);
    }

    @Transactional
    public TransferResult internalTransfer(String fromIban, String toIban, Double amount, User initiatedBy) {
        // Validate inputs
        if (amount == null || amount <= 0) {
            return new TransferResult(false, "❌ Transfer amount must be greater than zero");
        }

        // Find accounts
        Optional<BankAccount> fromAccountOpt = bankAccountRepository.findByIban(fromIban);
        Optional<BankAccount> toAccountOpt = bankAccountRepository.findByIban(toIban);

        if (fromAccountOpt.isEmpty()) {
            return new TransferResult(false, "❌ Source account not found");
        }
        if (toAccountOpt.isEmpty()) {
            return new TransferResult(false, "❌ Destination account not found");
        }

        BankAccount fromAccount = fromAccountOpt.get();
        BankAccount toAccount = toAccountOpt.get();

        // Verify both accounts belong to the same user
        if (!fromAccount.getOwner().getId().equals(initiatedBy.getId()) ||
                !toAccount.getOwner().getId().equals(initiatedBy.getId())) {
            return new TransferResult(false, "❌ Internal transfers can only be made between your own accounts");
        }

        // Check if same account
        if (fromAccount.getId().equals(toAccount.getId())) {
            return new TransferResult(false, "❌ Cannot transfer to the same account");
        }

        // Check absolute limit and balance
        if (fromAccount.wouldViolateAbsoluteLimit(amount)) {
            double availableAmount = fromAccount.getBalance() - fromAccount.getAbsoluteLimit();
            return new TransferResult(false,
                    String.format("❌ Transfer would exceed absolute limit. Available amount: €%.2f",
                            Math.max(0, availableAmount)));
        }

        if (fromAccount.getBalance() < amount) {
            return new TransferResult(false, "❌ Insufficient balance");
        }

        // Perform the transfer
        fromAccount.setBalance(fromAccount.getBalance() - amount);
        toAccount.setBalance(toAccount.getBalance() + amount);

        // Save accounts
        bankAccountRepository.save(fromAccount);
        bankAccountRepository.save(toAccount);

        // Create transaction record
        Transaction transaction = createTransactionRecord(fromAccount, toAccount, amount, "INTERNAL_TRANSFER", initiatedBy);

        return new TransferResult(true,
                String.format("✅ Successfully transferred €%.2f between your accounts (%s → %s)",
                        amount, fromIban, toIban), transaction);
    }

    @Transactional
    public TransferResult updateAccountLimits(String iban, Double absoluteLimit, Double dailyLimit, User initiatedBy) {
        // Only admins can update limits
        if (initiatedBy.getRole() != User.Role.ADMIN) {
            return new TransferResult(false, "❌ Only administrators can update account limits");
        }

        Optional<BankAccount> accountOpt = bankAccountRepository.findByIban(iban);
        if (accountOpt.isEmpty()) {
            return new TransferResult(false, "❌ Account not found");
        }

        BankAccount account = accountOpt.get();

        if (absoluteLimit != null) {
            if (absoluteLimit < 0) {
                return new TransferResult(false, "❌ Absolute limit cannot be negative");
            }
            account.setAbsoluteLimit(absoluteLimit);
        }

        if (dailyLimit != null) {
            if (dailyLimit < 0) {
                return new TransferResult(false, "❌ Daily limit cannot be negative");
            }
            account.setDailyLimit(dailyLimit);
        }

        bankAccountRepository.save(account);

        return new TransferResult(true,
                String.format("✅ Successfully updated limits for account %s. Absolute limit: €%.2f, Daily limit: €%.2f",
                        iban, account.getAbsoluteLimit(), account.getDailyLimit()));
    }
}
