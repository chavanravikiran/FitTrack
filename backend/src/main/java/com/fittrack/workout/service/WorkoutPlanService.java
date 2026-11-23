package com.fittrack.workout.service;

import com.fittrack.billing.entity.BillingEnums.Feature;
import com.fittrack.billing.service.EntitlementService;
import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.exercise.entity.Exercise;
import com.fittrack.exercise.repository.ExerciseRepository;
import com.fittrack.recommendation.FitnessContext;
import com.fittrack.recommendation.FitnessContextService;
import com.fittrack.security.CurrentUserService;
import com.fittrack.user.repository.UserRepository;
import com.fittrack.workout.dto.WorkoutDtos.PlanDto;
import com.fittrack.workout.dto.WorkoutDtos.PlanExerciseDto;
import com.fittrack.workout.entity.WorkoutExercise;
import com.fittrack.workout.entity.WorkoutPlan;
import com.fittrack.workout.entity.WorkoutPlan.Status;
import com.fittrack.workout.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutPlanService {

    private final WorkoutPlanRepository planRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final FitnessContextService contextService;
    private final PlanGenerator generator;
    private final EntitlementService entitlements;
    private final CurrentUserService currentUser;

    /** The active plan; generated on first use from the current assessment. */
    @Transactional
    public PlanDto activePlan() {
        return WorkoutMapper.toDto(activePlanEntity(currentUser.userId()));
    }

    @Transactional
    public WorkoutPlan activePlanEntity(Long userId) {
        return planRepository.findByUserIdAndStatus(userId, Status.ACTIVE)
                .orElseGet(() -> generateFor(userId));
    }

    /** Archives the current plan and builds a new one (e.g. after updating the assessment). */
    @Transactional
    public PlanDto regenerate() {
        Long userId = currentUser.userId();
        planRepository.findByUserIdAndStatus(userId, Status.ACTIVE).ifPresent(p -> {
            p.setStatus(Status.ARCHIVED);
            p.setEndDate(contextService.today(userId));
            planRepository.saveAndFlush(p); // free the "one active plan" unique index
        });
        return WorkoutMapper.toDto(generateFor(userId));
    }

    /** Premium: permanently replace an exercise in the plan with another eligible exercise. */
    @Transactional
    public PlanExerciseDto swapExercise(Long planExerciseId, Long newExerciseId) {
        Long userId = currentUser.userId();
        entitlements.requireFeature(userId, Feature.CUSTOM_WORKOUTS, "Customising your plan");
        WorkoutPlan plan = activePlanEntity(userId);
        WorkoutExercise target = plan.getDays().stream()
                .flatMap(d -> d.getExercises().stream())
                .filter(e -> e.getId().equals(planExerciseId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Plan exercise", planExerciseId));
        Exercise replacement = exerciseRepository.findWithDetailsById(newExerciseId)
                .filter(Exercise::isActive)
                .orElseThrow(() -> ResourceNotFoundException.of("Exercise", newExerciseId));

        FitnessContext ctx = contextService.load(userId);
        if (ctx.assessment() != null && !generator.isEligible(replacement, ctx.assessment())) {
            throw new BusinessRuleException("That exercise doesn't fit your equipment, level or declared limitations.");
        }
        boolean durationChange = replacement.isDurationBased() != target.getExercise().isDurationBased();
        target.setExercise(replacement);
        if (durationChange) {
            target.setDurationSec(replacement.isDurationBased() ? replacement.getDefaultDurationSec() : null);
            target.setRepsMin(replacement.isDurationBased() ? null : 10);
            target.setRepsMax(replacement.isDurationBased() ? null : 12);
        }
        target.setNotes("Swapped in by you");
        plan.setGeneratedBy(WorkoutPlan.GeneratedBy.USER);
        return WorkoutMapper.toDto(target);
    }

    private WorkoutPlan generateFor(Long userId) {
        FitnessContext ctx = contextService.load(userId);
        if (ctx.assessment() == null) {
            throw new BusinessRuleException("Complete your fitness assessment first so we can build your plan.");
        }
        List<Exercise> catalog = exerciseRepository.findActiveCatalog();
        WorkoutPlan plan = generator.generate(new PlanGenerator.Input(ctx.assessment(), ctx.age(), ctx.today()), catalog);
        plan.setUser(userRepository.getReferenceById(userId));
        plan.setAssessment(ctx.assessment());
        WorkoutPlan saved = planRepository.save(plan);
        log.info("Generated workout plan {} ({}) for user {}", saved.getId(), saved.getName(), userId);
        return saved;
    }
}
