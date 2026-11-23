-- =====================================================================
-- V7: Reminders, notifications, push subscriptions, progress photos,
-- and the ShedLock table for safe scheduling across instances.
-- =====================================================================

CREATE TABLE reminders (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type             VARCHAR(15)  NOT NULL,
    title            VARCHAR(100) NOT NULL,
    message          VARCHAR(255),
    local_time       TIME         NOT NULL,
    repeat_days      INTEGER      NOT NULL DEFAULT 127,   -- bitmask: bit0 = Monday ... bit6 = Sunday
    interval_minutes INTEGER,                             -- optional repeat within the day (water)
    end_time         TIME,
    channel          VARCHAR(10)  NOT NULL DEFAULT 'PUSH',
    time_zone        VARCHAR(50)  NOT NULL,
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    next_fire_at     TIMESTAMPTZ,
    last_fired_at    TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       BIGINT,
    updated_by       BIGINT,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_rem_type CHECK (type IN ('WAKE_UP', 'WATER', 'WORKOUT', 'WALK', 'MEAL', 'WEIGHT', 'SLEEP')),
    CONSTRAINT ck_rem_days CHECK (repeat_days BETWEEN 1 AND 127),
    CONSTRAINT ck_rem_interval CHECK (interval_minutes IS NULL OR interval_minutes BETWEEN 15 AND 480),
    CONSTRAINT ck_rem_channel CHECK (channel IN ('PUSH', 'IN_APP', 'EMAIL'))
);
CREATE INDEX idx_rem_user ON reminders (user_id);
CREATE INDEX idx_rem_next_fire ON reminders (next_fire_at) WHERE enabled;

CREATE TABLE notifications (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reminder_id BIGINT       REFERENCES reminders (id) ON DELETE SET NULL,
    title       VARCHAR(100) NOT NULL,
    body        VARCHAR(500),
    channel     VARCHAR(10)  NOT NULL,
    status      VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    sent_at     TIMESTAMPTZ,
    read_at     TIMESTAMPTZ,
    error       VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT,
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_ntf_channel CHECK (channel IN ('PUSH', 'IN_APP', 'EMAIL')),
    CONSTRAINT ck_ntf_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'READ'))
);
CREATE INDEX idx_ntf_user_status ON notifications (user_id, status, created_at DESC);

CREATE TABLE push_subscriptions (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    endpoint   VARCHAR(1000) NOT NULL,
    p256dh     VARCHAR(255) NOT NULL,
    auth       VARCHAR(255) NOT NULL,
    user_agent VARCHAR(255),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by BIGINT,
    updated_by BIGINT,
    version    BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_push_endpoint UNIQUE (endpoint)
);
CREATE INDEX idx_push_user ON push_subscriptions (user_id);

CREATE TABLE progress_photos (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    taken_on     DATE         NOT NULL,
    pose         VARCHAR(10)  NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    notes        VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   BIGINT,
    updated_by   BIGINT,
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_pp_pose CHECK (pose IN ('FRONT', 'SIDE', 'BACK'))
);
CREATE INDEX idx_pp_user_date ON progress_photos (user_id, taken_on DESC);

CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);
