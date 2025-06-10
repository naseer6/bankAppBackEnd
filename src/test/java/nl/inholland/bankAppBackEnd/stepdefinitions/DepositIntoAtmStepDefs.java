package nl.inholland.bankAppBackEnd.stepdefinitions;

import io.cucumber.java.en.*;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
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
    private TransactionService transactionService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    private String testUsername = "cucumberuser";
    private double depositAmount;
    private TransactionService.ATMResult depositResult;

    @When("the user deposits {double} into the ATM for IBAN {string}")
    public void the_user_deposits_into_the_atm_for_iban(double amount, String iban) {
        User user = userService.getUserByUsername(testUsername).orElseThrow();
        // Debug: print user and account owner IDs
        BankAccount account = bankAccountRepository.findByIban(iban).orElse(null);
        if (account != null) {
            System.out.println("ATM deposit: userId=" + user.getId() + ", accountOwnerId=" + account.getOwner().getId());
        }
        depositAmount = amount;
        depositResult = transactionService.atmDeposit(iban, amount, user);
    }

    @Then("the ATM deposit should be successful and the new balance should be {double}")
    public void the_atm_deposit_should_be_successful_and_the_new_balance_should_be(double expectedBalance) {
        assertNotNull(depositResult);
        assertTrue(depositResult.isSuccess(), "ATM deposit should be successful");
        assertEquals(expectedBalance, depositResult.getNewBalance(), 0.01);
    }
}
