-- =====================================================================
-- V8: Reference data (roles, muscle groups, equipment)
-- Exercise, food and video catalogs are seeded in their own phases.
-- =====================================================================

INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');

INSERT INTO muscle_groups (code, name, sort_order) VALUES
    ('CHEST', 'Chest', 1),
    ('BACK', 'Back', 2),
    ('SHOULDERS', 'Shoulders', 3),
    ('BICEPS', 'Biceps', 4),
    ('TRICEPS', 'Triceps', 5),
    ('LEGS', 'Legs', 6),
    ('ABS', 'Abs', 7),
    ('FULL_BODY', 'Full Body', 8),
    ('CARDIO', 'Cardio', 9),
    ('STRETCHING', 'Stretching', 10);

INSERT INTO equipment (code, name, sort_order) VALUES
    ('NONE', 'No equipment (bodyweight)', 1),
    ('YOGA_MAT', 'Yoga mat', 2),
    ('DUMBBELLS', 'Dumbbells', 3),
    ('RESISTANCE_BAND', 'Resistance bands', 4),
    ('KETTLEBELL', 'Kettlebell', 5),
    ('PULL_UP_BAR', 'Pull-up bar', 6),
    ('BENCH', 'Bench', 7),
    ('BARBELL', 'Barbell & plates', 8),
    ('CABLE_MACHINE', 'Cable machine', 9),
    ('MACHINES', 'Gym machines', 10),
    ('TREADMILL', 'Treadmill', 11),
    ('STATIONARY_BIKE', 'Stationary bike', 12),
    ('SKIPPING_ROPE', 'Skipping rope', 13);
