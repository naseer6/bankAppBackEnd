package nl.inholland.bankAppBackEnd.stepdefinitions;

import io.cucumber.java.en.*;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DepositIntoAtmStepDefs {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    private String testUsername = "cucumberuser";
    private double depositAmount;
    private TransactionService.ATMResult depositResult;

    // Rename the method and change the step text to avoid duplicate definition
    @Given("the user has an active bank account with IBAN {string} and balance {double}")
    public void the_user_has_an_active_bank_account_with_iban_and_balance(String iban, double balance) {
        User user = userService.getUserByUsername(testUsername).orElseThrow();
        
        // Ensure the user is approved for transactions
        if (!user.isApproved()) {
            user.setApproved(true);
            // Use userRepository to save user changes 
            userRepository.save(user);
        }
        
        BankAccount account = bankAccountRepository.findByIban(iban).orElse(null);
        
        if (account == null) {
            // Create a new account if it doesn't exist
            account = new BankAccount();
            account.setIban(iban);
            account.setBalance(balance);
            account.setOwner(user);
            account.setActive(true); // Set account to active explicitly
        } else {
            // Update existing account
            account.setBalance(balance);
            account.setActive(true); // Ensure account is active
            account.setOwner(user);
        }
        
        bankAccountRepository.save(account);
        
        // Debug information
        System.out.println("Test account setup: IBAN=" + iban + ", Balance=" + balance + 
                          ", Active=" + account.isActive() + ", Owner approved=" + user.isApproved());
    }

    @When("the user deposits {double} into the ATM for IBAN {string}")
    public void the_user_deposits_into_the_atm_for_iban(double amount, String iban) {
        User user = userService.getUserByUsername(testUsername).orElseThrow();
        
        // Debug: print user and account details
        BankAccount account = bankAccountRepository.findByIban(iban).orElse(null);
        if (account != null) {
            System.out.println("ATM deposit: userId=" + user.getId() + ", accountOwnerId=" + account.getOwner().getId() + 
                              ", accountActive=" + account.isActive() + ", userApproved=" + user.isApproved());
        }
        
        depositAmount = amount;
        depositResult = transactionService.atmDeposit(iban, amount, user);
        
        // Debug deposit result
        System.out.println("=== ATM Deposit Debug ===");
        System.out.println("Deposit success: " + depositResult.isSuccess());
        System.out.println("Deposit message: " + depositResult.getMessage());
        System.out.println("New balance: " + depositResult.getNewBalance());
    }

    @Then("the ATM deposit should be successful and the new balance should be {double}")
    public void the_atm_deposit_should_be_successful_and_the_new_balance_should_be(double expectedBalance) {
        assertNotNull(depositResult);
        assertTrue(depositResult.isSuccess(), "ATM deposit should be successful");
        assertEquals(expectedBalance, depositResult.getNewBalance(), 0.01);
    }
}
