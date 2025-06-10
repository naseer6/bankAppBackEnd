package nl.inholland.bankAppBackEnd.stepdefinitions;

import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SetAccountLimitStepDefs {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    private String lastIban;
    private Double lastAbsoluteLimit;
    private Double lastDailyLimit;

    @When("the admin sets the absolute limit to {double} and daily limit to {double} for IBAN {string}")
    public void the_admin_sets_absolute_and_daily_limit(Double absoluteLimit, Double dailyLimit, String iban) {
        // Simulate admin privilege: directly update limits
        BankAccount account = bankAccountRepository.findByIban(iban).orElseThrow();
        account.setAbsoluteLimit(absoluteLimit);
        account.setDailyLimit(dailyLimit);
        bankAccountRepository.save(account);
        lastIban = iban;
        lastAbsoluteLimit = absoluteLimit;
        lastDailyLimit = dailyLimit;
    }

    @When("the admin sets the daily limit to {double} for IBAN {string}")
    public void the_admin_sets_daily_limit(Double dailyLimit, String iban) {
        BankAccount account = bankAccountRepository.findByIban(iban).orElseThrow();
        account.setDailyLimit(dailyLimit);
        bankAccountRepository.save(account);
        lastIban = iban;
        lastDailyLimit = dailyLimit;
    }

    @When("the admin sets the absolute limit to {double} for IBAN {string}")
    public void the_admin_sets_absolute_limit(Double absoluteLimit, String iban) {
        BankAccount account = bankAccountRepository.findByIban(iban).orElseThrow();
        account.setAbsoluteLimit(absoluteLimit);
        bankAccountRepository.save(account);
        lastIban = iban;
        lastAbsoluteLimit = absoluteLimit;
    }

    @Then("the account with IBAN {string} should have absolute limit {double} and daily limit {double}")
    public void the_account_should_have_absolute_and_daily_limit(String iban, Double absoluteLimit, Double dailyLimit) {
        BankAccount account = bankAccountRepository.findByIban(iban).orElseThrow();
        assertEquals(absoluteLimit, account.getAbsoluteLimit());
        assertEquals(dailyLimit, account.getDailyLimit());
    }

    @Then("the account with IBAN {string} should have daily limit {double}")
    public void the_account_should_have_daily_limit(String iban, Double dailyLimit) {
        BankAccount account = bankAccountRepository.findByIban(iban).orElseThrow();
        assertEquals(dailyLimit, account.getDailyLimit());
    }

    @Then("the account with IBAN {string} should have absolute limit {double}")
    public void the_account_should_have_absolute_limit(String iban, Double absoluteLimit) {
        BankAccount account = bankAccountRepository.findByIban(iban).orElseThrow();
        assertEquals(absoluteLimit, account.getAbsoluteLimit());
    }
}
