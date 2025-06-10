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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ViewPersonalTransactionsStepDefs {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    private User testUser;
    private List<Transaction> personalTransactions;
    private Page<Transaction> pagedTransactions;

    @Given("I am logged in as a user")
    public void i_am_logged_in_as_a_user() {
        String testUsername = "cucumberuser";
        testUser = userService.getUserByUsername(testUsername).orElse(null);
        if (testUser == null) {
            testUser = new User();
            testUser.setName("Cucumber User");
            testUser.setUsername(testUsername);
            testUser.setEmail("cucumber@example.com");
            testUser.setPassword("cucumberpass");
            testUser.setPhone("0987654321");
            testUser.setAddress("Cucumber Street 123");
            testUser.setRole(User.Role.USER);
            testUser.setApproved(true);
            testUser.setBsnNumber("987654321");
            userService.register(testUser);
            testUser = userService.getUserByUsername(testUsername).orElseThrow();
        }
    }

    @When("I request my personal transactions")
    public void i_request_my_personal_transactions() {
        // Ensure the user has at least one bank account
        List<BankAccount> accounts = bankAccountRepository.findAllByOwner(testUser);
        BankAccount userAccount;
        if (accounts.isEmpty()) {
            userAccount = new BankAccount();
            userAccount.setIban("NL01INHO0000000003");
            userAccount.setOwner(testUser);
            userAccount.setBalance(1000.0);
            userAccount.setType(BankAccount.AccountType.CHECKING);
            userAccount.setAbsoluteLimit(0.0);
            userAccount.setDailyLimit(1000.0);
            userAccount.setActive(true);
            bankAccountRepository.save(userAccount);
        } else {
            userAccount = accounts.get(0);
        }

        // Ensure at least one transaction exists for the user
        personalTransactions = transactionRepository.findByAccountOwner(testUser);
        if (personalTransactions.isEmpty()) {
            // Create a second account to transfer to/from
            BankAccount otherAccount = bankAccountRepository.findAll().stream()
                .filter(acc -> !acc.getOwner().getId().equals(testUser.getId()))
                .findFirst()
                .orElseGet(() -> {
                    User otherUser = userService.getUserByUsername("otheruser").orElse(null);
                    if (otherUser == null) {
                        otherUser = new User();
                        otherUser.setName("Other User");
                        otherUser.setUsername("otheruser");
                        otherUser.setEmail("otheruser@example.com");
                        otherUser.setPassword("otherpass");
                        otherUser.setPhone("0000000000");
                        otherUser.setAddress("Other Street 1");
                        otherUser.setRole(User.Role.USER);
                        otherUser.setApproved(true);
                        otherUser.setBsnNumber("222222222");
                        userService.register(otherUser);
                        otherUser = userService.getUserByUsername("otheruser").orElseThrow();
                    }
                    BankAccount acc = new BankAccount();
                    acc.setIban("NL01INHO0000000004");
                    acc.setOwner(otherUser);
                    acc.setBalance(1000.0);
                    acc.setType(BankAccount.AccountType.CHECKING);
                    acc.setAbsoluteLimit(0.0);
                    acc.setDailyLimit(1000.0);
                    acc.setActive(true);
                    return bankAccountRepository.save(acc);
                });

            Transaction tx = new Transaction();
            tx.setFromAccount(userAccount);
            tx.setToAccount(otherAccount);
            tx.setAmount(50.0);
            tx.setTransactionType("TRANSFER");
            tx.setTimestamp(LocalDateTime.now());
            tx.setInitiatedByUser(testUser);
            transactionRepository.save(tx);

            personalTransactions = transactionRepository.findByAccountOwner(testUser);
        }
    }

    @Then("I should receive a list of my transactions")
    public void i_should_receive_a_list_of_my_transactions() {
        assertNotNull(personalTransactions);
        assertFalse(personalTransactions.isEmpty(), "User should have at least one transaction for this test to pass.");
        for (Transaction tx : personalTransactions) {
            boolean isMine = (tx.getFromAccount() != null && tx.getFromAccount().getOwner().getId().equals(testUser.getId()))
                    || (tx.getToAccount() != null && tx.getToAccount().getOwner().getId().equals(testUser.getId()));
            assertTrue(isMine, "Transaction does not belong to the user");
        }
    }

    @When("I request my personal transactions with page {int} and size {int}")
    public void i_request_my_personal_transactions_with_page_and_size(int page, int size) {
        // Ensure the user has at least one transaction for pagination as well
        i_request_my_personal_transactions();
        pagedTransactions = transactionRepository.findByAccountOwner(testUser, PageRequest.of(page, size));
    }

    @Then("I should receive a paginated list of my transactions with size {int}")
    public void i_should_receive_a_paginated_list_of_my_transactions_with_size(int size) {
        assertNotNull(pagedTransactions);
        assertTrue(pagedTransactions.getContent().size() <= size, "Page size should not exceed requested size");
        for (Transaction tx : pagedTransactions.getContent()) {
            boolean isMine = (tx.getFromAccount() != null && tx.getFromAccount().getOwner().getId().equals(testUser.getId()))
                    || (tx.getToAccount() != null && tx.getToAccount().getOwner().getId().equals(testUser.getId()));
            assertTrue(isMine, "Transaction does not belong to the user");
        }
    }
}