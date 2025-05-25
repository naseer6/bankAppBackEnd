package nl.inholland.bankAppBackEnd.repository;

import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByOwnerAndType(User owner, BankAccount.AccountType type);
    List<BankAccount> findAllByOwner(User owner);
    List<BankAccount> findByOwnerNameContainingIgnoreCase(String name);
    Optional<BankAccount> findByIban(String iban);
    List<BankAccount> findByIbanContainingIgnoreCase(String iban);
}