package nl.inholland.bankAppBackEnd.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.inholland.bankAppBackEnd.DTOs.TransactionDTO;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import nl.inholland.bankAppBackEnd.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserService userService;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private TransactionController transactionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User mockUser;
    private User mockAdminUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
        objectMapper = new ObjectMapper();

        // Setup mock users
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setEmail("test@example.com");
        mockUser.setRole(User.Role.USER);
        mockUser.setApproved(true);

        mockAdminUser = new User();
        mockAdminUser.setId(2L);
        mockAdminUser.setUsername("admin");
        mockAdminUser.setEmail("admin@example.com");
        mockAdminUser.setRole(User.Role.ADMIN);
        mockAdminUser.setApproved(true);
    }

    private void mockAuthentication(User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, Collections.emptyList());

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        lenient().when(userService.getUserByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    private void mockNoAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createTransaction_Success() throws Exception {
        // Given
        mockAuthentication(mockUser);
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setAmount(100.0);

        when(transactionService.save(any(Transaction.class))).thenReturn(transaction);

        // When & Then
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.amount").value(100.0));
    }

    @Test
    void getFilteredTransactions_Success() throws Exception {
        // Given
        mockAuthentication(mockUser);
        List<TransactionDTO> transactions = Arrays.asList(new TransactionDTO(), new TransactionDTO());
        Page<TransactionDTO> page = new PageImpl<>(transactions, PageRequest.of(0, 10), 2);

        when(transactionService.getFilteredTransactionsWithDirection(
                eq(mockUser), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/transactions")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getFilteredTransactions_Unauthenticated() throws Exception {
        // Given
        mockNoAuthentication();

        // When & Then
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Not authenticated"));
    }

    @Test
    void getTransactionById_Success() throws Exception {
        // Given
        mockAuthentication(mockUser);
        TransactionDTO transaction = new TransactionDTO();
        transaction.setId(1L);

        when(transactionService.getTransactionWithDirectionById(1L, mockUser))
                .thenReturn(transaction);

        // When & Then
        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getTransactionById_NotFound() throws Exception {
        // Given
        mockAuthentication(mockUser);
        when(transactionService.getTransactionWithDirectionById(1L, mockUser))
                .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyTransactions_Success() throws Exception {
        // Given
        mockAuthentication(mockUser);
        List<TransactionDTO> transactions = Arrays.asList(new TransactionDTO());
        Page<TransactionDTO> page = new PageImpl<>(transactions, PageRequest.of(0, 10), 1);

        when(transactionService.getTransactionsWithDirectionByUser(eq(mockUser), any(Pageable.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/transactions/my-transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions.length()").value(1));
    }

    @Test
    void getUserIbans_Success() throws Exception {
        // Given
        mockAuthentication(mockUser);
        List<String> ibans = Arrays.asList("NL01INHO0000000001", "NL01INHO0000000002");

        when(transactionService.getUserIbans(mockUser)).thenReturn(ibans);

        // When & Then
        mockMvc.perform(get("/api/transactions/user-ibans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ibans").isArray())
                .andExpect(jsonPath("$.ibans.length()").value(2));
    }

    @Test
    void transfer_Success() throws Exception {
        // Given
        mockAuthentication(mockUser);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fromIban", "NL01INHO0000000001");
        requestBody.put("toIban", "NL01INHO0000000002");
        requestBody.put("amount", 100.0);

        TransactionService.TransferResult result = mock(TransactionService.TransferResult.class);
        when(result.isSuccess()).thenReturn(true);
        when(result.getMessage()).thenReturn("Transfer successful");
        when(result.getTransaction()).thenReturn(new Transaction());

        when(transactionService.transferFunds(anyString(), anyString(), anyDouble(), eq(mockUser)))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transfer successful"));
    }

    @Test
    void transfer_UnapprovedUser() throws Exception {
        // Given
        User unapprovedUser = new User();
        unapprovedUser.setUsername("unapproved");
        unapprovedUser.setApproved(false);
        mockAuthentication(unapprovedUser);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fromIban", "NL01INHO0000000001");
        requestBody.put("toIban", "NL01INHO0000000002");
        requestBody.put("amount", 100.0);

        // When & Then
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("❌ Your account is not yet approved for transfers"));
    }

    @Test
    void transfer_MissingFields() throws Exception {
        // Given
        mockAuthentication(mockUser);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fromIban", "NL01INHO0000000001");
        // Missing toIban and amount

        // When & Then
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("❌ Missing required field: toIban"));
    }

    @Test
    void transfer_InvalidAmount() throws Exception {
        // Given
        mockAuthentication(mockUser);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fromIban", "NL01INHO0000000001");
        requestBody.put("toIban", "NL01INHO0000000002");
        requestBody.put("amount", "invalid");

        // When & Then
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("❌ Invalid amount format"));
    }

    @Test
    void transfer_ATM_Success() throws Exception {
        // Given
        mockAuthentication(mockUser);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fromIban", "NL01INHO0000000001");
        requestBody.put("toIban", "NL01INHO0000000002");
        requestBody.put("amount", 100.0);
        requestBody.put("isATM", true);

        TransactionService.ATMResult result = mock(TransactionService.ATMResult.class);
        when(result.isSuccess()).thenReturn(true);
        when(result.getMessage()).thenReturn("ATM transfer successful");
        when(result.getNewBalance()).thenReturn(900.0);
        when(result.getRemainingDailyLimit()).thenReturn(400.0);

        when(transactionService.atmTransfer(anyString(), anyString(), anyDouble(), eq(mockUser)))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ATM transfer successful"))
                .andExpect(jsonPath("$.newBalance").value(900.0))
                .andExpect(jsonPath("$.remainingDailyLimit").value(400.0));
    }

    @Test
    void adminTransfer_Success() throws Exception {
        // Given
        mockAuthentication(mockAdminUser);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fromIban", "NL01INHO0000000001");
        requestBody.put("toIban", "NL01INHO0000000002");
        requestBody.put("amount", 100.0);

        TransactionService.TransferResult result = mock(TransactionService.TransferResult.class);
        when(result.isSuccess()).thenReturn(true);
        when(result.getMessage()).thenReturn("Admin transfer successful");
        when(result.getTransaction()).thenReturn(new Transaction());

        when(transactionService.transferFunds(anyString(), anyString(), anyDouble(), eq(mockAdminUser)))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/transactions/admin/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Admin transfer successful"));
    }

    @Test
    void adminTransfer_NonAdmin() throws Exception {
        // Given
        mockAuthentication(mockUser); // Regular user, not admin
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fromIban", "NL01INHO0000000001");
        requestBody.put("toIban", "NL01INHO0000000002");
        requestBody.put("amount", 100.0);

        // When & Then
        mockMvc.perform(post("/api/transactions/admin/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("❌ Only administrators can perform this action"));
    }

    @Test
    void deposit_Success() throws Exception {
        // Given
        mockAuthentication(mockUser);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("iban", "NL01INHO0000000001");
        requestBody.put("amount", 100.0);

        TransactionService.TransferResult result = mock(TransactionService.TransferResult.class);
        when(result.isSuccess()).thenReturn(true);
        when(result.getMessage()).thenReturn("Deposit successful");
        when(result.getTransaction()).thenReturn(new Transaction());

        when(transactionService.deposit(anyString(), anyDouble(), eq(mockUser)))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Deposit successful"));
    }

    @Test
    void withdraw_Success() throws Exception {
        // Given
        mockAuthentication(mockUser);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("iban", "NL01INHO0000000001");
        requestBody.put("amount", 100.0);

        TransactionService.TransferResult result = mock(TransactionService.TransferResult.class);
        when(result.isSuccess()).thenReturn(true);
        when(result.getMessage()).thenReturn("Withdrawal successful");
        when(result.getTransaction()).thenReturn(new Transaction());

        when(transactionService.withdraw(anyString(), anyDouble(), eq(mockUser)))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Withdrawal successful"));
    }

    @Test
    void internalTransfer_Success() throws Exception {
        // Given
        mockAuthentication(mockUser);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fromIban", "NL01INHO0000000001");
        requestBody.put("toIban", "NL01INHO0000000002");
        requestBody.put("amount", 100.0);

        TransactionService.TransferResult result = mock(TransactionService.TransferResult.class);
        when(result.isSuccess()).thenReturn(true);
        when(result.getMessage()).thenReturn("Internal transfer successful");
        when(result.getTransaction()).thenReturn(new Transaction());

        when(transactionService.internalTransfer(anyString(), anyString(), anyDouble(), eq(mockUser)))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/transactions/internal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Internal transfer successful"));
    }

    @Test
    void getAccountSummary_Success() throws Exception {
        // Given
        mockAuthentication(mockUser);
        String iban = "NL01INHO0000000001";

        TransactionService.ATMResult result = mock(TransactionService.ATMResult.class);
        when(result.isSuccess()).thenReturn(true);
        when(result.getMessage()).thenReturn("Account summary retrieved");
        when(result.getNewBalance()).thenReturn(1000.0);
        when(result.getAvailableBalance()).thenReturn(900.0);
        when(result.getRemainingDailyLimit()).thenReturn(500.0);

        when(transactionService.getAccountSummary(iban, mockUser))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(get("/api/transactions/atm/account-summary/" + iban))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account summary retrieved"))
                .andExpect(jsonPath("$.newBalance").value(1000.0))
                .andExpect(jsonPath("$.availableBalance").value(900.0))
                .andExpect(jsonPath("$.remainingDailyLimit").value(500.0));
    }

    @Test
    void getRecentTransactions_Success() throws Exception {
        // Given
        mockAuthentication(mockUser);
        List<Transaction> transactions = Arrays.asList(new Transaction(), new Transaction());

        when(transactionService.getRecentATMTransactions(mockUser, 5))
                .thenReturn(transactions);

        // When & Then
        mockMvc.perform(get("/api/transactions/atm/recent-transactions")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Recent transactions retrieved"))
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void getATMStatus_Success() throws Exception {
        // Given
        when(transactionService.getATMStatus()).thenReturn("Operational");
        when(transactionService.isATMOperational()).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/transactions/atm/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operational").value(true))
                .andExpect(jsonPath("$.status").value("Operational"))
                .andExpect(jsonPath("$.services.withdrawal").value(true))
                .andExpect(jsonPath("$.services.deposit").value(true))
                .andExpect(jsonPath("$.services.transfer").value(true))
                .andExpect(jsonPath("$.services.balance_inquiry").value(true));
    }

    @Test

    void getATMAccountLimits_Success() throws Exception {
        // Given
        mockAuthentication(mockUser);
        String iban = "NL01INHO0000000001";

        TransactionService.ATMResult result = mock(TransactionService.ATMResult.class);
        when(result.isSuccess()).thenReturn(true);
        when(result.getMessage()).thenReturn("Account limits retrieved");
        when(result.getNewBalance()).thenReturn(1000.0);
        when(result.getAvailableBalance()).thenReturn(900.0);
        when(result.getRemainingDailyLimit()).thenReturn(500.0);

        // Make this stub lenient to avoid UnnecessaryStubbingException
        lenient().when(transactionService.getAccountSummary(iban, mockUser))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(get("/api/transactions/atm/limits/" + iban))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account limits retrieved"))
                .andExpect(jsonPath("$.iban").value(iban))
                .andExpect(jsonPath("$.balance").value(1000.0))
                .andExpect(jsonPath("$.availableBalance").value(900.0))
                .andExpect(jsonPath("$.remainingDailyLimit").value(500.0))
                .andExpect(jsonPath("$.atmLimits.maxWithdrawalPerTransaction").value(500.0))
                .andExpect(jsonPath("$.atmLimits.maxDepositPerTransaction").value(2000.0))
                .andExpect(jsonPath("$.atmLimits.maxTransferPerTransaction").value(1000.0));
    }

    @Test
    void getAllTransactions_AdminOnly_Success() throws Exception {
        // Given
        mockAuthentication(mockAdminUser);
        List<Transaction> transactions = Arrays.asList(new Transaction(), new Transaction());
        Page<Transaction> page = new PageImpl<>(transactions, PageRequest.of(0, 10), 2);

        when(transactionService.getFilteredTransactions(
                any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        when(transactionService.convertToAdminDTO(any(Transaction.class)))
                .thenReturn(new TransactionDTO());

        // When & Then
        mockMvc.perform(get("/api/transactions/all")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }
}