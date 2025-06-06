package nl.inholland.bankAppBackEnd.services;

import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import nl.inholland.bankAppBackEnd.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ATMServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransferService transferService;

    @InjectMocks
    private ATMService atmService;

    private User user;
    private BankAccount account;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setApproved(true);

        account = new BankAccount();
        account.setIban("NL01BANK0000123456");
        account.setOwner(user);
        account.setBalance(1000.0);
        account.setAbsoluteLimit(100.0);
        account.setType(BankAccount.AccountType.CHECKING);
    }

    @Test
    void atmWithdraw_ShouldSucceed_WhenAllConditionsMet() {
        Transaction tx = new Transaction();
        tx.setAmount(100.0); // ✅ Fix: set amount to prevent NullPointerException

        when(bankAccountRepository.findByIban(account.getIban())).thenReturn(Optional.of(account));
        when(transferService.withdraw(account.getIban(), 100.0, user))
                .thenReturn(new TransferService.TransferResult(true, "✅ OK", tx));

        ATMService.ATMResult result = atmService.atmWithdraw(account.getIban(), 100.0, user);

        assertTrue(result.isSuccess());
        assertEquals(100.0, result.getTransaction().getAmount(), 0.01);
    }


    @Test
    void atmWithdraw_ShouldFail_WhenAmountIsInvalid() {
        ATMService.ATMResult result = atmService.atmWithdraw(account.getIban(), -50.0, user);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Invalid withdrawal amount"));
    }

    @Test
    void atmWithdraw_ShouldFail_WhenNotUserAccount() {
        User otherUser = new User();
        otherUser.setId(2L);
        account.setOwner(otherUser);

        when(bankAccountRepository.findByIban(account.getIban())).thenReturn(Optional.of(account));

        ATMService.ATMResult result = atmService.atmWithdraw(account.getIban(), 100.0, user);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Unauthorized"));
    }

    @Test
    void atmDeposit_ShouldSucceed_WhenValidRequest() {
        Transaction tx = new Transaction();

        when(bankAccountRepository.findByIban(account.getIban())).thenReturn(Optional.of(account));
        when(transferService.deposit(account.getIban(), 500.0, user))
                .thenReturn(new TransferService.TransferResult(true, "✅ Deposit OK", tx));

        ATMService.ATMResult result = atmService.atmDeposit(account.getIban(), 500.0, user);

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Deposit successful"));
    }

    @Test
    void atmDeposit_ShouldFail_WhenOverLimit() {
        ATMService.ATMResult result = atmService.atmDeposit(account.getIban(), 5000.0, user);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("deposit limit"));
    }

    @Test
    void atmTransfer_ShouldSucceed_WhenValidRequest() {
        String toIban = "NL01BANK9999999999";
        Transaction tx = new Transaction();

        when(bankAccountRepository.findByIban(account.getIban())).thenReturn(Optional.of(account));
        when(transferService.transferFunds(account.getIban(), toIban, 300.0, user))
                .thenReturn(new TransferService.TransferResult(true, "✅ Transfer OK", tx));

        ATMService.ATMResult result = atmService.atmTransfer(account.getIban(), toIban, 300.0, user);

        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Transfer OK"));
    }

    @Test
    void atmTransfer_ShouldFail_WhenFromAccountIsSavings() {
        account.setType(BankAccount.AccountType.SAVINGS);
        when(bankAccountRepository.findByIban(account.getIban())).thenReturn(Optional.of(account));

        ATMService.ATMResult result = atmService.atmTransfer(account.getIban(), "OTHER", 200.0, user);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("only allowed from checking"));
    }

    @Test
    void getAccountSummary_ShouldSucceed_WhenUserOwnsAccount() {
        when(bankAccountRepository.findByIban(account.getIban())).thenReturn(Optional.of(account));

        ATMService.ATMResult result = atmService.getAccountSummary(account.getIban(), user);

        assertTrue(result.isSuccess());
        assertEquals(1000.0, result.getNewBalance());
    }

    @Test
    void getAccountSummary_ShouldFail_WhenUserUnauthorized() {
        User other = new User();
        other.setId(2L);
        account.setOwner(other);

        when(bankAccountRepository.findByIban(account.getIban())).thenReturn(Optional.of(account));

        ATMService.ATMResult result = atmService.getAccountSummary(account.getIban(), user);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Unauthorized"));
    }

    @Test
    void getATMStatus_ShouldReturnOperationalMessage() {
        String result = atmService.getATMStatus();
        assertTrue(result.contains("operational"));
    }
}
