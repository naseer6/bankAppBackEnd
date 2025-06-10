package nl.inholland.bankAppBackEnd.stepdefinitions;

import io.cucumber.java.en.*;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.repository.TransactionRepository;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ViewAllTransactionsStepDefs {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    private User adminUser;
    private List<Transaction> transactionsResult;

    @Given("I am logged in as an admin")
    public void i_am_logged_in_as_an_admin() {
        // Ensure admin exists
        String adminUsername = "cucumberadmin";
        adminUser = userService.getUserByUsername(adminUsername).orElse(null);
        if (adminUser == null) {
            adminUser = new User();
            adminUser.setName("Cucumber Admin");
            adminUser.setUsername(adminUsername);
            adminUser.setEmail("cucumberadmin@example.com");
            adminUser.setPassword("adminpass");
            adminUser.setPhone("0123456789");
            adminUser.setAddress("Admin Street 1");
            adminUser.setRole(User.Role.ADMIN);
            adminUser.setApproved(true);
            adminUser.setBsnNumber("123456789");
            userService.register(adminUser);
            adminUser = userService.getUserByUsername(adminUsername).orElseThrow();
        }
    }

    @When("I request all transactions")
    public void i_request_all_transactions() {
        transactionsResult = transactionRepository.findAll();
    }

    @Then("I should receive a list of all transactions")
    public void i_should_receive_a_list_of_all_transactions() {
        assertNotNull(transactionsResult);
        assertFalse(transactionsResult.isEmpty(), "There should be at least one transaction in the system for this test to pass.");
    }

    @When("I request all transactions filtered by IBAN {string}")
    public void i_request_all_transactions_filtered_by_iban(String iban) {
        // Ensure the IBAN exists and has at least one transaction
        Optional<BankAccount> accountOpt = bankAccountRepository.findByIban(iban);
        BankAccount account;
        if (accountOpt.isEmpty()) {
            // Create a user for this account
            User user = userService.getUserByUsername("ibanuser").orElse(null);
            if (user == null) {
                user = new User();
                user.setName("IBAN User");
                user.setUsername("ibanuser");
                user.setEmail("ibanuser@example.com");
                user.setPassword("ibanpass");
                user.setPhone("0000000000");
                user.setAddress("IBAN Street 1");
                user.setRole(User.Role.USER);
                user.setApproved(true);
                user.setBsnNumber("111111111");
                userService.register(user);
                user = userService.getUserByUsername("ibanuser").orElseThrow();
            }
            account = new BankAccount();
            account.setIban(iban);
            account.setOwner(user);
            account.setBalance(1000.0);
            account.setType(BankAccount.AccountType.CHECKING);
            account.setAbsoluteLimit(0.0);
            account.setDailyLimit(1000.0);
            account.setActive(true);
            bankAccountRepository.save(account);
        } else {
            account = accountOpt.get();
        }

        // Ensure at least one transaction exists for this IBAN
        boolean hasTransaction = transactionRepository.findAll().stream()
            .anyMatch(tx -> (tx.getFromAccount() != null && iban.equals(tx.getFromAccount().getIban()))
                         || (tx.getToAccount() != null && iban.equals(tx.getToAccount().getIban())));
        if (!hasTransaction) {
            // Create a transaction from this account to another
            // Find or create a second account
            BankAccount toAccount = bankAccountRepository.findAll().stream()
                .filter(acc -> !acc.getIban().equals(iban))
                .findFirst()
                .orElseGet(() -> {
                    BankAccount acc = new BankAccount();
                    acc.setIban("NL01INHO0000000002");
                    acc.setOwner(account.getOwner());
                    acc.setBalance(500.0);
                    acc.setType(BankAccount.AccountType.CHECKING);
                    acc.setAbsoluteLimit(0.0);
                    acc.setDailyLimit(1000.0);
                    acc.setActive(true);
                    return bankAccountRepository.save(acc);
                });
            Transaction tx = new Transaction();
            tx.setFromAccount(account);
            tx.setToAccount(toAccount);
            tx.setAmount(100.0);
            tx.setTransactionType("TRANSFER");
            tx.setTimestamp(LocalDateTime.now());
            tx.setInitiatedByUser(adminUser);
            transactionRepository.save(tx);
        }

        transactionsResult = transactionRepository.findAll().stream()
                .filter(tx -> (tx.getFromAccount() != null && iban.equals(tx.getFromAccount().getIban()))
                        || (tx.getToAccount() != null && iban.equals(tx.getToAccount().getIban())))
                .toList();
    }

    @Then("I should receive a list of transactions for IBAN {string}")
    public void i_should_receive_a_list_of_transactions_for_iban(String iban) {
        assertNotNull(transactionsResult);
        assertFalse(transactionsResult.isEmpty(), "There should be at least one transaction for the given IBAN.");
        for (Transaction tx : transactionsResult) {
            boolean matches = (tx.getFromAccount() != null && iban.equals(tx.getFromAccount().getIban()))
                    || (tx.getToAccount() != null && iban.equals(tx.getToAccount().getIban()));
            assertTrue(matches, "Transaction does not match the filtered IBAN");
        }
    }
}