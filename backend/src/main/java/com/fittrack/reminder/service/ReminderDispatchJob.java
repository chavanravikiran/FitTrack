package com.fittrack.reminder.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Spring Scheduler job: every minute, fire reminders whose next occurrence is due. Each reminder is processed in
 * its own transaction (via the NotificationService proxy) so one failure never blocks the others.
 * For multiple backend instances, add ShedLock (the shedlock table already exists) to run this on one node.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderDispatchJob {

    private final NotificationService notificationService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
    public void dispatchDueReminders() {
        Instant now = clock.instant();
        for (Long id : notificationService.dueReminderIds(now)) {
            try {
                notificationService.fire(id, now);
            } catch (RuntimeException e) {
                log.error("Failed to dispatch reminder {}", id, e);
            }
        }
    }
}
