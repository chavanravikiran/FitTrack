package com.fittrack.exercise.service;

import com.fittrack.assessment.entity.Equipment;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.exercise.dto.ExerciseDtos.ExerciseDetail;
import com.fittrack.exercise.dto.ExerciseDtos.ExerciseSummary;
import com.fittrack.exercise.dto.ExerciseDtos.MuscleGroupDto;
import com.fittrack.exercise.entity.Exercise;
import com.fittrack.exercise.entity.Exercise.Difficulty;
import com.fittrack.exercise.entity.MuscleGroup;
import com.fittrack.exercise.repository.ExerciseRepository;
import com.fittrack.exercise.repository.MuscleGroupRepository;
import com.fittrack.video.repository.ExerciseVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final MuscleGroupRepository muscleGroupRepository;
    private final ExerciseVideoRepository videoRepository;

    @Transactional(readOnly = true)
    public List<MuscleGroupDto> muscleGroups() {
        return muscleGroupRepository.findAllByOrderBySortOrderAsc().stream().map(ExerciseService::toDto).toList();
    }

    /** Filters the (small, cached-by-Hibernate) active catalog in memory. */
    @Transactional(readOnly = true)
    public List<ExerciseSummary> search(String muscle, Difficulty difficulty, String equipment, String query) {
        String q = StringUtils.hasText(query) ? query.trim().toLowerCase(Locale.ROOT) : null;
        return exerciseRepository.findActiveCatalog().stream()
                .filter(e -> !StringUtils.hasText(muscle) || e.getPrimaryMuscleGroup().getCode().equalsIgnoreCase(muscle))
                .filter(e -> difficulty == null || e.getDifficulty() == difficulty)
                .filter(e -> !StringUtils.hasText(equipment)
                        || e.getEquipment().stream().anyMatch(eq -> eq.getCode().equalsIgnoreCase(equipment)))
                .filter(e -> q == null || e.getName().toLowerCase(Locale.ROOT).contains(q))
                .map(ExerciseService::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExerciseDetail detail(Long id) {
        Exercise e = exerciseRepository.findWithDetailsById(id)
                .filter(Exercise::isActive)
                .orElseThrow(() -> ResourceNotFoundException.of("Exercise", id));
        return toDetail(e, videoRepository.findFirstByExerciseIdAndActiveTrue(id).map(v -> v.getId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<ExerciseSummary> alternatives(Long id) {
        Exercise e = exerciseRepository.findWithDetailsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Exercise", id));
        return e.getAlternatives().stream().filter(Exercise::isActive)
                .sorted(Comparator.comparing(Exercise::getName)).map(ExerciseService::toSummary).toList();
    }

    public static MuscleGroupDto toDto(MuscleGroup m) {
        return new MuscleGroupDto(m.getCode(), m.getName());
    }

    public static ExerciseSummary toSummary(Exercise e) {
        return new ExerciseSummary(e.getId(), e.getName(), e.getSlug(), toDto(e.getPrimaryMuscleGroup()), e.getDifficulty(),
                e.getLocation(), e.getEquipment().stream().map(Equipment::getCode).sorted().toList(),
                e.getDefaultSets(), e.getDefaultReps(), e.getDefaultDurationSec(), e.getDefaultRestSec(), e.isDurationBased());
    }

    public static ExerciseDetail toDetail(Exercise e, Long videoId) {
        return new ExerciseDetail(toSummary(e), e.getDescription(), e.getInstructions(), e.getCommonMistakes(),
                e.getSafetyPrecautions(),
                e.getSecondaryMuscles().stream().sorted(Comparator.comparingInt(MuscleGroup::getSortOrder))
                        .map(ExerciseService::toDto).toList(),
                e.getContraindications().stream().sorted().toList(),
                e.getAlternatives().stream().filter(Exercise::isActive).sorted(Comparator.comparing(Exercise::getName))
                        .map(ExerciseService::toSummary).toList(),
                videoId);
    }
}
