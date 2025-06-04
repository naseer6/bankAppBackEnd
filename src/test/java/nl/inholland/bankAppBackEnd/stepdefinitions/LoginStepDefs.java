package nl.inholland.bankAppBackEnd.stepdefinitions;

import io.cucumber.java.en.*;

public class LoginStepDefs {

    @Given("the user is on the login page")
    public void user_on_login_page() {
        System.out.println("User is on the login page");
    }

    @When("the user enters valid credentials")
    public void user_enters_valid_credentials() {
        System.out.println("User enters valid credentials");
    }

    @Then("the user should be redirected to the dashboard")
    public void user_redirected_to_dashboard() {
        System.out.println("User is redirected to the dashboard");
    }
}
