package com.fittrack.reminder.service;

import com.fittrack.common.api.PageResponse;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.notification.email.EmailService;
import com.fittrack.reminder.dto.ReminderDtos.NotificationDto;
import com.fittrack.reminder.entity.Notification;
import com.fittrack.reminder.entity.Notification.Status;
import com.fittrack.reminder.entity.Reminder;
import com.fittrack.reminder.entity.Reminder.Channel;
import com.fittrack.reminder.repository.NotificationRepository;
import com.fittrack.reminder.repository.ReminderRepository;
import com.fittrack.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Delivers reminders. Every reminder creates an in-app notification (shown in the app's bell and as a browser
 * notification while the app is open); EMAIL reminders are also emailed. Background Web Push is planned.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int BATCH = 200;
    /** If the server was down, don't flood users with reminders that are hours old. */
    private static final Duration MAX_LATENESS = Duration.ofMinutes(30);

    private final NotificationRepository notificationRepository;
    private final ReminderRepository reminderRepository;
    private final EmailService emailService;
    private final CurrentUserService currentUser;
    private final Clock clock;

    // ---------------------------------------------------------------- dispatch

    @Transactional(readOnly = true)
    public List<Long> dueReminderIds(Instant now) {
        return reminderRepository.findDueIds(now, PageRequest.of(0, BATCH));
    }

    /** Fires one reminder in its own transaction and schedules its next occurrence. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fire(Long reminderId, Instant now) {
        Reminder r = reminderRepository.findById(reminderId).orElse(null);
        if (r == null || !r.isEnabled() || r.getNextFireAt() == null || r.getNextFireAt().isAfter(now)) return;

        boolean tooLate = r.getNextFireAt().isBefore(now.minus(MAX_LATENESS));
        if (!tooLate) {
            String body = r.getMessage() != null ? r.getMessage() : defaultMessage(r.getType());
            Notification n = new Notification();
            n.setUser(r.getUser());
            n.setReminder(r);
            n.setTitle(r.getTitle());
            n.setBody(body);
            n.setChannel(r.getChannel());
            n.setStatus(Status.SENT);
            n.setSentAt(now);
            if (r.getChannel() == Channel.EMAIL) {
                try {
                    emailService.send(r.getUser().getEmail(), "FitTrack reminder: " + r.getTitle(),
                            body + "\n\nManage your reminders in FitTrack -> Reminders.");
                } catch (RuntimeException e) {
                    n.setStatus(Status.FAILED);
                    n.setError(e.getMessage() == null ? "Email failed" : e.getMessage().substring(0, Math.min(500, e.getMessage().length())));
                }
            }
            notificationRepository.save(n);
            r.setLastFiredAt(now);
        }
        r.setNextFireAt(ReminderSchedule.next(r, now));
    }

    @Scheduled(cron = "0 15 4 * * *", zone = "UTC")
    @Transactional
    public void purgeOld() {
        int removed = notificationRepository.deleteOlderThan(clock.instant().minus(Duration.ofDays(60)));
        log.info("Removed {} notifications older than 60 days", removed);
    }

    // ---------------------------------------------------------------- user API

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(boolean unreadOnly, int page, int size) {
        Long userId = currentUser.userId();
        PageRequest pr = PageRequest.of(page, size);
        return PageResponse.of(unreadOnly
                ? notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, Status.SENT, pr)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pr), NotificationService::toDto);
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByUserIdAndStatus(currentUser.userId(), Status.SENT);
    }

    @Transactional
    public NotificationDto markRead(Long id) {
        Notification n = notificationRepository.findByIdAndUserId(id, currentUser.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", id));
        if (n.getStatus() == Status.SENT) {
            n.setStatus(Status.READ);
            n.setReadAt(clock.instant());
        }
        return toDto(n);
    }

    @Transactional
    public int markAllRead() {
        return notificationRepository.markAllRead(currentUser.userId(), clock.instant());
    }

    static NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getBody(), n.getChannel(), n.getStatus() == Status.READ,
                n.getCreatedAt(), n.getReminder() == null ? null : n.getReminder().getId(),
                n.getReminder() == null ? null : n.getReminder().getType());
    }

    static String defaultMessage(Reminder.Type type) {
        return switch (type) {
            case WAKE_UP -> "Good morning! Start your day with a glass of water.";
            case WATER -> "Time for a glass of water.";
            case WORKOUT -> "Your workout is coming up - check today's plan.";
            case WALK -> "Time for a walk - every step counts towards your goal.";
            case MEAL -> "Time to eat. Remember to log your meal.";
            case WEIGHT -> "Time for your weigh-in. Same time and conditions give the most accurate trend.";
            case SLEEP -> "Wind down for bed - good sleep helps recovery.";
        };
    }
}
