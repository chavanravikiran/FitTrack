package com.fittrack.activity.repository;

import com.fittrack.activity.entity.DailyActivityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyActivityRecordRepository extends JpaRepository<DailyActivityRecord, Long> {

    Optional<DailyActivityRecord> findByUserIdAndRecordDate(Long userId, LocalDate date);

    List<DailyActivityRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(Long userId, LocalDate from, LocalDate to);

    @Query("select coalesce(avg(r.steps), 0) from DailyActivityRecord r where r.user.id = :userId and r.recordDate between :from and :to")
    double averageSteps(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
