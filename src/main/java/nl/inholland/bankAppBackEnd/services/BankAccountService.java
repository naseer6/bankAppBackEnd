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
        return bankAccountRepository.save(account);
    }

    public List<BankAccount> createAccountsForUser(User user) {
        BankAccount checking = new BankAccount();
        checking.setIban(generateIban());
        checking.setBalance(0.0);
        checking.setOwner(user);
        checking.setType(BankAccount.AccountType.CHECKING);

        BankAccount savings = new BankAccount();
        savings.setIban(generateIban());
        savings.setBalance(0.0);
        savings.setOwner(user);
        savings.setType(BankAccount.AccountType.SAVINGS);

        bankAccountRepository.save(checking);
        bankAccountRepository.save(savings);

        return List.of(checking, savings);
    }


    private String generateIban() {
        return "NL" + new Random().nextInt(90000000) + 10000000; // simple random IBAN
    }

    public List<BankAccount> getByOwner(User owner) {
        return bankAccountRepository.findAllByOwner(owner);
    }

    public void save(BankAccount account) {
        bankAccountRepository.save(account);
    }

}