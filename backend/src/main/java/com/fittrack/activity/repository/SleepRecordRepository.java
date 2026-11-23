package com.fittrack.activity.repository;

import com.fittrack.activity.entity.SleepRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SleepRecordRepository extends JpaRepository<SleepRecord, Long> {

    Optional<SleepRecord> findByUserIdAndSleepDate(Long userId, LocalDate date);

    List<SleepRecord> findByUserIdAndSleepDateBetweenOrderBySleepDateAsc(Long userId, LocalDate from, LocalDate to);
}
