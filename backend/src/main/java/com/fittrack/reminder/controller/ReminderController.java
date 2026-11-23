package com.fittrack.reminder.controller;

import com.fittrack.common.api.ApiResponse;
import com.fittrack.common.api.PageResponse;
import com.fittrack.reminder.dto.ReminderDtos.NotificationDto;
import com.fittrack.reminder.dto.ReminderDtos.ReminderDto;
import com.fittrack.reminder.dto.ReminderDtos.ReminderRequest;
import com.fittrack.reminder.dto.ReminderDtos.ToggleRequest;
import com.fittrack.reminder.dto.ReminderDtos.UnreadCountDto;
import com.fittrack.reminder.service.NotificationService;
import com.fittrack.reminder.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Reminders & notifications")
@Validated
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;
    private final NotificationService notificationService;

    @GetMapping("/reminders")
    public ApiResponse<List<ReminderDto>> list() {
        return ApiResponse.ok(reminderService.list());
    }

    @Operation(summary = "Create a reminder (local time in my profile time zone, repeat days, optional interval)")
    @PostMapping("/reminders")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReminderDto> create(@Valid @RequestBody ReminderRequest req) {
        return ApiResponse.ok(reminderService.create(req), "Reminder created");
    }

    @PutMapping("/reminders/{id}")
    public ApiResponse<ReminderDto> update(@PathVariable Long id, @Valid @RequestBody ReminderRequest req) {
        return ApiResponse.ok(reminderService.update(id, req), "Reminder saved");
    }

    @PatchMapping("/reminders/{id}/toggle")
    public ApiResponse<ReminderDto> toggle(@PathVariable Long id, @Valid @RequestBody ToggleRequest req) {
        return ApiResponse.ok(reminderService.toggle(id, req.enabled()));
    }

    @DeleteMapping("/reminders/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        reminderService.delete(id);
        return ApiResponse.message("Reminder deleted");
    }

    @GetMapping("/notifications")
    public ApiResponse<PageResponse<NotificationDto>> notifications(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                                                    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(notificationService.list(unreadOnly, page, size));
    }

    @GetMapping("/notifications/unread-count")
    public ApiResponse<UnreadCountDto> unreadCount() {
        return ApiResponse.ok(new UnreadCountDto(notificationService.unreadCount()));
    }

    @PatchMapping("/notifications/{id}/read")
    public ApiResponse<NotificationDto> markRead(@PathVariable Long id) {
        return ApiResponse.ok(notificationService.markRead(id));
    }

    @PostMapping("/notifications/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.message("All caught up");
    }
}
