package nl.inholland.bankAppBackEnd.stepdefinitions;

import io.cucumber.java.After;
import io.cucumber.java.en.*;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.services.UserService;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RegisterStepDefs {

    @Autowired
    private UserService userService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private String regUsername = "cucumberuser";
    private String regPassword = "cucumberpass";
    private String regEmail = "cucumber@example.com";
    private Exception registrationException;

    @Given("the user is on the registration page")
    public void user_on_registration_page() {
        // Clean up any existing user and their accounts before registering
        userService.getUserByUsername(regUsername).ifPresent(u -> {
            List<BankAccount> accounts = bankAccountRepository.findAllByOwner(u);
            for (BankAccount account : accounts) {
                List<Transaction> transactions = transactionRepository.findByFromAccountIdOrToAccountIdOrderByTimestampDesc(account.getId(), account.getId());
                transactionRepository.deleteAll(transactions);
            }
            bankAccountRepository.deleteAll(accounts);
            userService.deleteUserByUsername(regUsername);
        });
    }

    @When("the user enters valid registration details")
    public void user_enters_valid_registration_details() {
        try {
            User user = new User();
            user.setName("Cucumber User");
            user.setUsername(regUsername);
            user.setEmail(regEmail);
            user.setPassword(regPassword);
            user.setPhone("0987654321");
            user.setAddress("Cucumber Street 123");
            user.setRole(User.Role.USER);
            user.setApproved(true);
            user.setBsnNumber("987654321");
            userService.register(user);
            registrationException = null;
        } catch (Exception e) {
            registrationException = e;
        }
    }

    @Then("the user account should be created")
    public void user_account_should_be_created() {
        assertNull(registrationException, "Registration should not throw exception");
        assertTrue(userService.getUserByUsername(regUsername).isPresent(), "User should be registered");
    }

    @After
    public void cleanupRegisteredUser() {
        userService.getUserByUsername(regUsername).ifPresent(u -> {
            List<BankAccount> accounts = bankAccountRepository.findAllByOwner(u);
            for (BankAccount account : accounts) {
                List<Transaction> transactions = transactionRepository.findByFromAccountIdOrToAccountIdOrderByTimestampDesc(account.getId(), account.getId());
                transactionRepository.deleteAll(transactions);
            }
            bankAccountRepository.deleteAll(accounts);
            userService.deleteUserByUsername(regUsername);
        });
    }
}
