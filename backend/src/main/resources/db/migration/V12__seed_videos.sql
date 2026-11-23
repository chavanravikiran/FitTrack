-- =====================================================================
-- V12: Exercise video library entries.
-- Each entry carries step-by-step instructions. Video files/links are added by an
-- admin (YouTube link or upload to S3) - we only ship content we are licensed to use.
-- =====================================================================

-- One library entry per key exercise, reusing the exercise instructions.
INSERT INTO exercise_videos (exercise_id, muscle_group_id, title, category, difficulty, provider, instructions)
SELECT e.id, e.primary_muscle_group_id, e.name, v.category, e.difficulty, 'YOUTUBE', e.instructions
FROM (VALUES
('push-up','CHEST'),('incline-push-up','CHEST'),('dumbbell-bench-press','CHEST'),('machine-chest-press','CHEST'),
('dumbbell-row','BACK'),('lat-pulldown','BACK'),('band-row','BACK'),
('bodyweight-squat','LEGS'),('glute-bridge','LEGS'),('reverse-lunge','LEGS'),('goblet-squat','LEGS'),('leg-press','LEGS'),
('dumbbell-shoulder-press','SHOULDERS'),('lateral-raise','SHOULDERS'),
('dumbbell-curl','ARMS'),('hammer-curl','ARMS'),('bench-dip','ARMS'),('cable-pushdown','ARMS'),
('plank','ABS'),('dead-bug','ABS'),('bird-dog','ABS'),('side-plank','ABS'),
('brisk-walk','CARDIO'),('jumping-jacks','CARDIO'),('march-in-place','CARDIO'),('stationary-bike','CARDIO'),
('cat-cow','STRETCHING'),('childs-pose','STRETCHING'),('hamstring-stretch','STRETCHING'),('hip-flexor-stretch','STRETCHING'),
('chest-doorway-stretch','STRETCHING')
) AS v(slug, category)
JOIN exercises e ON e.slug = v.slug;

-- Warm-up routines (not tied to a single exercise)
INSERT INTO exercise_videos (title, category, difficulty, provider, instructions)
VALUES
('5-minute Full-body Warm-up', 'WARM_UP', 'BEGINNER', 'YOUTUBE',
 '1. March in place for 60 seconds.
2. Arm circles forward and backward, 30 seconds each.
3. 10 bodyweight squats to a comfortable depth.
4. 10 hip hinges (hands on hips, push hips back).
5. 30 seconds of side steps with arm swings.'),
('Lower-body Warm-up', 'WARM_UP', 'BEGINNER', 'YOUTUBE',
 '1. 60 seconds of marching or easy cycling.
2. 10 leg swings forward and back per leg (hold a wall).
3. 10 glute bridges.
4. 10 reverse lunges, alternating legs.
5. 10 calf raises.'),
('Upper-body Warm-up', 'WARM_UP', 'BEGINNER', 'YOUTUBE',
 '1. 30 seconds of arm circles each direction.
2. 10 band pull-aparts or arm openers.
3. 10 wall or incline push-ups.
4. 10 shoulder rolls.
5. 30 seconds of cat-cow.');
