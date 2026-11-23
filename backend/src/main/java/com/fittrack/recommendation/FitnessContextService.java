package com.fittrack.recommendation;

import com.fittrack.assessment.repository.FitnessAssessmentRepository;
import com.fittrack.body.entity.WeightRecord;
import com.fittrack.body.repository.WeightRecordRepository;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.user.entity.UserProfile;
import com.fittrack.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class FitnessContextService {

    private final UserProfileRepository profileRepository;
    private final FitnessAssessmentRepository assessmentRepository;
    private final WeightRecordRepository weightRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public FitnessContext load(Long userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Profile", userId));
        ZoneId zone = profile.zoneId();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        return new FitnessContext(userId, profile,
                assessmentRepository.findByUserIdAndCurrentTrue(userId).orElse(null),
                weightRepository.findFirstByUserIdOrderByRecordDateDesc(userId).map(WeightRecord::getWeightKg).orElse(null),
                profile.ageOn(today), today, zone);
    }

    public LocalDate today(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(p -> LocalDate.now(clock.withZone(p.zoneId())))
                .orElse(LocalDate.now(clock));
    }
}
