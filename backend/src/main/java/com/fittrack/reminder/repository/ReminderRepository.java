package com.fittrack.reminder.repository;

import com.fittrack.reminder.entity.Reminder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUserIdOrderByLocalTimeAsc(Long userId);

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Query("select r.id from Reminder r where r.enabled = true and r.nextFireAt is not null and r.nextFireAt <= :now order by r.nextFireAt")
    List<Long> findDueIds(@Param("now") Instant now, Pageable pageable);
}
