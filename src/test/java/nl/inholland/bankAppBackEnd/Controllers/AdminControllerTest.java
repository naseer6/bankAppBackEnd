package nl.inholland.bankAppBackEnd.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.inholland.bankAppBackEnd.config.JwtAuthenticationFilter;
import nl.inholland.bankAppBackEnd.config.JwtUtil;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AdminControllerTest {

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private BankAccountService bankAccountService;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void approveUser_ShouldApproveUnapprovedUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(User.Role.USER);
        user.setApproved(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setApproved(true);
            return savedUser;
        }).when(userRepository).save(any(User.class));

        mockMvc.perform(post("/api/admin/approve/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("User approved and bank account created."));

        verify(userRepository).save(user);
        verify(bankAccountService).createAccountsForUser(user);
    }

    @Test
    void approveUser_ShouldReturnForbidden_IfAlreadyApproved() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(User.Role.USER);
        user.setApproved(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/admin/approve/1"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("User is already approved or not eligible."));
    }

    @Test
    void approveUser_ShouldReturnNotFound_IfUserMissing() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/approve/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found."));
    }

    @Test
    void getUnapprovedUsers_ShouldReturnList() throws Exception {
        User user1 = new User(); user1.setId(1L); user1.setApproved(false); user1.setRole(User.Role.USER);
        User user2 = new User(); user2.setId(2L); user2.setApproved(false); user2.setRole(User.Role.USER);
        User admin = new User(); admin.setId(3L); admin.setApproved(false); admin.setRole(User.Role.ADMIN);

        when(userRepository.findAll()).thenReturn(List.of(user1, user2, admin));

        mockMvc.perform(get("/api/admin/unapproved-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void getAllAccounts_ShouldReturnList() throws Exception {
        when(bankAccountService.getAllAccounts()).thenReturn(List.of(new BankAccount(), new BankAccount()));

        mockMvc.perform(get("/api/admin/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAccountDetails_ShouldReturnAccount_IfExists() throws Exception {
        BankAccount account = new BankAccount();
        account.setId(1L);
        when(bankAccountService.getAccountById(1L)).thenReturn(Optional.of(account));

        mockMvc.perform(get("/api/admin/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(bankAccountService).getAccountById(1L);
    }

    @Test
    void getAccountDetails_ShouldReturn404_WhenNotFound() throws Exception {
        when(bankAccountService.getAccountById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/accounts/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found."));
    }

    @Test
    void getAccountTransactions_ShouldReturnList_IfAccountExists() throws Exception {
        BankAccount account = new BankAccount();
        account.setId(1L);
        when(bankAccountService.getAccountById(1L)).thenReturn(Optional.of(account));

        Transaction tx1 = new Transaction(); tx1.setId(1L);
        Transaction tx2 = new Transaction(); tx2.setId(2L);
        when(transactionService.getTransactionsByAccountId(1L)).thenReturn(List.of(tx1, tx2));

        mockMvc.perform(get("/api/admin/accounts/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(transactionService).getTransactionsByAccountId(1L);
    }

    @Test
    void closeAccount_ShouldReturnSuccess_WhenClosed() throws Exception {
        when(bankAccountService.closeAccount(1L)).thenReturn(true);

        mockMvc.perform(post("/api/admin/accounts/1/close"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account closed successfully."));

        verify(bankAccountService).closeAccount(1L);
    }

    @Test
    void closeAccount_ShouldReturnBadRequest_WhenFailed() throws Exception {
        when(bankAccountService.closeAccount(1L)).thenReturn(false);

        mockMvc.perform(post("/api/admin/accounts/1/close"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to close account. Make sure the account exists and has zero balance."));

        verify(bankAccountService).closeAccount(1L);
    }

    @Test
    void getDashboardStats_ShouldReturnCorrectStats() throws Exception {
        User u1 = new User(); u1.setRole(User.Role.USER); u1.setApproved(false);
        User u2 = new User(); u2.setRole(User.Role.USER); u2.setApproved(true);
        User u3 = new User(); u3.setRole(User.Role.ADMIN); u3.setApproved(false);

        when(userRepository.findAll()).thenReturn(List.of(u1, u2, u3));
        when(bankAccountService.getActiveAccountsCount()).thenReturn(5);
        when(transactionService.getTodayTransactionsCount()).thenReturn(3);

        mockMvc.perform(get("/api/admin/dashboard-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingApprovals").value(1))
                .andExpect(jsonPath("$.totalUsers").value(3))
                .andExpect(jsonPath("$.activeAccounts").value(5))
                .andExpect(jsonPath("$.todayTransactions").value(3));
    }
}
