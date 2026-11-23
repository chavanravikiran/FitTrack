package com.fittrack.video.repository;

import com.fittrack.video.entity.ExerciseVideo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExerciseVideoRepository extends JpaRepository<ExerciseVideo, Long> {

    @EntityGraph(attributePaths = {"exercise", "exercise.equipment", "equipment"})
    @Query("select v from ExerciseVideo v where v.active = true order by v.category, v.title")
    List<ExerciseVideo> findActiveLibrary();

    @EntityGraph(attributePaths = {"exercise", "exercise.equipment", "equipment"})
    @Query("select v from ExerciseVideo v order by v.category, v.title")
    List<ExerciseVideo> findAllForAdmin();

    @EntityGraph(attributePaths = {"exercise", "exercise.equipment", "equipment"})
    Optional<ExerciseVideo> findWithDetailsById(Long id);

    Optional<ExerciseVideo> findFirstByExerciseIdAndActiveTrue(Long exerciseId);
}
