package nl.inholland.bankAppBackEnd.services;

import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class BankAccountService {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    public BankAccount createAccountForUser(User user) {
        BankAccount account = new BankAccount();
        account.setOwner(user);
        account.setBalance(0.0);
        account.setIban(generateIban());
        account.setAbsoluteLimit(0.0); // Default absolute limit
        account.setDailyLimit(1000.0);  // Default daily limit
        return bankAccountRepository.save(account);
    }

    public List<BankAccount> createAccountsForUser(User user) {
        BankAccount checking = new BankAccount();
        checking.setIban(generateIban());
        checking.setBalance(0.0);
        checking.setOwner(user);
        checking.setType(BankAccount.AccountType.CHECKING);
        checking.setAbsoluteLimit(0.0);   // Default absolute limit
        checking.setDailyLimit(1000.0);   // Default daily limit

        BankAccount savings = new BankAccount();
        savings.setIban(generateIban());
        savings.setBalance(0.0);
        savings.setOwner(user);
        savings.setType(BankAccount.AccountType.SAVINGS);
        savings.setAbsoluteLimit(0.0);    // Default absolute limit
        savings.setDailyLimit(500.0);     // Lower daily limit for savings

        bankAccountRepository.save(checking);
        bankAccountRepository.save(savings);

        return List.of(checking, savings);
    }

    public BankAccount createAccountWithLimits(User user, BankAccount.AccountType type,
                                               Double absoluteLimit, Double dailyLimit) {
        BankAccount account = new BankAccount();
        account.setOwner(user);
        account.setBalance(0.0);
        account.setIban(generateIban());
        account.setType(type);
        account.setAbsoluteLimit(absoluteLimit != null ? absoluteLimit : 0.0);
        account.setDailyLimit(dailyLimit != null ? dailyLimit : 1000.0);
        return bankAccountRepository.save(account);
    }

    public void updateLimits(BankAccount account, Double absoluteLimit, Double dailyLimit) {
        if (absoluteLimit != null) {
            account.setAbsoluteLimit(absoluteLimit);
        }
        if (dailyLimit != null) {
            account.setDailyLimit(dailyLimit);
        }
        bankAccountRepository.save(account);
    }

    private String generateIban() {
        Random random = new Random();
        // Generate NL + 2 check digits + 4 bank code + 10 account number
        String bankCode = "INHB"; // Inholland Bank
        long accountNumber = 1000000000L + random.nextInt(900000000);
        return "NL" + String.format("%02d", random.nextInt(100)) + bankCode + accountNumber;
    }

    public List<BankAccount> getByOwner(User owner) {
        return bankAccountRepository.findAllByOwner(owner);
    }

    public void save(BankAccount account) {
        bankAccountRepository.save(account);
    }

    public Optional<BankAccount> findById(Long id) {
        return bankAccountRepository.findById(id);
    }
}