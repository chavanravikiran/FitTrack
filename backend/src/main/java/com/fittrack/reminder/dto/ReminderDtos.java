package com.fittrack.reminder.dto;

import com.fittrack.reminder.entity.Reminder.Channel;
import com.fittrack.reminder.entity.Reminder.Type;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class ReminderDtos {

    private ReminderDtos() {
    }

    public record ReminderRequest(
            @NotNull Type type,
            @NotBlank @Size(max = 100) String title,
            @Size(max = 255) String message,
            @NotNull @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "use HH:mm") String time,
            @NotEmpty Set<@NotNull DayOfWeek> days,
            @Min(15) @Max(480) Integer intervalMinutes,
            @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "use HH:mm") String endTime,
            @NotNull Channel channel,
            boolean enabled) {
    }

    public record ToggleRequest(@NotNull Boolean enabled) {
    }

    public record ReminderDto(Long id, Type type, String title, String message, String time, List<DayOfWeek> days,
                              Integer intervalMinutes, String endTime, Channel channel, String timeZone, boolean enabled,
                              Instant nextFireAt, Instant lastFiredAt) {
    }

    public record NotificationDto(Long id, String title, String body, Channel channel, boolean read, Instant createdAt,
                                  Long reminderId, Type reminderType) {
    }

    public record UnreadCountDto(long unread) {
    }
}
