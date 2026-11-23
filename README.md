# FitTrack – Smart Personal Fitness & Gym Coach

Angular 19 (NgModules) + Spring Boot 3.5 (Java 17) + PostgreSQL 16, built in phases.
See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full design, ER diagram and phase plan.

```
FitTrack/
├─ backend/            Spring Boot API  (Maven)
├─ frontend/           Angular SPA      (npm)
├─ docs/               Architecture & decisions
└─ docker-compose.yml  Dev PostgreSQL (port 5434) + optional MailHog
```

## Status

| Phase | Scope | State |
|---|---|---|
| 1 | Architecture, full DB schema (Flyway V1–V8), project skeletons, shared infrastructure | ✅ Done |
| 2 | Auth (register/login/refresh/logout/forgot/reset/change password), profile + photo, onboarding, assessment versioning, goals | ✅ Done |
| SaaS | Free/Premium plans & entitlements, 7-day trial, Razorpay checkout + webhook, admin panel, landing/pricing/legal pages, email verification, rate limiting, account deletion | ✅ Done |
| 3, 4, 5, 8, 9 | Recommendations, workouts, activity/water/sleep, nutrition, videos (S3), reminders | ✅ Done |
| 6, 7, 10 | Weight & measurements, analytics dashboard (charts), hardening & deployment | ⏳ Next |

Routes for later modules show a clearly labelled "coming in phase N" page. There is no fake data.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 20.11+ (Angular 19), npm
- Docker Desktop (dev database + Testcontainers integration tests)

## Run locally

```bash
# 1. Database: the dev profile uses the local PostgreSQL database "fittrack"
#    (localhost:5432, user postgres / password root). Create it once in pgAdmin.
#    Alternative: `docker compose up -d postgres` and set DB_URL=jdbc:postgresql://localhost:5434/fittrack,
#    DB_USERNAME=fittrack, DB_PASSWORD=fittrack.

# 2. Backend → http://localhost:8081  (Swagger UI: /swagger-ui.html)
cd backend
mvn spring-boot:run

# 3. Frontend → http://localhost:4200  (proxies /api to the backend on 8081)
cd frontend
npm install
npm start
```

`mvn spring-boot:run` always runs with the **dev** profile. It's passed as a CLI argument, so a machine-wide
`SPRING_PROFILES_ACTIVE` won't override it. The dev profile:
- uses a built-in dev JWT key and non-secure cookies (http://localhost)
- connects to `jdbc:postgresql://localhost:5432/fittrack` as `postgres` / `root`
- creates an admin account on first start from `ADMIN_EMAIL` / `ADMIN_PASSWORD` (dev defaults in `application-dev.yml`).
  The bootstrap **only creates** a missing admin; it never changes an existing admin's password.
  Admins sign in straight to the **Admin panel** at `/admin`.
- **logs password-reset emails to the console** instead of sending them. To see real emails, run
  `docker compose --profile mail up -d` and start the backend with `MAIL_ENABLED=true` (MailHog UI: http://localhost:8025).

### Ports already taken?

```powershell
# Defaults: API on 8081 (8080 is often taken, e.g. by Jenkins), web on 4200.
# Backend on another port
$env:SERVER_PORT='8090'; mvn spring-boot:run
# Frontend on another port, pointing its proxy at that backend
$env:FITTRACK_API_PORT='8090'; npx ng serve --port 4400
```

### Use an existing PostgreSQL instead of Docker

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5432/fittrack'; $env:DB_USERNAME='...'; $env:DB_PASSWORD='...'
```
The database must exist, and the user needs permission to `CREATE EXTENSION pg_trgm` (or a superuser can create it once).

## Tests

```bash
cd backend  && mvn test                                   # 60 tests (unit + Testcontainers integration)
cd frontend && npx ng test --watch=false --browsers=ChromeHeadless   # 14 unit tests
```
The integration tests start a throwaway PostgreSQL container, run every Flyway migration, and cover these flows:
register → refresh rotation → token-reuse detection → onboarding (including the unsafe-target rejection) → assessment.
They also check that one user can't read another user's records, and that errors use the standard envelope.
If Docker isn't running, they're skipped automatically.

## Configuration (environment variables)

| Variable | Default | Notes |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5434/fittrack`, `fittrack`, `fittrack` | |
| `JWT_SECRET` | dev key in dev profile; **required** otherwise | Base64, ≥ 256 bits. `openssl rand -base64 48` |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` | `PT15M` / `P7D` | ISO-8601 durations |
| `COOKIE_SECURE` | `true` (`false` in dev) | Must be `true` behind HTTPS |
| `FRONTEND_URL` | `http://localhost:4200` | Used in reset-password links |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Only needed if the SPA is served from a different origin |
| `STORAGE_ROOT` | `./data/uploads` | Profile photos (local disk; S3 adapter later) |
| `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | disabled | SMTP for reset emails |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | dev values in dev profile | Bootstraps the first admin if missing |

## REST API (Phases 1–2)

Base path `/api/v1`. Every response uses the same envelope:

```json
{ "success": true, "message": "Goal created", "data": { }, "timestamp": "2026-11-23T05:50:00Z" }
{ "success": false, "message": "Validation failed", "errors": [{ "field": "email", "message": "must be a well-formed email address" }], "path": "/api/v1/auth/register", "timestamp": "..." }
```

Status codes: `400` validation · `401` not signed in / session expired · `403` forbidden · `404` not found (also returned for
other users' records) · `409` duplicate/conflict · `422` business or safety rule · `500` unexpected (no internals leaked).

### Auth (public unless noted)
| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/auth/register` | `{fullName, email, password, timeZone?}` | 201. Returns access token; sets `ft_refresh` httpOnly cookie |
| POST | `/auth/login` | `{email, password}` | Same response as register |
| POST | `/auth/refresh` | – (cookie) | Rotates the refresh cookie; reusing an old one revokes the whole session family |
| POST | `/auth/logout` | – (cookie) | Revokes the session and clears the cookie |
| POST | `/auth/forgot-password` | `{email}` | Always 200 (doesn't reveal whether the account exists) |
| POST | `/auth/reset-password` | `{token, newPassword}` | Single-use, 30-minute token; signs out all devices |
| PUT | `/auth/change-password` 🔒 | `{currentPassword, newPassword}` | Signs out other devices |
| GET | `/auth/me` 🔒 | – | Current user summary |

Password rule: 8–64 characters with at least one upper-case letter, one lower-case letter and one digit.

### Profile 🔒
| Method | Path | Notes |
|---|---|---|
| GET | `/me/profile` | Includes derived age, latest weight, active target weight, BMI |
| PUT | `/me/profile` | `{fullName, dateOfBirth, gender, heightCm, timeZone, theme}`, age 16–100 |
| PATCH | `/me/profile/theme` | `{theme: LIGHT\|DARK\|SYSTEM}` |
| POST | `/me/profile/photo` | multipart `file` (JPEG/PNG ≤ 5 MB). Re-encoded server-side, metadata stripped |
| GET / DELETE | `/me/profile/photo` | Image bytes / remove |

### Onboarding, assessment & goals 🔒
| Method | Path | Notes |
|---|---|---|
| POST | `/me/onboarding` | Profile + first assessment + starting weight + target-weight goal, all in one transaction |
| GET | `/me/assessment` | Current assessment |
| POST | `/me/assessment` | Re-assessment: stores a **new version**; old versions are kept |
| GET | `/me/assessment/history?page=&size=` | Paged summaries |
| GET | `/me/assessment/{id}` | One of your versions |
| GET | `/equipment` (public) | Equipment reference list |
| GET | `/me/goals?status=` | `ACTIVE`, `ACHIEVED` or `ABANDONED` |
| POST | `/me/goals` | `{goalType, targetValue, title?, targetDate?}`. Replaces any active goal of the same type |
| PUT | `/me/goals/{id}` | Edit an active goal |
| PATCH | `/me/goals/{id}/status` | `{status}` |
| DELETE | `/me/goals/{id}` | |

**Safety rules enforced on the server:** a target weight can't put BMI below 18.5. A weight-loss goal needs a target below
your current weight. Goal targets must be in sensible ranges (e.g. 1–6 workouts a week, 1,000–30,000 steps). Users must be 16 or older.

Full interactive documentation: **http://localhost:8081/swagger-ui.html** (disabled in the prod profile unless `SWAGGER_ENABLED=true`).

## Feature modules

| Module | What it does | Seed data (Flyway, loaded on startup) |
|---|---|---|
| **Recommendations** | BMR (Mifflin-St Jeor), TDEE, safe calorie target (deficit ≤ 500 kcal / 20%, never below BMR), protein range, water, step target, sleep goal, each with an explanation | — |
| **My Workout** | Plan generated from the assessment: training days (beginners ≤ 4, at least 1 rest day), split, exercises filtered by location, equipment, level and declared injuries, sets/reps/rest/effort by goal and age. Session player with set logging, alternatives, finish with effort rating, calorie estimate, personal records, progression tips, skip/reschedule, calendar, history, streak. Permanent exercise swap = Premium (`CUSTOM_WORKOUTS`) | `V10`: 74 exercises with instructions, common mistakes, safety, equipment, contraindications, alternatives |
| **Activity** | Steps (manual entry + `POST /me/integrations/health/import` for a future mobile app), water (+250/+500/custom, per-day goal adjustment, history, safety cap), sleep (bed/wake times in the user's time zone) and recovery advice | — |
| **Nutrition** | Food search filtered by diet, custom foods, 4 meals per day with nutrient snapshots, totals vs targets, insights, suggestions that skip declared allergens | `V11`: 85 Indian & everyday foods (approximate values) |
| **Exercise videos** | Library with category/difficulty/search filters; YouTube/Vimeo embeds (privacy-enhanced), self-hosted S3 videos, thumbnails; admin management (Admin → Videos) | `V12`: 34 library entries with step-by-step instructions (videos are added by an admin - no unlicensed content is shipped) |
| **Reminders** | Wake-up, water (repeat every N minutes), workout, walk, meal, weigh-in, sleep; repeat days; time-zone and DST aware; Spring Scheduler fires them every minute → in-app notifications (+ email channel); toolbar bell and browser notifications while the app is open | — |

Background Web Push (notifications when the app is closed) is not implemented yet; it needs VAPID keys and a service worker.

## AWS S3 storage (images & videos)

Local disk is the default for development. To use S3:

```powershell
$env:STORAGE_TYPE='S3'
$env:AWS_S3_BUCKET='your-fittrack-bucket'
$env:AWS_REGION='ap-south-1'
# Credentials via the standard AWS chain - never in code:
$env:AWS_ACCESS_KEY_ID='...'; $env:AWS_SECRET_ACCESS_KEY='...'   # or an IAM role on EC2/ECS/EKS
# Optional:
$env:AWS_S3_PREFIX='prod/'                    # key prefix
$env:STORAGE_PUBLIC_BASE_URL='https://dxxxx.cloudfront.net'   # serve catalog media via CloudFront
```

- **Keep the bucket private** (Block Public Access on). Catalog media (`catalog/thumbnails`, `catalog/videos`) is served through presigned GET URLs (1-hour expiry) or your CloudFront distribution. Profile photos stay private and are streamed by the API after authentication.
- Objects are written with SSE-S3 (AES-256) encryption.
- **Video uploads go straight from the admin's browser to S3** through a presigned PUT URL (max 500 MB, MP4/WebM), so large files never pass through the API.
- The bucket needs a **CORS rule** so the browser can PUT to it:
  ```json
  [{ "AllowedOrigins": ["https://your-app-domain", "http://localhost:4200"],
     "AllowedMethods": ["PUT", "GET"], "AllowedHeaders": ["Content-Type"], "MaxAgeSeconds": 3000 }]
  ```
- Minimum IAM permissions for the app: `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject` on `arn:aws:s3:::your-fittrack-bucket/*`.
- For LocalStack/MinIO, set `AWS_S3_ENDPOINT` and `AWS_S3_PATH_STYLE=true`.

## SaaS: plans, payments and admin

**Plans** (`subscription_plans` + `plan_features`, editable in Admin → Plans):
- **Free**: core app, up to 3 active goals.
- **Premium**: ₹199/month or ₹1,999/year (starting prices). Unlimited goals, advanced analytics, PDF reports, progress photos,
  custom workouts and data export. Each feature is enforced server-side as later phases ship it.

**Lifecycle:**
- Every user always has exactly one live subscription. New accounts get a **7-day Premium trial** (`BILLING_TRIAL_DAYS`, 0 to disable).
- A scheduled job moves expired trials and paid periods back to Free every 10 minutes.
- Paid plans are bought for a fixed period and **don't auto-renew**. Paying again before the period ends extends it.
- When a user hits a plan limit, the API returns **HTTP 402**, and the app shows an "Upgrade" action.

**Payments with Razorpay** (UPI, cards, net banking, wallets):
1. Create free test keys at https://dashboard.razorpay.com → Settings → API Keys.
2. Set the keys on the backend:
   ```powershell
   $env:RAZORPAY_KEY_ID='rzp_test_...'; $env:RAZORPAY_KEY_SECRET='...'; $env:RAZORPAY_WEBHOOK_SECRET='...'
   ```
3. Add a webhook in the Razorpay dashboard: `https://<your-domain>/api/v1/webhooks/razorpay`, with events `payment.captured`,
   `order.paid` and `payment.failed`, using the same webhook secret. Locally, use a tunnel such as ngrok to test webhooks.

The flow:
- The server creates the order and decides the amount.
- The browser opens Razorpay Checkout.
- The server verifies the HMAC signature before activating the plan.
- The webhook is a backup in case the browser never returns. Activation is idempotent.
- Users must verify their email before paying, so receipts reach them.
- **Without keys, checkout shows "payments not available yet"; admins can still grant Premium manually.**

**Admin panel** (`/admin`, `ROLE_ADMIN` only):
- **Overview:** users, active users, Premium, trials, MRR, 30-day revenue, sign-ups chart.
- **Users:** search and filter, details with subscription and payment history, grant or extend a plan, move to Free, enable or
  disable an account. Disabling signs the user out everywhere.
- **Plans:** names, prices, availability, features and limits.
- **Payments:** all payments, filterable by status.

**Before launching publicly:**
- Have the Terms and Privacy pages (`frontend/src/app/features/public/legal`) reviewed by a lawyer.
- Set `supportEmail` and `companyName` in `environment.prod.ts`.
- Configure real SMTP (`MAIL_ENABLED=true`) so verification and reset emails are actually sent.
- If you run more than one backend instance, move rate limiting to a shared store (it is in-memory per instance).

### SaaS API
| Method | Path | Notes |
|---|---|---|
| GET | `/plans`, `/billing/config` (public) | Prices, features, whether payments are enabled, trial length |
| GET | `/me/subscription` | Current plan, status, days left, features |
| POST | `/me/billing/checkout` | `{planCode, billingCycle: MONTHLY\|YEARLY}` → Razorpay order details |
| POST | `/me/billing/verify` | `{orderId, paymentId, signature}` → activates or extends the plan |
| GET | `/me/billing/payments` | Payment history |
| POST | `/webhooks/razorpay` (public) | Signature-verified webhook |
| POST | `/auth/verify-email` (public), `/auth/resend-verification` | Email verification |
| DELETE | `/me/account` | `{password, confirmation: "DELETE"}`. Permanently deletes the account and all its data |
| GET | `/admin/stats`, `/admin/users`, `/admin/users/{id}`, `/admin/plans`, `/admin/payments` | Admin |
| PATCH/POST/PUT | `/admin/users/{id}/status`, `/admin/users/{id}/grant`, `/admin/users/{id}/revoke`, `/admin/plans/{code}` | Admin actions |

Auth endpoints that accept credentials or send email are limited to 10 requests per 5 minutes per IP (`429` with `Retry-After`).

## Key implementation decisions

- **Tokens:** the access token is short-lived (15 minutes) and kept only in memory. The refresh token is an httpOnly,
  SameSite=Strict cookie scoped to `/api/v1/auth`. Only its SHA-256 hash is stored, it rotates on every use, and reusing an old
  one is detected. An XSS bug can't read either token from storage.
- **Same origin:** the SPA reaches the API through the dev-server proxy (and nginx in production), so the cookie works without CORS.
- **Ownership:** every user-owned query includes the caller's id (`findByIdAndUserId`). Another user's record returns 404,
  so its existence isn't revealed.
- **Assessments are versioned** (one `is_current` row per user, enforced by a partial unique index). Later phases can explain any
  recommendation from the assessment it was based on.
- **Age is derived** from date of birth. **Current weight** lives only in `weight_records`. **Target weight** lives in `fitness_goals`.
  Nothing is stored twice.
- **Time zones:** instants are `TIMESTAMPTZ` in UTC. Each user has an IANA zone, and "today" is computed in that zone.
- **Gender** is used only for the BMR formula (Phase 3), never for choosing exercises.
- **Profile photos** are decoded and re-encoded server-side. This rejects non-images and strips EXIF/GPS data. Files are deleted only after the DB transaction commits.
