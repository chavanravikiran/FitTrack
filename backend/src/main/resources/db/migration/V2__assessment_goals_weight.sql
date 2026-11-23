-- =====================================================================
-- V2: Equipment reference, fitness assessments (versioned), goals, weight
-- =====================================================================

CREATE TABLE equipment (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       VARCHAR(40) NOT NULL UNIQUE,
    name       VARCHAR(80) NOT NULL,
    sort_order INTEGER     NOT NULL DEFAULT 0
);

-- A new row is inserted every time the user updates their assessment;
-- exactly one row per user is flagged is_current.
CREATE TABLE fitness_assessments (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id              BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    fitness_level        VARCHAR(20)   NOT NULL,
    primary_goal         VARCHAR(20)   NOT NULL,
    workout_location     VARCHAR(10)   NOT NULL,
    available_days       INTEGER       NOT NULL,           -- bitmask: bit0 = Monday ... bit6 = Sunday
    session_minutes      INTEGER       NOT NULL,
    diet_preference      VARCHAR(20)   NOT NULL,
    allergies            VARCHAR(500),
    dietary_restrictions VARCHAR(500),
    injury_notes         VARCHAR(1000),
    activity_level       VARCHAR(20)   NOT NULL,
    sleep_hours          NUMERIC(3, 1) NOT NULL,
    is_current           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by           BIGINT,
    updated_by           BIGINT,
    version              BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_fa_level CHECK (fitness_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_fa_goal CHECK (primary_goal IN ('WEIGHT_LOSS', 'MUSCLE_GAIN', 'MAINTENANCE', 'GENERAL_FITNESS')),
    CONSTRAINT ck_fa_location CHECK (workout_location IN ('HOME', 'GYM')),
    CONSTRAINT ck_fa_days CHECK (available_days BETWEEN 1 AND 127),
    CONSTRAINT ck_fa_minutes CHECK (session_minutes BETWEEN 10 AND 180),
    CONSTRAINT ck_fa_diet CHECK (diet_preference IN ('VEGETARIAN', 'NON_VEGETARIAN', 'VEGAN', 'EGGETARIAN')),
    CONSTRAINT ck_fa_activity CHECK (activity_level IN ('SEDENTARY', 'LIGHTLY_ACTIVE', 'MODERATELY_ACTIVE', 'VERY_ACTIVE')),
    CONSTRAINT ck_fa_sleep CHECK (sleep_hours BETWEEN 3 AND 14)
);
CREATE INDEX idx_fa_user_created ON fitness_assessments (user_id, created_at DESC);
CREATE UNIQUE INDEX uk_fa_user_current ON fitness_assessments (user_id) WHERE is_current;

CREATE TABLE assessment_equipment (
    assessment_id BIGINT NOT NULL REFERENCES fitness_assessments (id) ON DELETE CASCADE,
    equipment_id  BIGINT NOT NULL REFERENCES equipment (id) ON DELETE RESTRICT,
    PRIMARY KEY (assessment_id, equipment_id)
);

CREATE TABLE assessment_limitations (
    assessment_id BIGINT      NOT NULL REFERENCES fitness_assessments (id) ON DELETE CASCADE,
    body_area     VARCHAR(20) NOT NULL,
    severity      VARCHAR(10) NOT NULL,
    PRIMARY KEY (assessment_id, body_area),
    CONSTRAINT ck_al_area CHECK (body_area IN ('KNEE', 'LOWER_BACK', 'SHOULDER', 'WRIST', 'ANKLE', 'NECK', 'HIP', 'ELBOW')),
    CONSTRAINT ck_al_severity CHECK (severity IN ('MILD', 'MODERATE', 'SEVERE'))
);

CREATE TABLE fitness_goals (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    goal_type    VARCHAR(30)   NOT NULL,
    title        VARCHAR(120),
    start_value  NUMERIC(10, 2),
    target_value NUMERIC(10, 2) NOT NULL,
    unit         VARCHAR(15)   NOT NULL,
    start_date   DATE          NOT NULL,
    target_date  DATE,
    status       VARCHAR(15)   NOT NULL DEFAULT 'ACTIVE',
    achieved_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by   BIGINT,
    updated_by   BIGINT,
    version      BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_fg_type CHECK (goal_type IN ('TARGET_WEIGHT', 'WEEKLY_WORKOUTS', 'DAILY_STEPS', 'DAILY_WATER', 'SLEEP_HOURS', 'BODY_FAT')),
    CONSTRAINT ck_fg_status CHECK (status IN ('ACTIVE', 'ACHIEVED', 'ABANDONED')),
    CONSTRAINT ck_fg_target_positive CHECK (target_value > 0),
    CONSTRAINT ck_fg_dates CHECK (target_date IS NULL OR target_date >= start_date)
);
CREATE INDEX idx_fg_user_status ON fitness_goals (user_id, status);
CREATE UNIQUE INDEX uk_fg_user_active_type ON fitness_goals (user_id, goal_type) WHERE status = 'ACTIVE';

CREATE TABLE weight_records (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    record_date  DATE          NOT NULL,
    weight_kg    NUMERIC(5, 2) NOT NULL,
    body_fat_pct NUMERIC(4, 1),
    notes        VARCHAR(255),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by   BIGINT,
    updated_by   BIGINT,
    version      BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_weight_user_date UNIQUE (user_id, record_date),
    CONSTRAINT ck_weight_range CHECK (weight_kg BETWEEN 20 AND 400),
    CONSTRAINT ck_weight_bodyfat CHECK (body_fat_pct IS NULL OR body_fat_pct BETWEEN 2 AND 70)
);
CREATE INDEX idx_weight_user_date ON weight_records (user_id, record_date DESC);
