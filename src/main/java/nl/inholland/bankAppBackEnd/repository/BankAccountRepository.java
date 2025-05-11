package nl.inholland.bankAppBackEnd.repository;

import nl.inholland.bankAppBackEnd.models.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
}
