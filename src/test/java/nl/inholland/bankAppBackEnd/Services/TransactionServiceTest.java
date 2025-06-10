package nl.inholland.bankAppBackEnd.Services;

import nl.inholland.bankAppBackEnd.DTOs.TransactionDTO;
import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.repository.TransactionRepository;
import nl.inholland.bankAppBackEnd.services.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User mockUser;
    private BankAccount mockAccount;
    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock user
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setRole(User.Role.USER);

        // Setup mock bank account
        mockAccount = new BankAccount();
        mockAccount.setId(1L);
        mockAccount.setIban("NL01INHO0000000001");
        mockAccount.setOwner(mockUser);
        mockAccount.setBalance(1000.0);

        // Setup mock transaction
        mockTransaction = new Transaction();
        mockTransaction.setId(1L);
        mockTransaction.setFromAccount(mockAccount);
        mockTransaction.setToAccount(mockAccount);
        mockTransaction.setAmount(100.0);
        mockTransaction.setTransactionType("TRANSFER");
        mockTransaction.setTimestamp(LocalDateTime.now());
    }

    @Test
    void saveTransaction_Success() {
        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTransaction);

        Transaction savedTransaction = transactionService.save(mockTransaction);

        assertNotNull(savedTransaction);
        assertEquals(mockTransaction.getId(), savedTransaction.getId());
        verify(transactionRepository, times(1)).save(mockTransaction);
    }

    @Test
    void findAllTransactions_Success() {
        List<Transaction> transactions = Arrays.asList(mockTransaction, mockTransaction);
        when(transactionRepository.findAll()).thenReturn(transactions);

        List<Transaction> result = transactionService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(transactionRepository, times(1)).findAll();
    }

    @Test
    void findTransactionById_Success() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(mockTransaction));

        Optional<Transaction> result = transactionService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(mockTransaction.getId(), result.get().getId());
        verify(transactionRepository, times(1)).findById(1L);
    }

    @Test
    void getFilteredTransactionsWithDirection_Success() {
        List<Transaction> transactions = Arrays.asList(mockTransaction, mockTransaction);
        Page<Transaction> page = new PageImpl<>(transactions, PageRequest.of(0, 10), 2);
        when(transactionRepository.findFilteredByUser(any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(bankAccountRepository.findAllByOwner(mockUser)).thenReturn(List.of(mockAccount));

        Page<TransactionDTO> result = transactionService.getFilteredTransactionsWithDirection(
                mockUser, null, null, null, null, null, null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(transactionRepository, times(1)).findFilteredByUser(any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void transferFunds_Success() {
        BankAccount fromAccount = new BankAccount();
        fromAccount.setIban("NL01INHO0000000001");
        fromAccount.setBalance(1000.0);

        BankAccount toAccount = new BankAccount();
        toAccount.setIban("NL01INHO0000000002");
        toAccount.setBalance(500.0);

        when(bankAccountRepository.findByIban("NL01INHO0000000001")).thenReturn(Optional.of(fromAccount));
        when(bankAccountRepository.findByIban("NL01INHO0000000002")).thenReturn(Optional.of(toAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTransaction);

        TransactionService.TransferResult result = transactionService.transferFunds(
                "NL01INHO0000000001", "NL01INHO0000000002", 100.0, mockUser);

        assertTrue(result.isSuccess());
        assertEquals("✅ Successfully transferred €100.00 from NL01INHO0000000001 to NL01INHO0000000002", result.getMessage());
        assertEquals(900.0, fromAccount.getBalance());
        assertEquals(600.0, toAccount.getBalance());
        verify(bankAccountRepository, times(2)).save(any(BankAccount.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void deposit_Success() {
        when(bankAccountRepository.findByIban("NL01INHO0000000001")).thenReturn(Optional.of(mockAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTransaction);

        TransactionService.TransferResult result = transactionService.deposit("NL01INHO0000000001", 100.0, mockUser);

        assertTrue(result.isSuccess());
        assertEquals("✅ Successfully deposited €100.00 to NL01INHO0000000001. New balance: €1100.00", result.getMessage());
        verify(bankAccountRepository, times(1)).save(mockAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void withdraw_Success() {
        when(bankAccountRepository.findByIban("NL01INHO0000000001")).thenReturn(Optional.of(mockAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTransaction);

        TransactionService.TransferResult result = transactionService.withdraw("NL01INHO0000000001", 100.0, mockUser);

        assertTrue(result.isSuccess());
        assertEquals("✅ Successfully withdrew €100.00 from NL01INHO0000000001, please collect your cash! New balance: €900.00", result.getMessage());
        verify(bankAccountRepository, times(1)).save(mockAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void getUserIbans_Success() {
        when(bankAccountRepository.findAllByOwner(mockUser)).thenReturn(List.of(mockAccount));

        List<String> result = transactionService.getUserIbans(mockUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("NL01INHO0000000001", result.get(0));
        verify(bankAccountRepository, times(1)).findAllByOwner(mockUser);
    }
}
