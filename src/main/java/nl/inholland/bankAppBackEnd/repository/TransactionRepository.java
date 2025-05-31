package nl.inholland.bankAppBackEnd.repository;

import nl.inholland.bankAppBackEnd.models.Transaction;
import nl.inholland.bankAppBackEnd.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromAccountIdOrToAccountIdOrderByTimestampDesc(Long fromAccountId, Long toAccountId);
    
    // Find transactions where the account belongs to the specified user with pagination
    @Query("SELECT t FROM Transaction t WHERE t.toAccount.owner = :user OR t.fromAccount.owner = :user ORDER BY t.timestamp DESC")
    Page<Transaction> findByAccountOwner(@Param("user") User user, Pageable pageable);
    
    // Keep the non-paginated version for backward compatibility
    @Query("SELECT t FROM Transaction t WHERE t.toAccount.owner = :user OR t.fromAccount.owner = :user ORDER BY t.timestamp DESC")
    List<Transaction> findByAccountOwner(@Param("user") User user);
    
    // A comprehensive filtering query for user transactions
    @Query("SELECT t FROM Transaction t WHERE " +
           "(t.toAccount.owner = :user OR t.fromAccount.owner = :user) AND " +
           "(:iban IS NULL OR :ibanType IS NULL OR " +
           " (:ibanType = 'from' AND t.fromAccount.iban = :iban) OR " +
           " (:ibanType = 'to' AND t.toAccount.iban = :iban) OR " +
           " (:ibanType = 'both' AND (t.fromAccount.iban = :iban OR t.toAccount.iban = :iban))) AND " +
           "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR t.amount <= :maxAmount) AND " +
           "(:exactAmount IS NULL OR t.amount = :exactAmount) AND " +
           "(CAST(:startDate AS timestamp) IS NULL OR t.timestamp >= :startDate) AND " +
           "(CAST(:endDate AS timestamp) IS NULL OR t.timestamp <= :endDate) " +
           "ORDER BY t.timestamp DESC")
    Page<Transaction> findFilteredByUser(
            @Param("user") User user,
            @Param("iban") String iban,
            @Param("ibanType") String ibanType,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            @Param("exactAmount") Double exactAmount,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    
    // A comprehensive filtering query for admin (all transactions)
    @Query("SELECT t FROM Transaction t WHERE " +
           "(:iban IS NULL OR :ibanType IS NULL OR " +
           " (:ibanType = 'from' AND t.fromAccount.iban = :iban) OR " +
           " (:ibanType = 'to' AND t.toAccount.iban = :iban) OR " +
           " (:ibanType = 'both' AND (t.fromAccount.iban = :iban OR t.toAccount.iban = :iban))) AND " +
           "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR t.amount <= :maxAmount) AND " +
           "(:exactAmount IS NULL OR t.amount = :exactAmount) AND " +
           "(CAST(:startDate AS timestamp) IS NULL OR t.timestamp >= :startDate) AND " +
           "(CAST(:endDate AS timestamp) IS NULL OR t.timestamp <= :endDate) AND " +
           "(:initiatedBy IS NULL OR t.initiatedByUser.username = :initiatedBy) " +
           "ORDER BY t.timestamp DESC")
    Page<Transaction> findFiltered(
            @Param("iban") String iban,
            @Param("ibanType") String ibanType,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            @Param("exactAmount") Double exactAmount,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("initiatedBy") String initiatedBy,
            Pageable pageable);
    
    // Non-paginated version
    @Query("SELECT count(t) FROM Transaction t WHERE " +
           "t.timestamp >= :startOfDay AND t.timestamp <= :endOfDay")
    long countTransactionsByDate(
            @Param("startOfDay") LocalDateTime startOfDay, 
            @Param("endOfDay") LocalDateTime endOfDay);
    
    // Override default findAll to return transactions in descending timestamp order
    @Query("SELECT t FROM Transaction t ORDER BY t.timestamp DESC")
    Page<Transaction> findAll(Pageable pageable);
    
    @Query("SELECT t FROM Transaction t ORDER BY t.timestamp DESC")
    List<Transaction> findAll();
}
