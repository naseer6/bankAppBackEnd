package nl.inholland.bankAppBackEnd.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.inholland.bankAppBackEnd.config.JwtAuthenticationFilter;
import nl.inholland.bankAppBackEnd.config.JwtUtil;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import nl.inholland.bankAppBackEnd.services.UserService;
import nl.inholland.bankAppBackEnd.models.Transaction;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BankAccountController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class BankAccountControllerTest {
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BankAccountService bankAccountService;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})

    void createAccount_ShouldReturnOk_WhenValidUserAndAdmin() throws Exception {
        // Mock the currently authenticated user (admin)
        User admin = new User();
        admin.setId(99L);
        admin.setUsername("admin");
        admin.setRole(User.Role.ADMIN);
        admin.setApproved(true);

        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(admin));

        // Mock the user for whom the account is being created
        User targetUser = new User();
        targetUser.setId(1L);
        targetUser.setUsername("user");
        targetUser.setApproved(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(targetUser));

        // Mock service layer
        when(bankAccountService.createAccountsForUserWithLimits(any(), anyDouble(), anyDouble()))
                .thenReturn(List.of(new BankAccount()));

        // Perform request
        mockMvc.perform(post("/api/accounts/create")
                        .param("userId", "1")
                        .param("absoluteLimit", "0.0")
                        .param("dailyLimit", "1000.0"))
                .andExpect(status().isOk());
    }

    @Test
    void transferFunds_ShouldReturnOk_WhenTransferSuccessful() throws Exception {
        // Arrange
        String fromIban = "NL01BANK0123456789";
        String toIban = "NL01BANK9876543210";
        double amount = 100.0;

        User mockUser = new User();
        mockUser.setUsername("admin");
        mockUser.setRole(User.Role.ADMIN);

        Transaction mockTransaction = new Transaction(); // mock transaction object
        mockTransaction.setAmount(amount);

        TransactionService.TransferResult mockResult = new TransactionService.TransferResult(true, "✅ Transfer successful", mockTransaction);

        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(mockUser));
        when(transactionService.transferFunds(fromIban, toIban, amount, mockUser)).thenReturn(mockResult);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/transfer")
                        .param("fromIban", fromIban)
                        .param("toIban", toIban)
                        .param("amount", String.valueOf(amount)))
                .andExpect(status().isOk());
    }
    @Test
    void deposit_ShouldReturnOk_WhenDepositSuccessful() throws Exception {
        // Arrange
        String iban = "NL01BANK0000123456";
        double amount = 200.0;

        User mockUser = new User();
        mockUser.setUsername("admin");
        mockUser.setRole(User.Role.ADMIN);

        Transaction mockTransaction = new Transaction();
        mockTransaction.setAmount(amount);

        TransactionService.TransferResult mockResult = new TransactionService.TransferResult(true, "✅ Deposit successful", mockTransaction);

        when(userService.getUserByUsername("admin")).thenReturn(Optional.of(mockUser));
        when(transactionService.deposit(iban, amount, mockUser)).thenReturn(mockResult);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/deposit")
                        .param("iban", iban)
                        .param("amount", String.valueOf(amount)))
                .andExpect(status().isOk());
    }
    @Test
    void withdraw_ShouldReturnOkEvenWhenFails() throws Exception {
        // Arrange
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setRole(User.Role.USER);

        when(userService.getUserByUsername(any())).thenReturn(Optional.of(mockUser));

        TransactionService.TransferResult failedResult = new TransactionService.TransferResult(false, "Insufficient funds", null);
        when(transactionService.withdraw(anyString(), anyDouble(), any())).thenReturn(failedResult);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/withdraw")
                        .param("iban", "NL01BANK0000123456")
                        .param("amount", "9999.0"))
                .andExpect(status().isOk()); // ✅ Expect 200, because controller always returns OK
    }




    @Test
    void updateLimits_ShouldReturnOk_WhenSuccessful() throws Exception {
        // Arrange
        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setRole(User.Role.ADMIN);

        when(userService.getUserByUsername(any())).thenReturn(Optional.of(adminUser));

        TransactionService.TransferResult successResult = new TransactionService.TransferResult(true, "Limits updated successfully", null);
        when(transactionService.updateAccountLimits(anyString(), any(), any(), any())).thenReturn(successResult);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/update-limits")
                        .param("iban", "NL01BANK0000123456")
                        .param("absoluteLimit", "100.0")
                        .param("dailyLimit", "500.0"))
                .andExpect(status().isOk());
    }







}
