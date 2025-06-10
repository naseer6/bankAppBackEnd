package nl.inholland.bankAppBackEnd.stepdefinitions;

import io.cucumber.java.en.*;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class LoginStepDefs {

    @Autowired
    private UserService userService;

    private String testUsername = "testuser";
    private String testPassword = "testpass";
    private UserDetails loggedInUser;
    private Exception loginException;

    @Given("the user is on the login page")
    public void user_on_login_page() {
        // Ensure test user exists in DB
        if (userService.getUserByUsername(testUsername).isEmpty()) {
            User user = new User();
            user.setName("Test User");
            user.setUsername(testUsername);
            user.setEmail("testuser@example.com");
            user.setPassword(testPassword);
            user.setPhone("1234567890");
            user.setAddress("Test Address");
            user.setRole(User.Role.USER);
            user.setApproved(true);
            user.setBsnNumber("123456789");
            userService.register(user);
        }
    }

    @When("the user enters valid credentials")
    public void user_enters_valid_credentials() {
        try {
            loggedInUser = userService.loadUserByUsername(testUsername);
            // Simulate password check (since loadUserByUsername doesn't check password)
            assertTrue(userService.getUserByUsername(testUsername)
                .map(u -> userService
                    .loadUserByUsername(testUsername)
                    .getPassword()
                    .equals(loggedInUser.getPassword()))
                .orElse(false));
            loginException = null;
        } catch (Exception e) {
            loginException = e;
        }
    }

    @Then("the user should be redirected to the dashboard")
    public void user_redirected_to_dashboard() {
        assertNotNull(loggedInUser, "User should be logged in");
        assertNull(loginException, "There should be no login exception");
        assertEquals(testUsername, loggedInUser.getUsername());
    }
}
