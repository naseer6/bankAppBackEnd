package nl.inholland.bankAppBackEnd.repository;

import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromAccountIdOrToAccountIdOrderByTimestampDesc(Long fromAccountId, Long toAccountId);
    
    // Find transactions where the account belongs to the specified user
    @Query("SELECT t FROM Transaction t WHERE t.toAccount.owner = :user OR t.fromAccount.owner = :user ORDER BY t.timestamp DESC")
    List<Transaction> findByAccountOwner(@Param("user") User user);
}
