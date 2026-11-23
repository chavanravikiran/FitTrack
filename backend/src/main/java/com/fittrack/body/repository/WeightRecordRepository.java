package com.fittrack.body.repository;

import com.fittrack.body.entity.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {

    Optional<WeightRecord> findFirstByUserIdOrderByRecordDateDesc(Long userId);

    Optional<WeightRecord> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);
}
