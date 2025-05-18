package nl.inholland.bankAppBackEnd.repository;

import nl.inholland.bankAppBackEnd.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}

