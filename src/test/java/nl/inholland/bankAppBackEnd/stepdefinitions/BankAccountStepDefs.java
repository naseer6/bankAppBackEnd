package nl.inholland.bankAppBackEnd.stepdefinitions;

import io.cucumber.java.en.Given;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BankAccountStepDefs {

    @Autowired
    private UserService userService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    private String testUsername = "cucumberuser";

    @Given("the user has a bank account with IBAN {string} and balance {double}")
    public void the_user_has_a_bank_account_with_iban_and_balance(String iban, double balance) {
        User user = userService.getUserByUsername(testUsername).orElse(null);
        if (user == null) {
            user = new User();
            user.setName("Cucumber User");
            user.setUsername(testUsername);
            user.setEmail("cucumber@example.com");
            user.setPassword("cucumberpass");
            user.setPhone("0987654321");
            user.setAddress("Cucumber Street 123");
            user.setRole(User.Role.USER);
            user.setApproved(true);
            user.setBsnNumber("987654321");
            userService.register(user);
            user = userService.getUserByUsername(testUsername).orElseThrow();
        } else {
            user = userService.getUserByUsername(testUsername).orElseThrow();
        }

        // Always delete any existing account with this IBAN before creating a new one
        bankAccountRepository.findByIban(iban).ifPresent(bankAccountRepository::delete);

        BankAccount account = new BankAccount();
        account.setIban(iban);
        account.setOwner(user);
        account.setBalance(balance);
        account.setType(BankAccount.AccountType.CHECKING);
        account.setAbsoluteLimit(0.0);
        account.setDailyLimit(1000.0);
        account.setActive(true);
        bankAccountRepository.save(account);
    }
}