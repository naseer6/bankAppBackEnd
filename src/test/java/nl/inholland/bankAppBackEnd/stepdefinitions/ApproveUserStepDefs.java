package nl.inholland.bankAppBackEnd.stepdefinitions;

import io.cucumber.java.en.*;
import io.cucumber.java.After;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import nl.inholland.bankAppBackEnd.Controllers.AdminController;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ApproveUserStepDefs {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private AdminController adminController;

    private String testUsername = "pendinguser";
    private Long testUserId;
    private ResponseEntity<String> approveResponse;

    @Given("there is a pending user with username {string}")
    public void there_is_a_pending_user_with_username(String username) {
        // Clean up if user exists
        userRepository.findByUsername(username).ifPresent(u -> {
            List<BankAccount> accounts = bankAccountRepository.findAllByOwner(u);
            bankAccountRepository.deleteAll(accounts);
            userRepository.delete(u);
        });

        User user = new User();
        user.setName("Pending User");
        user.setUsername(username);
        user.setEmail("pendinguser@example.com");
        user.setPassword("testpass");
        user.setPhone("0123456789");
        user.setAddress("Pending Street 1");
        user.setRole(User.Role.USER);
        user.setApproved(false);
        user.setBsnNumber("123456789");
        userRepository.save(user);
        testUserId = userRepository.findByUsername(username).get().getId();
    }

    @When("the admin approves the user with username {string}")
    public void the_admin_approves_the_user_with_username(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        assertTrue(userOpt.isPresent(), "User should exist");
        testUserId = userOpt.get().getId();
        approveResponse = adminController.approveUser(testUserId);
    }

    @Then("the user with username {string} should be approved")
    public void the_user_should_be_approved(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        assertTrue(userOpt.isPresent(), "User should exist");
        assertTrue(userOpt.get().isApproved(), "User should be approved");
        assertEquals(200, approveResponse.getStatusCodeValue());
    }

    @Then("a bank account should be created for the user")
    public void a_bank_account_should_be_created_for_the_user() {
        Optional<User> userOpt = userRepository.findByUsername(testUsername);
        assertTrue(userOpt.isPresent(), "User should exist");
        List<BankAccount> accounts = bankAccountRepository.findAllByOwner(userOpt.get());
        assertFalse(accounts.isEmpty(), "Bank account should be created for the user");
    }

    @After
    public void cleanup() {
        userRepository.findByUsername(testUsername).ifPresent(u -> {
            List<BankAccount> accounts = bankAccountRepository.findAllByOwner(u);
            bankAccountRepository.deleteAll(accounts);
            userRepository.delete(u);
        });
    }
}
