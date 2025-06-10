package nl.inholland.bankAppBackEnd.stepdefinitions;

import io.cucumber.java.en.*;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class WithdrawFromAtmStepDefs {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    private String testUsername = "cucumberuser";
    private double withdrawAmount;
    private TransactionService.ATMResult withdrawResult;

    @When("the user withdraws {double} from the ATM for IBAN {string}")
    public void the_user_withdraws_from_the_atm_for_iban(double amount, String iban) {
        User user = userService.getUserByUsername(testUsername).orElseThrow();
        withdrawAmount = amount;
        withdrawResult = transactionService.atmWithdraw(iban, amount, user);
    }

    @Then("the ATM withdrawal should be successful and the new balance should be {double}")
    public void the_atm_withdrawal_should_be_successful_and_the_new_balance_should_be(double expectedBalance) {
        assertNotNull(withdrawResult);
        assertTrue(withdrawResult.isSuccess(), "ATM withdrawal should be successful");
        assertEquals(expectedBalance, withdrawResult.getNewBalance(), 0.01);
    }
}