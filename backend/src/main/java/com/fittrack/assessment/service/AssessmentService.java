package com.fittrack.assessment.service;

import com.fittrack.assessment.dto.AssessmentDtos.AssessmentRequest;
import com.fittrack.assessment.dto.AssessmentDtos.AssessmentResponse;
import com.fittrack.assessment.dto.AssessmentDtos.AssessmentSummary;
import com.fittrack.assessment.dto.AssessmentDtos.EquipmentDto;
import com.fittrack.assessment.dto.AssessmentDtos.LimitationDto;
import com.fittrack.assessment.entity.Equipment;
import com.fittrack.assessment.entity.FitnessAssessment;
import com.fittrack.assessment.entity.Limitation;
import com.fittrack.assessment.mapper.AssessmentMapper;
import com.fittrack.assessment.repository.EquipmentRepository;
import com.fittrack.assessment.repository.FitnessAssessmentRepository;
import com.fittrack.common.api.PageResponse;
import com.fittrack.common.exception.BadRequestException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.common.util.DayMask;
import com.fittrack.security.CurrentUserService;
import com.fittrack.user.entity.User;
import com.fittrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final FitnessAssessmentRepository assessmentRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final AssessmentMapper mapper;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<EquipmentDto> listEquipment() {
        return equipmentRepository.findAllByOrderBySortOrderAsc().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AssessmentResponse getCurrent() {
        return assessmentRepository.findByUserIdAndCurrentTrue(currentUser.userId())
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No fitness assessment yet. Please complete onboarding."));
    }

    @Transactional(readOnly = true)
    public AssessmentResponse getById(Long id) {
        return assessmentRepository.findByIdAndUserId(id, currentUser.userId())
                .map(mapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Assessment", id));
    }

    @Transactional(readOnly = true)
    public PageResponse<AssessmentSummary> history(int page, int size) {
        return PageResponse.of(assessmentRepository.findByUserIdOrderByCreatedAtDesc(
                currentUser.userId(), PageRequest.of(page, size)), mapper::toSummary);
    }

    /** Re-assessment: stores a new version and makes it current. */
    @Transactional
    public AssessmentResponse reassess(AssessmentRequest request) {
        User user = userRepository.getReferenceById(currentUser.userId());
        return mapper.toResponse(createVersion(user, request));
    }

    /** Used by onboarding and re-assessment. Caller must be inside a transaction. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public FitnessAssessment createVersion(User user, AssessmentRequest request) {
        assessmentRepository.clearCurrent(user.getId());

        FitnessAssessment a = new FitnessAssessment();
        a.setUser(user);
        a.setFitnessLevel(request.fitnessLevel());
        a.setPrimaryGoal(request.primaryGoal());
        a.setWorkoutLocation(request.workoutLocation());
        a.setAvailableDays(DayMask.toMask(request.availableDays()));
        a.setSessionMinutes(request.sessionMinutes());
        a.setDietPreference(request.dietPreference());
        a.setAllergies(trimToNull(request.allergies()));
        a.setDietaryRestrictions(trimToNull(request.dietaryRestrictions()));
        a.setInjuryNotes(trimToNull(request.injuryNotes()));
        a.setActivityLevel(request.activityLevel());
        a.setSleepHours(request.sleepHours());
        a.setCurrent(true);
        a.setEquipment(resolveEquipment(request.equipmentCodes()));
        a.setLimitations(toLimitations(request.limitations()));
        return assessmentRepository.save(a);
    }

    private Set<Equipment> resolveEquipment(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new HashSet<>();
        }
        List<Equipment> found = equipmentRepository.findByCodeIn(codes);
        if (found.size() != codes.size()) {
            Set<String> known = found.stream().map(Equipment::getCode).collect(Collectors.toSet());
            Set<String> unknown = new HashSet<>(codes);
            unknown.removeAll(known);
            throw new BadRequestException("Unknown equipment: " + String.join(", ", unknown));
        }
        return new HashSet<>(found);
    }

    private Set<Limitation> toLimitations(List<LimitationDto> dtos) {
        if (dtos == null) {
            return new HashSet<>();
        }
        Set<Limitation> result = new HashSet<>();
        for (LimitationDto dto : dtos) {
            if (!result.add(new Limitation(dto.bodyArea(), dto.severity()))) {
                throw new BadRequestException("Body area listed more than once: " + dto.bodyArea());
            }
        }
        return result;
    }

    private static String trimToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
