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
import java.util.List;
import java.util.Optional;

@Service
public class ATMService {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransferService transferService;

    public static class ATMResult {
        private boolean success;
        private String message;
        private Double newBalance;
        private Double remainingDailyLimit;
        private Double availableBalance;
        private Transaction transaction;

        public ATMResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public ATMResult(boolean success, String message, BankAccount account, Transaction transaction) {
            this.success = success;
            this.message = message;
            this.newBalance = account.getBalance();
            this.remainingDailyLimit = account.getRemainingDailyLimit();
            this.availableBalance = Math.max(0, account.getBalance() - account.getAbsoluteLimit());
            this.transaction = transaction;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Double getNewBalance() { return newBalance; }
        public Double getRemainingDailyLimit() { return remainingDailyLimit; }
        public Double getAvailableBalance() { return availableBalance; }
        public Transaction getTransaction() { return transaction; }
    }

    @Transactional
    public ATMResult atmWithdraw(String iban, Double amount, User user) {
        // Enhanced ATM withdrawal with detailed validation and logging
        if (amount == null || amount <= 0) {
            return new ATMResult(false, "❌ Invalid withdrawal amount");
        }

        // Check for common ATM denominations (optional business rule)
        if (!isValidATMDenomination(amount)) {
            return new ATMResult(false, "❌ ATM can only dispense amounts in €10 increments");
        }

        // Check ATM daily cash limits (simulated)
        if (amount > 500) {
            return new ATMResult(false, "❌ ATM daily withdrawal limit is €500 per transaction");
        }

        Optional<BankAccount> accountOpt = bankAccountRepository.findByIban(iban);
        if (accountOpt.isEmpty()) {
            return new ATMResult(false, "❌ Account not found");
        }

        BankAccount account = accountOpt.get();

        // Verify account ownership
        if (!account.getOwner().getId().equals(user.getId())) {
            return new ATMResult(false, "❌ Unauthorized access to account");
        }

        // Check if account is active/approved
        if (!account.getOwner().isApproved()) {
            return new ATMResult(false, "❌ Account is not active for ATM transactions");
        }

        // Use transfer service for consistent limit validation
        TransferService.TransferResult result = transferService.withdraw(iban, amount, user);

        if (result.isSuccess()) {
            // Refresh account data
            account = bankAccountRepository.findByIban(iban).get();
            return new ATMResult(true,
                    String.format("✅ Withdrawal successful. Please collect your cash: €%.2f", amount),
                    account, result.getTransaction());
        } else {
            return new ATMResult(false, result.getMessage());
        }
    }

    @Transactional
    public ATMResult atmDeposit(String iban, Double amount, User user) {
        // Enhanced ATM deposit with validation
        if (amount == null || amount <= 0) {
            return new ATMResult(false, "❌ Invalid deposit amount");
        }

        // ATM deposit limits (simulate cash handling limits)
        if (amount > 2000) {
            return new ATMResult(false, "❌ ATM deposit limit is €2,000 per transaction");
        }

        Optional<BankAccount> accountOpt = bankAccountRepository.findByIban(iban);
        if (accountOpt.isEmpty()) {
            return new ATMResult(false, "❌ Account not found");
        }

        BankAccount account = accountOpt.get();

        // Verify account ownership
        if (!account.getOwner().getId().equals(user.getId())) {
            return new ATMResult(false, "❌ Unauthorized access to account");
        }

        // Use transfer service for consistent processing
        TransferService.TransferResult result = transferService.deposit(iban, amount, user);

        if (result.isSuccess()) {
            // Refresh account data
            account = bankAccountRepository.findByIban(iban).get();
            return new ATMResult(true,
                    String.format("✅ Deposit successful. Your cash has been processed: €%.2f", amount),
                    account, result.getTransaction());
        } else {
            return new ATMResult(false, result.getMessage());
        }
    }

    @Transactional
    public ATMResult atmTransfer(String fromIban, String toIban, Double amount, User user) {
        // Enhanced ATM transfer with additional validations
        if (amount == null || amount <= 0) {
            return new ATMResult(false, "❌ Invalid transfer amount");
        }

        // ATM transfer limits
        if (amount > 1000) {
            return new ATMResult(false, "❌ ATM transfer limit is €1,000 per transaction");
        }

        Optional<BankAccount> fromAccountOpt = bankAccountRepository.findByIban(fromIban);
        if (fromAccountOpt.isEmpty()) {
            return new ATMResult(false, "❌ Source account not found");
        }

        BankAccount fromAccount = fromAccountOpt.get();

        // Verify account ownership
        if (!fromAccount.getOwner().getId().equals(user.getId())) {
            return new ATMResult(false, "❌ Unauthorized access to account");
        }

        // Only allow transfers from checking accounts via ATM
        if (fromAccount.getType() != BankAccount.AccountType.CHECKING) {
            return new ATMResult(false, "❌ ATM transfers only allowed from checking accounts");
        }

        // Use transfer service for consistent processing
        TransferService.TransferResult result = transferService.transferFunds(fromIban, toIban, amount, user);

        if (result.isSuccess()) {
            // Refresh account data
            fromAccount = bankAccountRepository.findByIban(fromIban).get();
            return new ATMResult(true, result.getMessage(), fromAccount, result.getTransaction());
        } else {
            return new ATMResult(false, result.getMessage());
        }
    }

    public List<Transaction> getRecentATMTransactions(User user, int limit) {
        // Get recent transactions for user's accounts
        List<BankAccount> userAccounts = bankAccountRepository.findAllByOwner(user);

        return transactionRepository.findAll().stream()
                .filter(tx -> tx.getInitiatedByUser().getId().equals(user.getId()))
                .filter(tx -> userAccounts.stream().anyMatch(acc ->
                        (tx.getFromAccount() != null && tx.getFromAccount().getId().equals(acc.getId())) ||
                                (tx.getToAccount() != null && tx.getToAccount().getId().equals(acc.getId()))
                ))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .toList();
    }

    public ATMResult getAccountSummary(String iban, User user) {
        Optional<BankAccount> accountOpt = bankAccountRepository.findByIban(iban);
        if (accountOpt.isEmpty()) {
            return new ATMResult(false, "❌ Account not found");
        }

        BankAccount account = accountOpt.get();

        // Verify account ownership
        if (!account.getOwner().getId().equals(user.getId())) {
            return new ATMResult(false, "❌ Unauthorized access to account");
        }

        return new ATMResult(true, "Account information retrieved", account, null);
    }

    private boolean isValidATMDenomination(Double amount) {
        // ATMs typically dispense money in €10, €20, €50 denominations
        // For simplicity, we'll check if the amount is divisible by 10
        return amount % 10 == 0;
    }

    public boolean isATMOperational() {
        // Simulate ATM operational status
        // In real implementation, this would check:
        // - Cash availability
        // - Network connectivity
        // - Machine status
        return true;
    }

    public String getATMStatus() {
        if (isATMOperational()) {
            return "✅ ATM is operational and ready for use";
        } else {
            return "❌ ATM is temporarily out of service";
        }
    }



}