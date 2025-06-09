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

    // ✅ Null-safe paginated user transactions (via LEFT JOIN to avoid implicit INNER JOINs)
    @Query("SELECT t FROM Transaction t " +
            "LEFT JOIN t.toAccount ta " +
            "LEFT JOIN t.fromAccount fa " +
            "WHERE " +
            "(fa IS NOT NULL AND fa.owner = :user) OR " +
            "(ta IS NOT NULL AND ta.owner = :user) " +
            "ORDER BY t.timestamp DESC")
    Page<Transaction> findByAccountOwner(@Param("user") User user, Pageable pageable);

    @Query("SELECT t FROM Transaction t " +
            "LEFT JOIN t.toAccount ta " +
            "LEFT JOIN t.fromAccount fa " +
            "WHERE " +
            "(fa IS NOT NULL AND fa.owner = :user) OR " +
            "(ta IS NOT NULL AND ta.owner = :user) " +
            "ORDER BY t.timestamp DESC")
    List<Transaction> findByAccountOwner(@Param("user") User user);


    // ✅ Filtered with full null-safety for user context
    @Query("SELECT t FROM Transaction t " +
            "LEFT JOIN t.fromAccount fa " +
            "LEFT JOIN t.toAccount ta " +
            "WHERE ((ta.owner = :user OR fa.owner = :user)) AND " +
            "(:iban IS NULL OR :ibanType IS NULL OR " +
            " (:ibanType = 'from' AND fa.iban = :iban) OR " +
            " (:ibanType = 'to' AND ta.iban = :iban) OR " +
            " (:ibanType = 'both' AND (fa.iban = :iban OR ta.iban = :iban))) AND " +
            "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR t.amount <= :maxAmount) AND " +
            "(:exactAmount IS NULL OR t.amount = :exactAmount) AND " +
            "(:startDate IS NULL OR t.timestamp >= :startDate) AND " +
            "(:endDate IS NULL OR t.timestamp <= :endDate) " +
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

    // ✅ Admin version — includes all, null-safe
    @Query("SELECT t FROM Transaction t " +
            "LEFT JOIN t.fromAccount fa " +
            "LEFT JOIN t.toAccount ta " +
            "LEFT JOIN t.initiatedByUser iu " +
            "WHERE " +
            "(:iban IS NULL OR :ibanType IS NULL OR " +
            " (:ibanType = 'from' AND fa.iban = :iban) OR " +
            " (:ibanType = 'to' AND ta.iban = :iban) OR " +
            " (:ibanType = 'both' AND (fa.iban = :iban OR ta.iban = :iban))) AND " +
            "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR t.amount <= :maxAmount) AND " +
            "(:exactAmount IS NULL OR t.amount = :exactAmount) AND " +
            "(:startDate IS NULL OR t.timestamp >= :startDate) AND " +
            "(:endDate IS NULL OR t.timestamp <= :endDate) AND " +
            "(:initiatedBy IS NULL OR iu.username = :initiatedBy) " +
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

    // ✅ Safe and simple
    @Query("SELECT count(t) FROM Transaction t WHERE t.timestamp >= :startOfDay AND t.timestamp <= :endOfDay")
    long countTransactionsByDate(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    // ✅ Sorted all transactions - paged
    @Query("SELECT t FROM Transaction t ORDER BY t.timestamp DESC")
    Page<Transaction> findAll(Pageable pageable);

    // ✅ Sorted all transactions - non-paged
    @Query("SELECT t FROM Transaction t ORDER BY t.timestamp DESC")
    List<Transaction> findAll();
}
