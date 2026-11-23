package com.fittrack.workout.repository;

import com.fittrack.workout.entity.PersonalRecord;
import com.fittrack.workout.entity.PersonalRecord.RecordType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalRecordRepository extends JpaRepository<PersonalRecord, Long> {

    Optional<PersonalRecord> findByUserIdAndExerciseIdAndRecordType(Long userId, Long exerciseId, RecordType type);

    List<PersonalRecord> findByUserIdOrderByAchievedOnDesc(Long userId);
}
