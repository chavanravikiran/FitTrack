-- =====================================================================
-- V4: Workout plans, scheduled sessions, logged sets, personal records
-- =====================================================================

CREATE TABLE workout_plans (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    assessment_id BIGINT       REFERENCES fitness_assessments (id) ON DELETE SET NULL,
    name          VARCHAR(100) NOT NULL,
    split_type    VARCHAR(20)  NOT NULL,
    start_date    DATE         NOT NULL,
    end_date      DATE,
    status        VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    generated_by  VARCHAR(10)  NOT NULL DEFAULT 'ENGINE',
    notes         VARCHAR(1000),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    BIGINT,
    updated_by    BIGINT,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_wp_split CHECK (split_type IN ('FULL_BODY', 'UPPER_LOWER', 'PUSH_PULL_LEGS', 'CUSTOM')),
    CONSTRAINT ck_wp_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_wp_generated CHECK (generated_by IN ('ENGINE', 'USER')),
    CONSTRAINT ck_wp_dates CHECK (end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX idx_wp_user ON workout_plans (user_id);
CREATE UNIQUE INDEX uk_wp_user_active ON workout_plans (user_id) WHERE status = 'ACTIVE';

CREATE TABLE workout_plan_days (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plan_id      BIGINT       NOT NULL REFERENCES workout_plans (id) ON DELETE CASCADE,
    day_of_week  INTEGER      NOT NULL,          -- ISO 1 = Monday ... 7 = Sunday
    title        VARCHAR(100) NOT NULL,
    focus        VARCHAR(255),
    is_rest_day  BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order   INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   BIGINT,
    updated_by   BIGINT,
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_wpd_plan_day UNIQUE (plan_id, day_of_week),
    CONSTRAINT ck_wpd_dow CHECK (day_of_week BETWEEN 1 AND 7)
);

CREATE TABLE workout_exercises (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plan_day_id  BIGINT      NOT NULL REFERENCES workout_plan_days (id) ON DELETE CASCADE,
    exercise_id  BIGINT      NOT NULL REFERENCES exercises (id) ON DELETE RESTRICT,
    sets         INTEGER     NOT NULL,
    reps_min     INTEGER,
    reps_max     INTEGER,
    duration_sec INTEGER,
    rest_sec     INTEGER     NOT NULL DEFAULT 60,
    target_rpe   INTEGER,
    sort_order   INTEGER     NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   BIGINT,
    updated_by   BIGINT,
    version      BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_we_sets CHECK (sets BETWEEN 1 AND 10),
    CONSTRAINT ck_we_reps CHECK (reps_min IS NULL OR (reps_min >= 1 AND (reps_max IS NULL OR reps_max >= reps_min))),
    CONSTRAINT ck_we_volume CHECK (reps_min IS NOT NULL OR duration_sec IS NOT NULL),
    CONSTRAINT ck_we_rpe CHECK (target_rpe IS NULL OR target_rpe BETWEEN 1 AND 10)
);
CREATE INDEX idx_we_plan_day ON workout_exercises (plan_day_id, sort_order);

CREATE TABLE workout_sessions (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan_day_id      BIGINT      REFERENCES workout_plan_days (id) ON DELETE SET NULL,
    scheduled_date   DATE        NOT NULL,
    status           VARCHAR(15) NOT NULL DEFAULT 'SCHEDULED',
    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    rescheduled_to   DATE,
    perceived_effort INTEGER,
    calories_est     INTEGER,
    notes            VARCHAR(1000),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       BIGINT,
    updated_by       BIGINT,
    version          BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_ws_user_day_date UNIQUE (user_id, plan_day_id, scheduled_date),
    CONSTRAINT ck_ws_status CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED', 'RESCHEDULED')),
    CONSTRAINT ck_ws_effort CHECK (perceived_effort IS NULL OR perceived_effort BETWEEN 1 AND 10),
    CONSTRAINT ck_ws_completed CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at)
);
CREATE INDEX idx_ws_user_date ON workout_sessions (user_id, scheduled_date DESC);
CREATE INDEX idx_ws_user_status ON workout_sessions (user_id, status);

CREATE TABLE workout_session_exercises (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id   BIGINT        NOT NULL REFERENCES workout_sessions (id) ON DELETE CASCADE,
    exercise_id  BIGINT        NOT NULL REFERENCES exercises (id) ON DELETE RESTRICT,
    set_number   INTEGER       NOT NULL,
    reps_done    INTEGER,
    weight_kg    NUMERIC(6, 2),
    duration_sec INTEGER,
    completed    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by   BIGINT,
    updated_by   BIGINT,
    version      BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_wse_session_ex_set UNIQUE (session_id, exercise_id, set_number),
    CONSTRAINT ck_wse_set CHECK (set_number BETWEEN 1 AND 20),
    CONSTRAINT ck_wse_reps CHECK (reps_done IS NULL OR reps_done BETWEEN 0 AND 500),
    CONSTRAINT ck_wse_weight CHECK (weight_kg IS NULL OR weight_kg BETWEEN 0 AND 500)
);
CREATE INDEX idx_wse_session ON workout_session_exercises (session_id);
CREATE INDEX idx_wse_exercise ON workout_session_exercises (exercise_id);

CREATE TABLE personal_records (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exercise_id BIGINT         NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    record_type VARCHAR(20)    NOT NULL,
    value       NUMERIC(10, 2) NOT NULL,
    achieved_on DATE           NOT NULL,
    session_id  BIGINT         REFERENCES workout_sessions (id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT,
    version     BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT uk_pr_user_ex_type UNIQUE (user_id, exercise_id, record_type),
    CONSTRAINT ck_pr_type CHECK (record_type IN ('MAX_WEIGHT', 'MAX_REPS', 'LONGEST_DURATION'))
);
