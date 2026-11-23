package com.fittrack.activity.repository;

import com.fittrack.activity.entity.WaterIntakeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WaterIntakeEntryRepository extends JpaRepository<WaterIntakeEntry, Long> {

    List<WaterIntakeEntry> findByUserIdAndRecordDateOrderByLoggedAtAsc(Long userId, LocalDate date);

    Optional<WaterIntakeEntry> findByIdAndUserId(Long id, Long userId);

    @Query("select coalesce(sum(w.amountMl), 0) from WaterIntakeEntry w where w.user.id = :userId and w.recordDate = :date")
    int totalFor(@Param("userId") Long userId, @Param("date") LocalDate date);

    /** [date, totalMl] rows for a date range. */
    @Query("""
            select w.recordDate, sum(w.amountMl) from WaterIntakeEntry w
            where w.user.id = :userId and w.recordDate between :from and :to
            group by w.recordDate order by w.recordDate
            """)
    List<Object[]> dailyTotals(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
