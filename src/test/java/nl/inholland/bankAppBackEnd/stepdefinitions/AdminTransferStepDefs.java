package nl.inholland.bankAppBackEnd.stepdefinitions;

import io.cucumber.java.en.*;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AdminTransferStepDefs {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionService transactionService;

    private User admin;
    private String fromIban;
    private String toIban;
    private double amount;
    private TransactionService.TransferResult transferResult;

    @Given("an admin is authenticated")
    public void an_admin_is_authenticated() {
        admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin");
            admin.setPassword("adminpass");
            admin.setName("Admin User");
            admin.setRole(User.Role.ADMIN);
            admin.setApproved(true);
            admin.setEmail("admin@bank.com");
            admin.setPhone("0000000000");
            admin.setAddress("Admin Street 1");
            admin.setBsnNumber("111111111");
            userRepository.save(admin);
        }
    }

    @And("there is a source account with IBAN {string} and a destination account with IBAN {string}")
    public void there_is_a_source_and_destination_account(String fromIban, String toIban) {
        this.fromIban = fromIban;
        this.toIban = toIban;

        // Set up balances for scenarios
        double sourceBalance = 1000.0;
        if (fromIban.endsWith("0003")) {
            sourceBalance = 10.0; // For insufficient funds scenario
        }

        BankAccount from = bankAccountRepository.findByIban(fromIban).orElse(null);
        if (from == null) {
            from = new BankAccount();
            from.setIban(fromIban);
            from.setOwner(admin);
            from.setType(BankAccount.AccountType.CHECKING);
        }
        from.setBalance(sourceBalance);
        from.setAbsoluteLimit(0.0);
        from.setDailyLimit(10000.0);
        from.setActive(true);
        bankAccountRepository.save(from);

        BankAccount to = bankAccountRepository.findByIban(toIban).orElse(null);
        if (to == null) {
            to = new BankAccount();
            to.setIban(toIban);
            to.setOwner(admin);
            to.setType(BankAccount.AccountType.CHECKING);
        }
        to.setBalance(1000.0);
        to.setAbsoluteLimit(0.0);
        to.setDailyLimit(10000.0);
        to.setActive(true);
        bankAccountRepository.save(to);
    }

    @And("the transfer amount is {double}")
    public void the_transfer_amount_is(Double amount) {
        this.amount = amount;
    }

    @When("the admin initiates a transfer")
    public void the_admin_initiates_a_transfer() {
        transferResult = transactionService.transferFunds(fromIban, toIban, amount, admin);
    }

    @Then("the transfer should be successful")
    public void the_transfer_should_be_successful() {
        assertNotNull(transferResult);
        assertTrue(transferResult.isSuccess());
    }
}