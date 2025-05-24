package nl.inholland.bankAppBackEnd.repository;

import nl.inholland.bankAppBackEnd.models.BankAccount;
import nl.inholland.bankAppBackEnd.models.DailyTransferTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyTransferTrackingRepository extends JpaRepository<DailyTransferTracking, Long> {

    Optional<DailyTransferTracking> findByAccountAndTransferDate(BankAccount account, LocalDate transferDate);

    @Query("SELECT d FROM DailyTransferTracking d WHERE d.account.id = :accountId AND d.transferDate = :date")
    Optional<DailyTransferTracking> findByAccountIdAndDate(@Param("accountId") Long accountId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(d.totalTransferred), 0) FROM DailyTransferTracking d WHERE d.account.id = :accountId AND d.transferDate = :date")
    Double getTotalTransferredToday(@Param("accountId") Long accountId, @Param("date") LocalDate date);
}