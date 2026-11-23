-- =====================================================================
-- V9: Public SaaS foundation - terms acceptance, email verification,
-- subscription plans & entitlements, subscriptions, payments.
-- Money is stored in minor units (paise) as BIGINT to avoid rounding issues.
-- =====================================================================

ALTER TABLE users
    ADD COLUMN terms_accepted_at TIMESTAMPTZ,
    ADD COLUMN terms_version     VARCHAR(20);

CREATE TABLE email_verification_tokens (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by BIGINT,
    updated_by BIGINT,
    version    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_email_verification_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX idx_email_verification_tokens_user ON email_verification_tokens (user_id);

CREATE TABLE subscription_plans (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code                VARCHAR(20)  NOT NULL,
    name                VARCHAR(60)  NOT NULL,
    description         VARCHAR(500),
    price_monthly_minor BIGINT       NOT NULL DEFAULT 0,
    price_yearly_minor  BIGINT       NOT NULL DEFAULT 0,
    currency            VARCHAR(3)   NOT NULL DEFAULT 'INR',
    is_free             BOOLEAN      NOT NULL DEFAULT FALSE,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order          INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          BIGINT,
    updated_by          BIGINT,
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_subscription_plans_code UNIQUE (code),
    CONSTRAINT ck_sp_prices CHECK (price_monthly_minor >= 0 AND price_yearly_minor >= 0),
    CONSTRAINT ck_sp_free_prices CHECK (NOT is_free OR (price_monthly_minor = 0 AND price_yearly_minor = 0))
);
-- Exactly one free (fallback) plan.
CREATE UNIQUE INDEX uk_sp_single_free ON subscription_plans (is_free) WHERE is_free;

-- A feature row means the plan includes that feature; limit_value NULL = unlimited.
CREATE TABLE plan_features (
    plan_id      BIGINT      NOT NULL REFERENCES subscription_plans (id) ON DELETE CASCADE,
    feature_code VARCHAR(40) NOT NULL,
    limit_value  INTEGER,
    PRIMARY KEY (plan_id, feature_code),
    CONSTRAINT ck_pf_feature CHECK (feature_code IN ('ACTIVE_GOALS', 'ADVANCED_ANALYTICS', 'PDF_REPORTS',
        'PROGRESS_PHOTOS', 'CUSTOM_WORKOUTS', 'DATA_EXPORT', 'AI_ASSISTANT')),
    CONSTRAINT ck_pf_limit CHECK (limit_value IS NULL OR limit_value >= 0)
);

CREATE TABLE subscriptions (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id              BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan_id              BIGINT      NOT NULL REFERENCES subscription_plans (id) ON DELETE RESTRICT,
    status               VARCHAR(10) NOT NULL,
    source               VARCHAR(15) NOT NULL,
    billing_cycle        VARCHAR(10),
    current_period_start TIMESTAMPTZ NOT NULL,
    current_period_end   TIMESTAMPTZ,
    ended_at             TIMESTAMPTZ,
    notes                VARCHAR(255),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           BIGINT,
    updated_by           BIGINT,
    version              BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_sub_status CHECK (status IN ('ACTIVE', 'TRIALING', 'EXPIRED', 'CANCELED')),
    CONSTRAINT ck_sub_source CHECK (source IN ('FREE', 'TRIAL', 'PAYMENT', 'ADMIN_GRANT')),
    CONSTRAINT ck_sub_cycle CHECK (billing_cycle IS NULL OR billing_cycle IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT ck_sub_period CHECK (current_period_end IS NULL OR current_period_end > current_period_start)
);
-- Every user has at most one live subscription.
CREATE UNIQUE INDEX uk_sub_user_live ON subscriptions (user_id) WHERE status IN ('ACTIVE', 'TRIALING');
CREATE INDEX idx_sub_user ON subscriptions (user_id, created_at DESC);
CREATE INDEX idx_sub_period_end ON subscriptions (current_period_end) WHERE status IN ('ACTIVE', 'TRIALING');

CREATE TABLE payments (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan_id             BIGINT       NOT NULL REFERENCES subscription_plans (id) ON DELETE RESTRICT,
    subscription_id     BIGINT       REFERENCES subscriptions (id) ON DELETE SET NULL,
    billing_cycle       VARCHAR(10)  NOT NULL,
    amount_minor        BIGINT       NOT NULL,
    currency            VARCHAR(3)   NOT NULL,
    status              VARCHAR(10)  NOT NULL DEFAULT 'CREATED',
    provider            VARCHAR(15)  NOT NULL,
    provider_order_id   VARCHAR(64)  NOT NULL,
    provider_payment_id VARCHAR(64),
    failure_reason      VARCHAR(255),
    paid_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          BIGINT,
    updated_by          BIGINT,
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_payments_order UNIQUE (provider, provider_order_id),
    CONSTRAINT ck_pay_status CHECK (status IN ('CREATED', 'PAID', 'FAILED')),
    CONSTRAINT ck_pay_provider CHECK (provider IN ('RAZORPAY')),
    CONSTRAINT ck_pay_cycle CHECK (billing_cycle IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT ck_pay_amount CHECK (amount_minor > 0)
);
CREATE INDEX idx_pay_user ON payments (user_id, created_at DESC);
CREATE INDEX idx_pay_status_created ON payments (status, created_at DESC);

-- ---------------------------------------------------------------------
-- Seed plans. Prices are starting values - admins can change them.
-- ---------------------------------------------------------------------
INSERT INTO subscription_plans (code, name, description, price_monthly_minor, price_yearly_minor, currency, is_free, sort_order)
VALUES ('FREE', 'Free', 'Everything you need to start: personalised workouts, daily tracking and core progress charts.',
        0, 0, 'INR', TRUE, 1),
       ('PREMIUM', 'Premium', 'Unlimited goals, advanced analytics, PDF reports, progress photos, custom workouts and data export.',
        19900, 199900, 'INR', FALSE, 2);

INSERT INTO plan_features (plan_id, feature_code, limit_value)
SELECT id, 'ACTIVE_GOALS', 3 FROM subscription_plans WHERE code = 'FREE';

INSERT INTO plan_features (plan_id, feature_code, limit_value)
SELECT p.id, f.code, NULL
FROM subscription_plans p
CROSS JOIN (VALUES ('ACTIVE_GOALS'), ('ADVANCED_ANALYTICS'), ('PDF_REPORTS'), ('PROGRESS_PHOTOS'),
                   ('CUSTOM_WORKOUTS'), ('DATA_EXPORT')) AS f(code)
WHERE p.code = 'PREMIUM';

-- Existing accounts start on the Free plan.
INSERT INTO subscriptions (user_id, plan_id, status, source, current_period_start)
SELECT u.id, p.id, 'ACTIVE', 'FREE', now()
FROM users u CROSS JOIN subscription_plans p
WHERE p.is_free;
