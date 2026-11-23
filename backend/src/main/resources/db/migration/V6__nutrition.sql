-- =====================================================================
-- V6: Food database and meal logging
-- =====================================================================

CREATE TABLE nutrition_foods (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name               VARCHAR(120)  NOT NULL,
    category           VARCHAR(30)   NOT NULL,
    diet_type          VARCHAR(15)   NOT NULL,
    serving_qty        NUMERIC(7, 2) NOT NULL,
    serving_unit       VARCHAR(20)   NOT NULL,
    calories           NUMERIC(7, 2) NOT NULL,
    protein_g          NUMERIC(6, 2) NOT NULL DEFAULT 0,
    carbs_g            NUMERIC(6, 2) NOT NULL DEFAULT 0,
    fat_g              NUMERIC(6, 2) NOT NULL DEFAULT 0,
    is_system          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by_user_id BIGINT        REFERENCES users (id) ON DELETE CASCADE,
    active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by         BIGINT,
    updated_by         BIGINT,
    version            BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_nf_category CHECK (category IN ('GRAINS', 'PULSES', 'DAIRY', 'EGGS', 'MEAT', 'SEAFOOD', 'FRUITS', 'VEGETABLES', 'NUTS_SEEDS', 'BEVERAGES', 'SNACKS', 'PREPARED', 'OTHER')),
    CONSTRAINT ck_nf_diet CHECK (diet_type IN ('VEGETARIAN', 'NON_VEGETARIAN', 'VEGAN', 'EGGETARIAN')),
    CONSTRAINT ck_nf_values CHECK (serving_qty > 0 AND calories >= 0 AND protein_g >= 0 AND carbs_g >= 0 AND fat_g >= 0),
    CONSTRAINT ck_nf_owner CHECK (is_system OR created_by_user_id IS NOT NULL)
);
CREATE INDEX idx_nf_name_trgm ON nutrition_foods USING gin (name gin_trgm_ops);
CREATE INDEX idx_nf_category_diet ON nutrition_foods (category, diet_type);
CREATE UNIQUE INDEX uk_nf_system_name ON nutrition_foods (lower(name)) WHERE is_system;

CREATE TABLE meal_records (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    record_date DATE        NOT NULL,
    meal_type   VARCHAR(20) NOT NULL,
    notes       VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  BIGINT,
    updated_by  BIGINT,
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_mr_user_date_type UNIQUE (user_id, record_date, meal_type),
    CONSTRAINT ck_mr_type CHECK (meal_type IN ('BREAKFAST', 'LUNCH', 'EVENING_SNACK', 'DINNER'))
);
CREATE INDEX idx_mr_user_date ON meal_records (user_id, record_date DESC);

-- Nutrient values are snapshotted at log time so later food edits never rewrite history.
CREATE TABLE meal_record_items (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    meal_record_id BIGINT        NOT NULL REFERENCES meal_records (id) ON DELETE CASCADE,
    food_id        BIGINT        NOT NULL REFERENCES nutrition_foods (id) ON DELETE RESTRICT,
    servings       NUMERIC(5, 2) NOT NULL,
    calories       NUMERIC(8, 2) NOT NULL,
    protein_g      NUMERIC(7, 2) NOT NULL,
    carbs_g        NUMERIC(7, 2) NOT NULL,
    fat_g          NUMERIC(7, 2) NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by     BIGINT,
    updated_by     BIGINT,
    version        BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_mri_servings CHECK (servings > 0 AND servings <= 20)
);
CREATE INDEX idx_mri_meal ON meal_record_items (meal_record_id);
