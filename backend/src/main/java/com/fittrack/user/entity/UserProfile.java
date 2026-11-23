package com.fittrack.user.entity;

import com.fittrack.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(name = "height_cm", precision = 5, scale = 1)
    private BigDecimal heightCm;

    @Column(name = "time_zone", nullable = false, length = 50)
    private String timeZone = "UTC";

    @Column(name = "photo_path")
    private String photoPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ThemePreference theme = ThemePreference.SYSTEM;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    /** Age is derived from date of birth (never stored) so it can't go stale. */
    public Integer ageOn(LocalDate date) {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, date).getYears();
    }

    public ZoneId zoneId() {
        return ZoneId.of(timeZone);
    }

    public LocalDate today() {
        return LocalDate.now(zoneId());
    }
}
