package nl.inholland.bankAppBackEnd.test;

import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import nl.inholland.bankAppBackEnd.services.UserService;
import nl.inholland.bankAppBackEnd.DTOs.TransactionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit tests for validating core bank functionality.
 * Tests customer registration, account creation, transfers, and limit enforcement.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BankFunctionalityTest {

    @Autowired
    private UserService userService;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionService transactionService;

    private User customer;
    // Employee is only used in setup, can be a local variable there

    private BankAccount checkingAccount;
    private BankAccount savingsAccount;

    @BeforeEach
    public void setup() {
        // Create test customer
        customer = new User();
        customer.setName("Test Customer");
        customer.setUsername("testcustomer");
        customer.setEmail("customer@test.com");
        customer.setPassword("password");
        customer.setPhone("1234567890");
        customer.setAddress("Test Address");
        customer.setBsnNumber("123456789");
        customer.setRole(User.Role.CUSTOMER);
        customer.setApproved(false);
        customer = userService.save(customer);

        // Create test employee (as a local variable since it's only used here)
        User employee = new User();
        employee.setName("Test Employee");
        employee.setUsername("testemployee");
        employee.setEmail("employee@test.com");
        employee.setPassword("password");
        employee.setPhone("0987654321");
        employee.setAddress("Employee Address");
        employee.setBsnNumber("987654321");
        employee.setRole(User.Role.EMPLOYEE);
        employee.setApproved(true);
        userService.save(employee);
    }

    @Test
    public void testCustomerRegistrationAndApproval() {
        // Verify customer starts as unapproved
        assertFalse(customer.isApproved());

        // Approve the customer
        customer.setApproved(true);
        customer = userService.save(customer);

        // Create accounts for the customer
        List<BankAccount> accounts = bankAccountService.createAccountsForUser(customer);
        assertEquals(2, accounts.size());

        // Verify account details
        checkingAccount = accounts.stream()
                .filter(a -> a.getType() == BankAccount.AccountType.CHECKING)
                .findFirst()
                .orElse(null);

        savingsAccount = accounts.stream()
                .filter(a -> a.getType() == BankAccount.AccountType.SAVINGS)
                .findFirst()
                .orElse(null);

        assertNotNull(checkingAccount);
        assertNotNull(savingsAccount);
        assertTrue(checkingAccount.getIban().startsWith("NL"));
        assertTrue(savingsAccount.getIban().startsWith("NL"));
        assertEquals(0.0, checkingAccount.getBalance());
        assertEquals(0.0, savingsAccount.getBalance());
    }

    @Test
    public void testTransferBetweenOwnAccounts() {
        // Approve customer and create accounts
        customer.setApproved(true);
        customer = userService.save(customer);
        List<BankAccount> accounts = bankAccountService.createAccountsForUser(customer);
        checkingAccount = accounts.getFirst();
        savingsAccount = accounts.get(1);

        // Add initial balance to checking account
        checkingAccount.setBalance(1000.0);
        // Use the repository method via the service instead of a direct save()
        bankAccountRepository.save(checkingAccount);

        // Create transfer DTO
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setFromIban(checkingAccount.getIban());
        transactionDTO.setToIban(savingsAccount.getIban());
        transactionDTO.setAmount(500.0);
        transactionDTO.setDescription("Test transfer");
        transactionDTO.setInitiatedBy(customer.getUsername()); // Using initiatedBy instead of performedBy

        // Process the transfer
        transactionService.createTransaction(transactionDTO);

        // Refresh accounts from database
        checkingAccount = bankAccountService.getAccountById(checkingAccount.getId());
        savingsAccount = bankAccountService.getAccountById(savingsAccount.getId());

        // Verify balances
        assertEquals(500.0, checkingAccount.getBalance());
        assertEquals(500.0, savingsAccount.getBalance());
    }

    @Test
    public void testTransferLimitEnforcement() {
        // Approve customer and create accounts with specific limits
        customer.setApproved(true);
        customer = userService.save(customer);
        List<BankAccount> accounts = bankAccountService.createAccountsForUserWithLimits(customer, 100.0, 500.0);
        checkingAccount = accounts.getFirst();
        savingsAccount = accounts.get(1);

        // Add initial balance to checking account
        checkingAccount.setBalance(1000.0);
        bankAccountRepository.save(checkingAccount);

        // Create transaction DTO that exceeds daily limit
        TransactionDTO dailyLimitTx = new TransactionDTO();
        dailyLimitTx.setFromIban(checkingAccount.getIban());
        dailyLimitTx.setToIban(savingsAccount.getIban());
        dailyLimitTx.setAmount(600.0); // Exceeds 500.0 daily limit
        dailyLimitTx.setDescription("Test daily limit");
        dailyLimitTx.setInitiatedBy(customer.getUsername());

        // Create transaction DTO that would violate absolute limit
        TransactionDTO absoluteLimitTx = new TransactionDTO();
        absoluteLimitTx.setFromIban(checkingAccount.getIban());
        absoluteLimitTx.setToIban(savingsAccount.getIban());
        absoluteLimitTx.setAmount(950.0); // Would leave 50.0, below 100.0 absolute limit
        absoluteLimitTx.setDescription("Test absolute limit");
        absoluteLimitTx.setInitiatedBy(customer.getUsername());

        // Assert that both transfers throw appropriate exceptions
        Exception dailyLimitException = assertThrows(IllegalStateException.class,
            () -> transactionService.createTransaction(dailyLimitTx));
        assertTrue(dailyLimitException.getMessage().toLowerCase().contains("daily limit"));

        Exception absoluteLimitException = assertThrows(IllegalStateException.class,
            () -> transactionService.createTransaction(absoluteLimitTx));
        assertTrue(absoluteLimitException.getMessage().toLowerCase().contains("absolute limit"));
    }

    @Test
    public void testTransactionFiltering() {
        // Approve customer and create accounts
        customer.setApproved(true);
        customer = userService.save(customer);
        List<BankAccount> accounts = bankAccountService.createAccountsForUser(customer);
        checkingAccount = accounts.getFirst();
        savingsAccount = accounts.get(1);

        // Add initial balance to checking account
        checkingAccount.setBalance(1000.0);
        bankAccountRepository.save(checkingAccount);

        // Create several test transactions
        for (double amount : new double[]{200.0, 300.0, 100.0}) {
            TransactionDTO tx = new TransactionDTO();
            tx.setFromIban(checkingAccount.getIban());
            tx.setToIban(savingsAccount.getIban());
            tx.setAmount(amount);
            tx.setDescription("Test transaction of " + amount);
            tx.setInitiatedBy(customer.getUsername());
            transactionService.createTransaction(tx);
        }

        // Filter by date (today)
        List<Transaction> todayTransactions = transactionService.filterTransactions(
                customer, LocalDate.now(), LocalDate.now(), null, null, null);
        assertEquals(3, todayTransactions.size());

        // Filter by amount
        List<Transaction> largeTransactions = transactionService.filterTransactions(
                customer, null, null, 250.0, null, null);
        assertEquals(1, largeTransactions.size());
        assertEquals(300.0, largeTransactions.getFirst().getAmount());

        // Filter by IBAN
        List<Transaction> accountTransactions = transactionService.filterTransactions(
                customer, null, null, null, null, checkingAccount.getIban());
        assertEquals(3, accountTransactions.size());
    }

    @Test
    public void testAtmOperations() {
        // Approve customer and create accounts
        customer.setApproved(true);
        customer = userService.save(customer);
        List<BankAccount> accounts = bankAccountService.createAccountsForUser(customer);
        checkingAccount = accounts.getFirst();

        // Create and save deposit transaction
        Transaction deposit = new Transaction();
        deposit.setToAccount(checkingAccount);
        deposit.setAmount(500.0);
        deposit.setTransactionType("DEPOSIT");
        deposit.setInitiatedByUser(customer);
        deposit.setTimestamp(LocalDateTime.now());
        transactionService.saveTransaction(deposit);

        // Update account balance
        checkingAccount.setBalance(500.0);
        bankAccountRepository.save(checkingAccount);

        // Create and save withdrawal transaction
        Transaction withdrawal = new Transaction();
        withdrawal.setFromAccount(checkingAccount);
        withdrawal.setAmount(200.0);
        withdrawal.setTransactionType("WITHDRAWAL");
        withdrawal.setInitiatedByUser(customer);
        withdrawal.setTimestamp(LocalDateTime.now());
        transactionService.saveTransaction(withdrawal);

        // Update account balance
        checkingAccount.setBalance(300.0);
        bankAccountRepository.save(checkingAccount);

        // Get ATM transactions
        List<Transaction> atmTransactions = transactionService.getTransactionsByUser(customer);
        assertEquals(2, atmTransactions.size());
    }
}
