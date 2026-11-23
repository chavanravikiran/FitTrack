package com.fittrack.exercise.repository;

import com.fittrack.exercise.entity.Exercise;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    /** The whole active catalog with the associations the plan generator needs (catalog is small). */
    @EntityGraph(attributePaths = {"equipment", "contraindications", "secondaryMuscles"})
    @Query("select e from Exercise e where e.active = true order by e.name")
    List<Exercise> findActiveCatalog();

    @EntityGraph(attributePaths = {"equipment", "contraindications", "secondaryMuscles", "alternatives"})
    Optional<Exercise> findWithDetailsById(Long id);

    Optional<Exercise> findBySlug(String slug);
}
