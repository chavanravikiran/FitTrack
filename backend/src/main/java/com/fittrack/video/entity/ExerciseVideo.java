package com.fittrack.video.entity;

import com.fittrack.assessment.entity.Equipment;
import com.fittrack.common.entity.BaseEntity;
import com.fittrack.exercise.entity.Exercise;
import com.fittrack.exercise.entity.Exercise.Difficulty;
import com.fittrack.exercise.entity.MuscleGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Library entry. {@code embedUrl} holds a canonical YouTube/Vimeo embed URL, or - for SELF_HOSTED - the
 * storage key of a file uploaded to S3. {@code thumbnailUrl} holds a storage key for uploaded thumbnails.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exercise_videos")
public class ExerciseVideo extends BaseEntity {

    public enum Category { CHEST, BACK, LEGS, SHOULDERS, ARMS, ABS, CARDIO, WARM_UP, STRETCHING }

    public enum Provider { YOUTUBE, VIMEO, SELF_HOSTED }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "muscle_group_id")
    private MuscleGroup muscleGroup;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider = Provider.YOUTUBE;

    @Column(name = "embed_url", length = 500)
    private String embedUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(columnDefinition = "text")
    private String instructions;

    @Column(name = "license_note", length = 255)
    private String licenseNote;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "exercise_video_equipment",
            joinColumns = @JoinColumn(name = "video_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    private Set<Equipment> equipment = new HashSet<>();
}
