-- =====================================================================
-- V5: Daily tracking - activity/steps, water, body measurements, sleep
-- All record_date values are calendar dates in the user's time zone.
-- =====================================================================

CREATE TABLE daily_activity_records (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    record_date    DATE        NOT NULL,
    steps          INTEGER     NOT NULL DEFAULT 0,
    distance_m     INTEGER,
    active_minutes INTEGER,
    calories_est   INTEGER,
    step_goal      INTEGER,
    source         VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     BIGINT,
    updated_by     BIGINT,
    version        BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_dar_user_date UNIQUE (user_id, record_date),
    CONSTRAINT ck_dar_steps CHECK (steps BETWEEN 0 AND 100000),
    CONSTRAINT ck_dar_distance CHECK (distance_m IS NULL OR distance_m BETWEEN 0 AND 200000),
    CONSTRAINT ck_dar_minutes CHECK (active_minutes IS NULL OR active_minutes BETWEEN 0 AND 1440),
    CONSTRAINT ck_dar_source CHECK (source IN ('MANUAL', 'HEALTH_CONNECT', 'APPLE_HEALTH', 'IMPORT'))
);
CREATE INDEX idx_dar_user_date ON daily_activity_records (user_id, record_date DESC);

CREATE TABLE water_intake_entries (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    record_date DATE        NOT NULL,
    amount_ml   INTEGER     NOT NULL,
    logged_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT,
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_wie_amount CHECK (amount_ml BETWEEN 1 AND 3000)
);
CREATE INDEX idx_wie_user_date ON water_intake_entries (user_id, record_date DESC);

CREATE TABLE water_daily_goals (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    record_date       DATE         NOT NULL,
    goal_ml           INTEGER      NOT NULL,
    adjustment_reason VARCHAR(255),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by        BIGINT,
    updated_by        BIGINT,
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_wdg_user_date UNIQUE (user_id, record_date),
    CONSTRAINT ck_wdg_goal CHECK (goal_ml BETWEEN 500 AND 6000)
);

CREATE TABLE body_measurement_records (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    record_date DATE          NOT NULL,
    waist_cm    NUMERIC(5, 1),
    chest_cm    NUMERIC(5, 1),
    arm_cm      NUMERIC(5, 1),
    hip_cm      NUMERIC(5, 1),
    thigh_cm    NUMERIC(5, 1),
    notes       VARCHAR(255),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT,
    version     BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_bmr_user_date UNIQUE (user_id, record_date),
    CONSTRAINT ck_bmr_any CHECK (COALESCE(waist_cm, chest_cm, arm_cm, hip_cm, thigh_cm) IS NOT NULL),
    CONSTRAINT ck_bmr_waist CHECK (waist_cm IS NULL OR waist_cm BETWEEN 30 AND 250),
    CONSTRAINT ck_bmr_chest CHECK (chest_cm IS NULL OR chest_cm BETWEEN 30 AND 250),
    CONSTRAINT ck_bmr_arm CHECK (arm_cm IS NULL OR arm_cm BETWEEN 10 AND 100),
    CONSTRAINT ck_bmr_hip CHECK (hip_cm IS NULL OR hip_cm BETWEEN 30 AND 250),
    CONSTRAINT ck_bmr_thigh CHECK (thigh_cm IS NULL OR thigh_cm BETWEEN 20 AND 150)
);
CREATE INDEX idx_bmr_user_date ON body_measurement_records (user_id, record_date DESC);

CREATE TABLE sleep_records (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    sleep_date   DATE        NOT NULL,               -- the date the user woke up
    sleep_start  TIMESTAMPTZ NOT NULL,
    wake_time    TIMESTAMPTZ NOT NULL,
    duration_min INTEGER     NOT NULL,
    quality      INTEGER,
    is_rest_day  BOOLEAN     NOT NULL DEFAULT FALSE,
    notes        VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   BIGINT,
    updated_by   BIGINT,
    version      BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_sleep_user_date UNIQUE (user_id, sleep_date),
    CONSTRAINT ck_sleep_order CHECK (wake_time > sleep_start),
    CONSTRAINT ck_sleep_duration CHECK (duration_min BETWEEN 1 AND 1440),
    CONSTRAINT ck_sleep_quality CHECK (quality IS NULL OR quality BETWEEN 1 AND 5)
);
CREATE INDEX idx_sleep_user_date ON sleep_records (user_id, sleep_date DESC);
