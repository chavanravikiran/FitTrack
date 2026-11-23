package com.fittrack.assessment.repository;

import com.fittrack.assessment.entity.FitnessAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FitnessAssessmentRepository extends JpaRepository<FitnessAssessment, Long> {

    @EntityGraph(attributePaths = {"equipment", "limitations"})
    Optional<FitnessAssessment> findByUserIdAndCurrentTrue(Long userId);

    @EntityGraph(attributePaths = {"equipment", "limitations"})
    Optional<FitnessAssessment> findByIdAndUserId(Long id, Long userId);

    Page<FitnessAssessment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByUserId(Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("update FitnessAssessment a set a.current = false where a.user.id = :userId and a.current = true")
    int clearCurrent(@Param("userId") Long userId);
}
