package nl.inholland.bankAppBackEnd.services;

import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TransferService {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

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
            this.message = message;
            this.transaction = null;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Transaction getTransaction() { return transaction; }
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
        if (initiatedBy.getRole() == User.Role.USER && !fromAccount.getOwner().getId().equals(initiatedBy.getId())) {
            return new TransferResult(false, "❌ You can only transfer from your own accounts");
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

        // Perform the transfer
        fromAccount.setBalance(fromAccount.getBalance() - amount);
        toAccount.setBalance(toAccount.getBalance() + amount);

        // Update daily spent amount for source account
        fromAccount.addToDailySpent(amount);

        // Save accounts
        bankAccountRepository.save(fromAccount);
        bankAccountRepository.save(toAccount);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setTransactionType("TRANSFER");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setInitiatedByUser(initiatedBy);

        transaction = transactionRepository.save(transaction);

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
        if (initiatedBy.getRole() == User.Role.USER && !account.getOwner().getId().equals(initiatedBy.getId())) {
            return new TransferResult(false, "❌ You can only deposit to your own accounts");
        }

        account.setBalance(account.getBalance() + amount);
        bankAccountRepository.save(account);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setToAccount(account);
        transaction.setAmount(amount);
        transaction.setTransactionType("DEPOSIT");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setInitiatedByUser(initiatedBy);

        transaction = transactionRepository.save(transaction);

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
        if (initiatedBy.getRole() == User.Role.USER && !account.getOwner().getId().equals(initiatedBy.getId())) {
            return new TransferResult(false, "❌ You can only withdraw from your own accounts");
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
        Transaction transaction = new Transaction();
        transaction.setFromAccount(account);
        transaction.setAmount(amount);
        transaction.setTransactionType("WITHDRAWAL");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setInitiatedByUser(initiatedBy);

        transaction = transactionRepository.save(transaction);

        return new TransferResult(true,
                String.format("✅ Successfully withdrew €%.2f from %s. New balance: €%.2f",
                        amount, iban, account.getBalance()), transaction);
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

        // For internal transfers, use a special validation without account type restriction
        TransferResult validationResult = validateInternalTransferLimits(fromAccount, amount, initiatedBy);
        if (!validationResult.isSuccess()) {
            return validationResult;
        }

        // Perform the transfer
        fromAccount.setBalance(fromAccount.getBalance() - amount);
        toAccount.setBalance(toAccount.getBalance() + amount);

        // For internal transfers, we don't count against daily limits since it's between own accounts
        // But we still update daily spent if it's from a checking account to maintain consistency
        if (fromAccount.getType() == BankAccount.AccountType.CHECKING) {
            fromAccount.addToDailySpent(amount);
        }

        // Save accounts
        bankAccountRepository.save(fromAccount);
        bankAccountRepository.save(toAccount);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setTransactionType("INTERNAL_TRANSFER");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setInitiatedByUser(initiatedBy);

        transaction = transactionRepository.save(transaction);

        return new TransferResult(true,
                String.format("✅ Successfully transferred €%.2f between your accounts (%s → %s)",
                        amount, fromIban, toIban), transaction);
    }

    private TransferResult validateInternalTransferLimits(BankAccount fromAccount, Double amount, User initiatedBy) {
        // Check absolute limit (minimum balance)
        if (fromAccount.wouldViolateAbsoluteLimit(amount)) {
            double availableAmount = fromAccount.getBalance() - fromAccount.getAbsoluteLimit();
            return new TransferResult(false,
                    String.format("❌ Transfer would exceed absolute limit. Available amount: €%.2f",
                            Math.max(0, availableAmount)));
        }

        // Check sufficient balance
        if (fromAccount.getBalance() < amount) {
            return new TransferResult(false, "❌ Insufficient balance");
        }

        return new TransferResult(true, "Validation passed");
    }
}
