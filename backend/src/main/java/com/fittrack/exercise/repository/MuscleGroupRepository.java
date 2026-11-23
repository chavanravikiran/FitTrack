package com.fittrack.exercise.repository;

import com.fittrack.exercise.entity.MuscleGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MuscleGroupRepository extends JpaRepository<MuscleGroup, Long> {

    List<MuscleGroup> findAllByOrderBySortOrderAsc();

    Optional<MuscleGroup> findByCode(String code);
}
