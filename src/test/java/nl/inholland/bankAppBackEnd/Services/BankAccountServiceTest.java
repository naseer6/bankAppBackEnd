package nl.inholland.bankAppBackEnd.services;

import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.User;
import nl.inholland.bankAppBackEnd.repository.BankAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private BankAccountService bankAccountService;

    @Test
    void testGetAccountById_ShouldReturnAccount() {
        BankAccount mockAccount = new BankAccount();
        mockAccount.setId(1L);

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(mockAccount));

        Optional<BankAccount> result = bankAccountService.getAccountById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void testCreateAccountForUser_ShouldReturnSavedAccount() {
        User user = new User();
        BankAccount savedAccount = new BankAccount();
        savedAccount.setOwner(user);
        savedAccount.setId(1L);
        savedAccount.setIban("NL00TEST1234567890");

        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(savedAccount);

        BankAccount result = bankAccountService.createAccountForUser(user);

        assertNotNull(result);
        assertEquals(user, result.getOwner());
        assertEquals("NL00TEST1234567890", result.getIban());
    }

    @Test
    void testGetAllAccounts_ShouldReturnAccountList() {
        List<BankAccount> accounts = List.of(new BankAccount(), new BankAccount());
        when(bankAccountRepository.findAll()).thenReturn(accounts);

        List<BankAccount> result = bankAccountService.getAllAccounts();

        assertEquals(2, result.size());
    }

//    @Test
//    void testCloseAccount_ShouldDeactivateIfBalanceZero() {
//        BankAccount account = new BankAccount();
//        account.setId(1L);
//        account.setBalance(0.0);
//        account.setActive(true);
//
//        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
//
//        boolean result = bankAccountService.closeAccount(1L);
//
//        assertTrue(result);
//        assertFalse(account.isActive());
//        verify(bankAccountRepository).save(account);
//    }

//    @Test
//    void testCloseAccount_ShouldNotDeactivateIfBalanceNotZero() {
//        BankAccount account = new BankAccount();
//        account.setId(1L);
//        account.setBalance(100.0);
//        account.setActive(true);
//
//        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
//
//        boolean result = bankAccountService.closeAccount(1L);
//
//        assertFalse(result);
//        assertTrue(account.isActive()); // Still active
//        verify(bankAccountRepository, never()).save(account);
//    }
}
