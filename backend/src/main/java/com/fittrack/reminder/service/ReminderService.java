package com.fittrack.reminder.service;

import com.fittrack.common.exception.BadRequestException;
import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.common.util.DayMask;
import com.fittrack.reminder.dto.ReminderDtos.ReminderDto;
import com.fittrack.reminder.dto.ReminderDtos.ReminderRequest;
import com.fittrack.reminder.entity.Reminder;
import com.fittrack.reminder.repository.ReminderRepository;
import com.fittrack.security.CurrentUserService;
import com.fittrack.user.entity.UserProfile;
import com.fittrack.user.repository.UserProfileRepository;
import com.fittrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final int MAX_REMINDERS = 30;
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final ReminderRepository repository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final CurrentUserService currentUser;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ReminderDto> list() {
        return repository.findByUserIdOrderByLocalTimeAsc(currentUser.userId()).stream().map(ReminderService::toDto).toList();
    }

    @Transactional
    public ReminderDto create(ReminderRequest req) {
        Long userId = currentUser.userId();
        if (repository.countByUserId(userId) >= MAX_REMINDERS) {
            throw new BusinessRuleException("You can have up to " + MAX_REMINDERS + " reminders");
        }
        Reminder r = new Reminder();
        r.setUser(userRepository.getReferenceById(userId));
        apply(r, req, userId);
        return toDto(repository.save(r));
    }

    @Transactional
    public ReminderDto update(Long id, ReminderRequest req) {
        Reminder r = find(id);
        apply(r, req, currentUser.userId());
        return toDto(r);
    }

    @Transactional
    public ReminderDto toggle(Long id, boolean enabled) {
        Reminder r = find(id);
        r.setEnabled(enabled);
        r.setNextFireAt(enabled ? ReminderSchedule.next(r, clock.instant()) : null);
        return toDto(r);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private void apply(Reminder r, ReminderRequest req, Long userId) {
        LocalTime time = LocalTime.parse(req.time());
        LocalTime end = StringUtils.hasText(req.endTime()) ? LocalTime.parse(req.endTime()) : null;
        if (req.intervalMinutes() != null && end != null && !end.isAfter(time)) {
            throw new BadRequestException("End time must be after the start time");
        }
        UserProfile profile = profileRepository.findByUserId(userId).orElseThrow();
        r.setType(req.type());
        r.setTitle(req.title().trim());
        r.setMessage(StringUtils.hasText(req.message()) ? req.message().trim() : null);
        r.setLocalTime(time);
        r.setRepeatDays(DayMask.toMask(req.days()));
        r.setIntervalMinutes(req.intervalMinutes());
        r.setEndTime(req.intervalMinutes() == null ? null : end);
        r.setChannel(req.channel());
        r.setTimeZone(profile.getTimeZone()); // reminders follow the user's profile time zone
        r.setEnabled(req.enabled());
        r.setNextFireAt(req.enabled() ? ReminderSchedule.next(r, clock.instant()) : null);
    }

    private Reminder find(Long id) {
        return repository.findByIdAndUserId(id, currentUser.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("Reminder", id));
    }

    static ReminderDto toDto(Reminder r) {
        return new ReminderDto(r.getId(), r.getType(), r.getTitle(), r.getMessage(), r.getLocalTime().format(HHMM),
                DayMask.fromMask(r.getRepeatDays()).stream().sorted().toList(), r.getIntervalMinutes(),
                r.getEndTime() == null ? null : r.getEndTime().format(HHMM), r.getChannel(), r.getTimeZone(),
                r.isEnabled(), r.getNextFireAt(), r.getLastFiredAt());
    }
}
