package nl.inholland.bankAppBackEnd.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ViewAllAccountsStepDefs {

    @Mock
    private BankAccountService bankAccountService;
    
    private List<BankAccount> accountList;
    private ResponseEntity<?> responseEntity;
    private HttpStatus responseStatus;
    private AutoCloseable closeable;

    public ViewAllAccountsStepDefs() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @Given("the system has bank accounts")
    public void theSystemHasBankAccounts() {
        accountList = new ArrayList<>();
        
        // Create sample bank accounts for testing
        User user1 = new User();
        user1.setId(1L);
        user1.setName("Test User 1");
        
        BankAccount account1 = new BankAccount();
        account1.setId(1L);
        account1.setIban("NL01INHO0123456789");
        account1.setBalance(1000.0);
        account1.setType(BankAccount.AccountType.CHECKING);
        account1.setOwner(user1);
        
        BankAccount account2 = new BankAccount();
        account2.setId(2L);
        account2.setIban("NL02INHO0987654321");
        account2.setBalance(500.0);
        account2.setType(BankAccount.AccountType.SAVINGS);
        account2.setOwner(user1);
        
        accountList.add(account1);
        accountList.add(account2);
        
        when(bankAccountService.getAllAccounts()).thenReturn(accountList);
    }

    @Given("the system has no bank accounts")
    public void theSystemHasNoBankAccounts() {
        accountList = new ArrayList<>();
        when(bankAccountService.getAllAccounts()).thenReturn(accountList);
    }

    @Given("a user is not authenticated")
    public void aUserIsNotAuthenticated() {
        SecurityContextHolder.clearContext();
    }

    @Given("a user with role {string} is authenticated")
    public void aUserWithRoleIsAuthenticated(String role) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "testuser",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @When("the admin requests to view all accounts")
    public void theAdminRequestsToViewAllAccounts() {
        // Setup admin authentication
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // Simulate controller call
        try {
            List<BankAccount> accounts = bankAccountService.getAllAccounts();
            responseEntity = new ResponseEntity<>(accounts, HttpStatus.OK);
            responseStatus = HttpStatus.OK;
        } catch (Exception e) {
            responseEntity = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            responseStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    @When("the user requests to view all accounts")
    public void theUserRequestsToViewAllAccounts() {
        // Simulate controller call with current authentication
        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null ||
                    !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
                responseEntity = new ResponseEntity<>("Not authenticated", HttpStatus.UNAUTHORIZED);
                responseStatus = HttpStatus.UNAUTHORIZED;
            } else {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                boolean isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                
                if (!isAdmin) {
                    responseEntity = new ResponseEntity<>("Access denied", HttpStatus.FORBIDDEN);
                    responseStatus = HttpStatus.FORBIDDEN;
                } else {
                    List<BankAccount> accounts = bankAccountService.getAllAccounts();
                    responseEntity = new ResponseEntity<>(accounts, HttpStatus.OK);
                    responseStatus = HttpStatus.OK;
                }
            }
        } catch (Exception e) {
            responseEntity = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            responseStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    @Then("all bank accounts in the system are returned")
    public void allBankAccountsInTheSystemAreReturned() {
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<BankAccount> returnedAccounts = (List<BankAccount>) responseEntity.getBody();
        assertEquals(accountList.size(), returnedAccounts.size());
        
        // Verify the service was called
        verify(bankAccountService, times(1)).getAllAccounts();
    }

    @Then("an empty list of accounts is returned")
    public void anEmptyListOfAccountsIsReturned() {
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<BankAccount> returnedAccounts = (List<BankAccount>) responseEntity.getBody();
        assertTrue(returnedAccounts.isEmpty());
        
        // Verify the service was called
        verify(bankAccountService, times(1)).getAllAccounts();
    }

    @Then("the request is denied")
    public void theRequestIsDenied() {
        assertNotEquals(HttpStatus.OK, responseStatus);
    }

    @And("the response status is {int} {word}")
    public void theResponseStatusIs(int statusCode, String statusText) {
        HttpStatus expectedStatus = HttpStatus.valueOf(statusCode);
        assertEquals(expectedStatus, responseStatus);
    }
}
