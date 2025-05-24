package nl.inholland.bankAppBackEnd.services;

import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.DailyTransferTracking;
import nl.inholland.bankAppBackEnd.repository.DailyTransferTrackingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class TransferLimitService {

    @Autowired
    private DailyTransferTrackingRepository dailyTransferTrackingRepository;

    /**
     * Check if a withdrawal is allowed based on absolute limit
     */
    public boolean canWithdraw(BankAccount account, Double amount) {
        return (account.getBalance() - amount) >= account.getAbsoluteLimit();
    }

    /**
     * Check if a transfer is allowed based on daily limit
     */
    public boolean canTransferToday(BankAccount account, Double amount) {
        LocalDate today = LocalDate.now();
        Double totalTransferredToday = getTotalTransferredToday(account, today);
        return (totalTransferredToday + amount) <= account.getDailyLimit();
    }

    /**
     * Get total amount transferred today for an account
     */
    public Double getTotalTransferredToday(BankAccount account, LocalDate date) {
        return dailyTransferTrackingRepository.getTotalTransferredToday(account.getId(), date);
    }

    /**
     * Get remaining daily transfer limit
     */
    public Double getRemainingDailyLimit(BankAccount account) {
        LocalDate today = LocalDate.now();
        Double totalTransferredToday = getTotalTransferredToday(account, today);
        return Math.max(0, account.getDailyLimit() - totalTransferredToday);
    }

    /**
     * Record a transfer amount for daily tracking
     */
    public void recordTransfer(BankAccount account, Double amount) {
        LocalDate today = LocalDate.now();

        DailyTransferTracking tracking = dailyTransferTrackingRepository
                .findByAccountAndTransferDate(account, today)
                .orElse(new DailyTransferTracking(account, today));

        tracking.addTransfer(amount);
        dailyTransferTrackingRepository.save(tracking);
    }

    /**
     * Validate both absolute and daily limits for a withdrawal/transfer
     */
    public TransferValidationResult validateTransfer(BankAccount fromAccount, Double amount, boolean isWithdrawal) {
        TransferValidationResult result = new TransferValidationResult();

        // Check absolute limit for withdrawals
        if (isWithdrawal && !canWithdraw(fromAccount, amount)) {
            result.setValid(false);
            result.setErrorMessage("Transfer denied: Would exceed absolute limit. Available balance: €" +
                    fromAccount.getAvailableBalance());
            return result;
        }

        // Check daily limit
        if (!canTransferToday(fromAccount, amount)) {
            Double remaining = getRemainingDailyLimit(fromAccount);
            result.setValid(false);
            result.setErrorMessage("Transfer denied: Would exceed daily limit. Remaining daily limit: €" +
                    String.format("%.2f", remaining));
            return result;
        }

        result.setValid(true);
        return result;
    }

    /**
     * Helper class for transfer validation results
     */
    public static class TransferValidationResult {
        private boolean valid;
        private String errorMessage;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}