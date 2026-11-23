package com.fittrack.assessment.entity;

import com.fittrack.assessment.entity.AssessmentEnums.BodyArea;
import com.fittrack.assessment.entity.AssessmentEnums.LimitationSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A declared injury or exercise limitation; drives exercise filtering in the recommendation engine. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "bodyArea")
@Embeddable
public class Limitation {

    @Enumerated(EnumType.STRING)
    @Column(name = "body_area", nullable = false, length = 20)
    private BodyArea bodyArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private LimitationSeverity severity;
}
