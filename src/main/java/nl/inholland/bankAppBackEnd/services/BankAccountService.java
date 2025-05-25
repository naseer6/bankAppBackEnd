package nl.inholland.bankAppBackEnd.services;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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
        account.setDailyLimit(1000.0); // Default daily limit
        return bankAccountRepository.save(account);
    }

    public List<BankAccount> createAccountsForUser(User user) {
        return createAccountsForUserWithLimits(user, 0.0, 1000.0);
    }

    public List<BankAccount> createAccountsForUserWithLimits(User user, Double absoluteLimit, Double dailyLimit) {
        BankAccount checking = new BankAccount();
        checking.setIban(generateIban());
        checking.setBalance(0.0);
        checking.setOwner(user);
        checking.setType(BankAccount.AccountType.CHECKING);
        checking.setAbsoluteLimit(absoluteLimit);
        checking.setDailyLimit(dailyLimit);

        BankAccount savings = new BankAccount();
        savings.setIban(generateIban());
        savings.setBalance(0.0);
        savings.setOwner(user);
        savings.setType(BankAccount.AccountType.SAVINGS);
        savings.setAbsoluteLimit(absoluteLimit);
        savings.setDailyLimit(0.0); // Savings accounts typically don't allow transfers

        bankAccountRepository.save(checking);
        bankAccountRepository.save(savings);

        return List.of(checking, savings);
    }

    private String generateIban() {
        // Generate a unique IBAN - check if it already exists
        String iban;
        do {
            iban = "NL" + String.format("%02d", new Random().nextInt(100)) +
                    "INHO" + String.format("%010d", new Random().nextInt(1000000000));
        } while (bankAccountRepository.findByIban(iban).isPresent());

        return iban;
    }

    public List<BankAccount> getByOwner(User owner) {
        return bankAccountRepository.findAllByOwner(owner);
    }

    public void save(BankAccount account) {
        bankAccountRepository.save(account);
    }
}