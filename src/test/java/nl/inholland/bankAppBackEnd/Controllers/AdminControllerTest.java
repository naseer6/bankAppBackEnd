package nl.inholland.bankAppBackEnd.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.inholland.bankAppBackEnd.DTOs.BankAccountDTO;
import nl.inholland.bankAppBackEnd.DTOs.DashboardStatsDTO;
import nl.inholland.bankAppBackEnd.DTOs.TransactionDTO;
import nl.inholland.bankAppBackEnd.config.JwtAuthenticationFilter;
import nl.inholland.bankAppBackEnd.config.JwtUtil;
import nl.inholland.bankAppBackEnd.exceptions.ResourceNotFoundException;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.UserRepository;
import nl.inholland.bankAppBackEnd.services.BankAccountService;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AdminControllerTest {

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private BankAccountService bankAccountService;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .alwaysDo(print()) // Print request and response for debugging
                .build();
    }

    @Test
    void approveUser_ShouldApproveUnapprovedUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setRole(User.Role.USER);
        user.setApproved(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setApproved(true);
            return savedUser;
        });

        // Mock the account creation
        when(bankAccountService.createAccountsForUser(any(User.class))).thenReturn(new ArrayList<>());

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

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
        verify(bankAccountService, never()).createAccountsForUser(any(User.class));
    }

    @Test
    void approveUser_ShouldReturnNotFound_IfUserMissing() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/approve/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found."));

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
        verify(bankAccountService, never()).createAccountsForUser(any(User.class));
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
        // Create sample DTOs
        BankAccountDTO dto1 = new BankAccountDTO();
        BankAccountDTO dto2 = new BankAccountDTO();

        when(bankAccountService.getAllAccountDTOs()).thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/admin/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(bankAccountService).getAllAccountDTOs();
    }

    @Test
    void getAccountDetails_ShouldReturnAccount_IfExists() throws Exception {
        BankAccountDTO accountDTO = new BankAccountDTO();
        accountDTO.setId(1L);

        when(bankAccountService.getAccountDTOById(1L)).thenReturn(Optional.of(accountDTO));

        mockMvc.perform(get("/api/admin/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(bankAccountService).getAccountDTOById(1L);
    }

    @Test
    void getAccountDetails_ShouldReturn404_WhenNotFound() throws Exception {
        // When no account is found, the controller should throw ResourceNotFoundException
        when(bankAccountService.getAccountDTOById(1L)).thenReturn(Optional.empty());

        // Mock the behavior for handling the ResourceNotFoundException (thrown inside controller)
        mockMvc.perform(get("/api/admin/accounts/1"))
                .andExpect(status().isNotFound());

        verify(bankAccountService).getAccountDTOById(1L);
    }

    @Test
    void getAccountTransactions_ShouldReturnList_IfAccountExists() throws Exception {
        // Mock that the account exists
        when(bankAccountService.accountExists(1L)).thenReturn(true);

        // Create sample TransactionDTOs
        TransactionDTO tx1 = new TransactionDTO();
        tx1.setId(1L);

        TransactionDTO tx2 = new TransactionDTO();
        tx2.setId(2L);

        when(transactionService.getTransactionDTOsByAccountId(1L)).thenReturn(List.of(tx1, tx2));

        mockMvc.perform(get("/api/admin/accounts/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(transactionService).getTransactionDTOsByAccountId(1L);
    }

    @Test
    void getAccountTransactions_ShouldReturn404_WhenAccountNotFound() throws Exception {
        // Mock that the account doesn't exist
        when(bankAccountService.accountExists(1L)).thenReturn(false);

        // The controller should throw ResourceNotFoundException when account doesn't exist
        mockMvc.perform(get("/api/admin/accounts/1/transactions"))
                .andExpect(status().isNotFound());

        verify(bankAccountService).accountExists(1L);
        verify(transactionService, never()).getTransactionDTOsByAccountId(anyLong());
    }

    @Test
    void closeAccount_ShouldReturnSuccess_WhenClosed() throws Exception {
        // Use doNothing() for void methods
        doNothing().when(bankAccountService).closeAccount(1L);

        mockMvc.perform(post("/api/admin/accounts/1/close"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account closed successfully."));

        verify(bankAccountService).closeAccount(1L);
    }

    @Test
    void closeAccount_ShouldReturnNotFound_WhenAccountDoesNotExist() throws Exception {
        // Explicitly throw ResourceNotFoundException when closeAccount is called
        doThrow(new ResourceNotFoundException("Account not found"))
            .when(bankAccountService).closeAccount(1L);

        mockMvc.perform(post("/api/admin/accounts/1/close"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found"));

        verify(bankAccountService).closeAccount(1L);
    }

    @Test
    void closeAccount_ShouldReturnBadRequest_WhenAccountHasNonZeroBalance() throws Exception {
        // Explicitly throw IllegalStateException when closeAccount is called
        doThrow(new IllegalStateException("Account has non-zero balance"))
            .when(bankAccountService).closeAccount(1L);

        mockMvc.perform(post("/api/admin/accounts/1/close"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Account has non-zero balance"));

        verify(bankAccountService).closeAccount(1L);
    }

    @Test
    public void testGetDashboardStats() throws Exception {
        // Arrange
        DashboardStatsDTO mockStats = new DashboardStatsDTO(5, 50, 75, 30);
        when(bankAccountService.getDashboardStats()).thenReturn(mockStats);

        // Act & Assert
        mockMvc.perform(get("/api/admin/dashboard-stats")
                .accept(MediaType.APPLICATION_JSON))  // Added accept header
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingApprovals", is(5)))
                .andExpect(jsonPath("$.totalUsers", is(50)))
                .andExpect(jsonPath("$.activeAccounts", is(75)))
                .andExpect(jsonPath("$.todayTransactions", is(30)));

        // Verify
        verify(bankAccountService, times(1)).getDashboardStats();
    }
}