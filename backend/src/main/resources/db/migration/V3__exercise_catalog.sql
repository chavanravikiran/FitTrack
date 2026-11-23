-- =====================================================================
-- V3: Exercise catalog and exercise video library
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE muscle_groups (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       VARCHAR(20) NOT NULL UNIQUE,
    name       VARCHAR(50) NOT NULL,
    sort_order INTEGER     NOT NULL DEFAULT 0
);

CREATE TABLE exercises (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                    VARCHAR(100)  NOT NULL,
    slug                    VARCHAR(120)  NOT NULL,
    description             TEXT,
    primary_muscle_group_id BIGINT        NOT NULL REFERENCES muscle_groups (id) ON DELETE RESTRICT,
    difficulty              VARCHAR(20)   NOT NULL,
    location                VARCHAR(10)   NOT NULL DEFAULT 'BOTH',
    default_sets            INTEGER,
    default_reps            INTEGER,
    default_duration_sec    INTEGER,
    default_rest_sec        INTEGER,
    instructions            TEXT,
    common_mistakes         TEXT,
    safety_precautions      TEXT,
    met_value               NUMERIC(4, 1),
    video_url               VARCHAR(500),
    active                  BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by              BIGINT,
    updated_by              BIGINT,
    version                 BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_exercises_name UNIQUE (name),
    CONSTRAINT uk_exercises_slug UNIQUE (slug),
    CONSTRAINT ck_ex_difficulty CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_ex_location CHECK (location IN ('HOME', 'GYM', 'BOTH')),
    CONSTRAINT ck_ex_sets CHECK (default_sets IS NULL OR default_sets BETWEEN 1 AND 10),
    CONSTRAINT ck_ex_reps CHECK (default_reps IS NULL OR default_reps BETWEEN 1 AND 100),
    CONSTRAINT ck_ex_duration CHECK (default_duration_sec IS NULL OR default_duration_sec BETWEEN 5 AND 7200),
    CONSTRAINT ck_ex_rest CHECK (default_rest_sec IS NULL OR default_rest_sec BETWEEN 0 AND 600)
);
CREATE INDEX idx_ex_muscle_difficulty ON exercises (primary_muscle_group_id, difficulty);
CREATE INDEX idx_ex_name_trgm ON exercises USING gin (name gin_trgm_ops);

CREATE TABLE exercise_secondary_muscles (
    exercise_id     BIGINT NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    muscle_group_id BIGINT NOT NULL REFERENCES muscle_groups (id) ON DELETE RESTRICT,
    PRIMARY KEY (exercise_id, muscle_group_id)
);

CREATE TABLE exercise_equipment (
    exercise_id  BIGINT NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    equipment_id BIGINT NOT NULL REFERENCES equipment (id) ON DELETE RESTRICT,
    PRIMARY KEY (exercise_id, equipment_id)
);

-- Exercises to avoid (or substitute) when the user declares a limitation in that body area.
CREATE TABLE exercise_contraindications (
    exercise_id BIGINT      NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    body_area   VARCHAR(20) NOT NULL,
    PRIMARY KEY (exercise_id, body_area),
    CONSTRAINT ck_ec_area CHECK (body_area IN ('KNEE', 'LOWER_BACK', 'SHOULDER', 'WRIST', 'ANKLE', 'NECK', 'HIP', 'ELBOW'))
);

CREATE TABLE exercise_alternatives (
    exercise_id             BIGINT NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    alternative_exercise_id BIGINT NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    PRIMARY KEY (exercise_id, alternative_exercise_id),
    CONSTRAINT ck_ea_not_self CHECK (exercise_id <> alternative_exercise_id)
);

CREATE TABLE exercise_videos (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    exercise_id     BIGINT       REFERENCES exercises (id) ON DELETE SET NULL,
    muscle_group_id BIGINT       REFERENCES muscle_groups (id) ON DELETE SET NULL,
    title           VARCHAR(150) NOT NULL,
    category        VARCHAR(20)  NOT NULL,
    difficulty      VARCHAR(20)  NOT NULL,
    provider        VARCHAR(20)  NOT NULL DEFAULT 'YOUTUBE',
    embed_url       VARCHAR(500),
    thumbnail_url   VARCHAR(500),
    instructions    TEXT,
    license_note    VARCHAR(255),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      BIGINT,
    updated_by      BIGINT,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_ev_category CHECK (category IN ('CHEST', 'BACK', 'LEGS', 'SHOULDERS', 'ARMS', 'ABS', 'CARDIO', 'WARM_UP', 'STRETCHING')),
    CONSTRAINT ck_ev_difficulty CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_ev_provider CHECK (provider IN ('YOUTUBE', 'VIMEO', 'SELF_HOSTED'))
);
CREATE INDEX idx_ev_category_difficulty ON exercise_videos (category, difficulty);
CREATE INDEX idx_ev_title_trgm ON exercise_videos USING gin (title gin_trgm_ops);

CREATE TABLE exercise_video_equipment (
    video_id     BIGINT NOT NULL REFERENCES exercise_videos (id) ON DELETE CASCADE,
    equipment_id BIGINT NOT NULL REFERENCES equipment (id) ON DELETE RESTRICT,
    PRIMARY KEY (video_id, equipment_id)
);
