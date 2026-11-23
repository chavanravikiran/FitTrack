-- =====================================================================
-- V10: Exercise catalog seed (beginner-friendly, home and gym).
-- Text avoids apostrophes on purpose. Equipment lists are "all required";
-- NONE / YOGA_MAT are always treated as available by the plan generator.
-- =====================================================================

ALTER TABLE workout_exercises ADD COLUMN notes VARCHAR(255);

INSERT INTO exercises (name, slug, description, primary_muscle_group_id, difficulty, location, default_sets, default_reps,
                       default_duration_sec, default_rest_sec, instructions, common_mistakes, safety_precautions, met_value)
SELECT v.name, v.slug, v.description, mg.id, v.difficulty, v.location, v.sets, v.reps, v.dur, v.rest,
       v.instructions, v.mistakes, v.safety, v.met
FROM (VALUES
-- ---------------- CHEST ----------------
('Push-up', 'push-up', 'Classic bodyweight press for chest, shoulders and triceps.', 'CHEST', 'BEGINNER', 'BOTH', 3, 10, NULL::int, 60,
 '1. Hands slightly wider than shoulders, body in a straight line from head to heels.
2. Lower your chest until it is just above the floor, elbows at about 45 degrees.
3. Press back up to straight arms while keeping your core tight.',
 'Hips sagging or piking up; elbows flared straight out; half repetitions.',
 'Stop if you feel wrist or shoulder pain. Use the incline or knee version until you can do 10 clean reps.', 3.8),
('Incline Push-up', 'incline-push-up', 'Easier push-up with hands on a table, bench or wall.', 'CHEST', 'BEGINNER', 'BOTH', 3, 12, NULL, 60,
 '1. Place hands on a sturdy raised surface, slightly wider than shoulders.
2. Walk feet back until your body is straight.
3. Lower your chest to the edge and press back up.',
 'Using an unstable surface; letting the lower back arch.',
 'Make sure the surface cannot slide. The higher the surface, the easier the exercise.', 3.5),
('Knee Push-up', 'knee-push-up', 'Push-up from the knees to build strength for full push-ups.', 'CHEST', 'BEGINNER', 'BOTH', 3, 10, NULL, 60,
 '1. Start on hands and knees, then walk hands forward so hips, back and head form a line.
2. Lower your chest toward the floor.
3. Push back up without letting hips bend.',
 'Bending at the hips instead of keeping a straight line.',
 'Place a folded towel under your knees for comfort.', 3.3),
('Dumbbell Floor Press', 'dumbbell-floor-press', 'Chest press lying on the floor; the floor limits shoulder range, making it joint-friendly.', 'CHEST', 'BEGINNER', 'BOTH', 3, 10, NULL, 75,
 '1. Lie on your back, knees bent, a dumbbell in each hand above your chest.
2. Lower until your upper arms lightly touch the floor.
3. Press the weights back up over your chest.',
 'Bouncing elbows off the floor; flaring elbows to 90 degrees.',
 'Start light. Keep wrists straight and stacked over elbows.', 3.5),
('Dumbbell Bench Press', 'dumbbell-bench-press', 'Free-weight chest press on a flat bench.', 'CHEST', 'BEGINNER', 'BOTH', 3, 10, NULL, 90,
 '1. Lie on a flat bench, feet on the floor, dumbbells at chest level.
2. Press the weights up until arms are straight.
3. Lower slowly with control until elbows are slightly below the bench.',
 'Arching the back excessively; lowering too fast.',
 'Use a weight you can control for all reps. Ask for a spotter when going heavier.', 5.0),
('Incline Dumbbell Press', 'incline-dumbbell-press', 'Press on an inclined bench to emphasise the upper chest.', 'CHEST', 'INTERMEDIATE', 'BOTH', 3, 10, NULL, 90,
 '1. Set the bench to 30-45 degrees and sit back with dumbbells at shoulder level.
2. Press up and slightly together.
3. Lower under control.',
 'Bench angle too steep, which turns it into a shoulder press.',
 'Avoid if you have shoulder pain when pressing overhead.', 5.0),
('Barbell Bench Press', 'barbell-bench-press', 'Compound barbell press for chest strength.', 'CHEST', 'INTERMEDIATE', 'GYM', 3, 8, NULL, 120,
 '1. Lie under the bar, eyes below it, grip slightly wider than shoulders.
2. Unrack, lower the bar to mid-chest.
3. Press up until arms are straight.',
 'Bouncing the bar off the chest; lifting hips off the bench.',
 'Always use safety pins or a spotter. Learn the movement with an empty bar first.', 6.0),
('Machine Chest Press', 'machine-chest-press', 'Guided chest press, ideal for learning the pressing pattern.', 'CHEST', 'BEGINNER', 'GYM', 3, 12, NULL, 75,
 '1. Adjust the seat so handles are at mid-chest height.
2. Press forward until arms are nearly straight.
3. Return slowly.',
 'Seat too high or low; locking elbows hard.',
 'Keep your back against the pad throughout.', 4.0),
('Band Chest Press', 'band-chest-press', 'Standing chest press with a resistance band anchored behind you.', 'CHEST', 'BEGINNER', 'BOTH', 3, 12, NULL, 60,
 '1. Anchor the band at chest height behind you and hold a handle in each hand.
2. Step forward to create tension and press hands forward.
3. Return slowly.',
 'Letting the band snap back; shrugging shoulders.',
 'Check the band and anchor for damage before every session.', 3.5),
('Cable Fly', 'cable-fly', 'Chest isolation with cables in a hugging motion.', 'CHEST', 'INTERMEDIATE', 'GYM', 3, 12, NULL, 60,
 '1. Set pulleys at shoulder height and step forward with a handle in each hand.
2. With a slight bend in the elbows, bring hands together in front of the chest.
3. Open the arms slowly.',
 'Bending elbows too much so it becomes a press; using too much weight.',
 'Keep the range comfortable; stop if you feel shoulder pinching.', 3.5),
-- ---------------- BACK ----------------
('One-arm Dumbbell Row', 'dumbbell-row', 'Rowing movement for the upper back and lats.', 'BACK', 'BEGINNER', 'BOTH', 3, 10, NULL, 75,
 '1. Place one hand and knee on a bench or sturdy chair, back flat.
2. Pull the dumbbell toward your hip, leading with the elbow.
3. Lower until the arm is straight. Switch sides.',
 'Rounding the back; twisting the torso to swing the weight.',
 'Keep your back flat and neutral. Use a lighter weight if your lower back feels strained.', 4.0),
('Band Row', 'band-row', 'Seated or standing row with a resistance band.', 'BACK', 'BEGINNER', 'BOTH', 3, 12, NULL, 60,
 '1. Loop the band around your feet (seated) or a fixed anchor (standing).
2. Pull the handles toward your lower ribs, squeezing shoulder blades together.
3. Return with control.',
 'Leaning back to finish the pull; shrugging.',
 'Secure the band so it cannot slip.', 3.5),
('Lat Pulldown', 'lat-pulldown', 'Machine pull-down that builds the back muscles used in pull-ups.', 'BACK', 'BEGINNER', 'GYM', 3, 12, NULL, 75,
 '1. Sit with thighs under the pads and grip the bar wider than shoulders.
2. Pull the bar to your upper chest, elbows down and back.
3. Let the bar rise slowly.',
 'Pulling the bar behind the neck; leaning far back.',
 'Always pull to the front of the body.', 4.0),
('Seated Cable Row', 'seated-cable-row', 'Cable row for the middle back.', 'BACK', 'BEGINNER', 'GYM', 3, 12, NULL, 75,
 '1. Sit tall with knees slightly bent, holding the handle.
2. Pull it to your stomach, squeezing shoulder blades.
3. Extend arms slowly without rounding the back.',
 'Rocking the torso back and forth.',
 'Keep the chest up and spine neutral.', 4.0),
('Pull-up', 'pull-up', 'Bodyweight pull for back and biceps.', 'BACK', 'ADVANCED', 'BOTH', 3, 6, NULL, 120,
 '1. Hang from the bar with hands slightly wider than shoulders.
2. Pull until your chin is over the bar.
3. Lower all the way down with control.',
 'Kipping or swinging; partial reps.',
 'Build up with lat pulldowns or band rows first. Avoid with elbow or shoulder pain.', 8.0),
('Barbell Row', 'barbell-row', 'Bent-over barbell row for back thickness.', 'BACK', 'INTERMEDIATE', 'GYM', 3, 8, NULL, 90,
 '1. Hinge at the hips with a flat back, bar hanging at arm length.
2. Row the bar to your lower ribs.
3. Lower with control.',
 'Rounding the lower back; standing up during the row.',
 'Avoid if you have lower back pain. Keep the load moderate.', 5.0),
('Superman Hold', 'superman', 'Floor exercise for the lower back and glutes.', 'BACK', 'BEGINNER', 'BOTH', 3, 12, NULL, 45,
 '1. Lie face down with arms extended in front.
2. Lift arms, chest and legs slightly off the floor.
3. Hold for 2 seconds and lower.',
 'Cranking the neck up; lifting too high.',
 'Keep the movement small. Skip it if you have lower back pain.', 3.0),
('Band Pull-apart', 'band-pull-apart', 'Light band exercise for upper back and posture.', 'BACK', 'BEGINNER', 'BOTH', 3, 15, NULL, 45,
 '1. Hold a band in front of you at shoulder height with straight arms.
2. Pull the band apart until it touches your chest.
3. Return slowly.',
 'Bending the elbows; arching the back.',
 'Use a light band; this should feel easy on the joints.', 2.8),
-- ---------------- SHOULDERS ----------------
('Dumbbell Shoulder Press', 'dumbbell-shoulder-press', 'Overhead press for the shoulders.', 'SHOULDERS', 'BEGINNER', 'BOTH', 3, 10, NULL, 90,
 '1. Sit or stand tall with dumbbells at shoulder height, palms forward.
2. Press overhead until arms are straight.
3. Lower back to shoulder height.',
 'Arching the lower back; pressing the weights forward instead of up.',
 'Avoid overhead pressing with shoulder or neck pain. Brace your core.', 4.5),
('Machine Shoulder Press', 'machine-shoulder-press', 'Guided overhead press.', 'SHOULDERS', 'BEGINNER', 'GYM', 3, 12, NULL, 75,
 '1. Adjust the seat so handles start at shoulder height.
2. Press up until arms are nearly straight.
3. Lower slowly.',
 'Seat too low; locking elbows.',
 'Keep your back against the pad.', 4.0),
('Pike Push-up', 'pike-push-up', 'Bodyweight shoulder press with hips raised.', 'SHOULDERS', 'INTERMEDIATE', 'BOTH', 3, 8, NULL, 75,
 '1. Start in a downward-dog position with hips high.
2. Bend elbows to lower your head toward the floor.
3. Press back up.',
 'Letting hips drop; elbows flaring wide.',
 'Avoid with wrist, neck or shoulder issues.', 4.0),
('Dumbbell Lateral Raise', 'lateral-raise', 'Isolation for the side shoulders.', 'SHOULDERS', 'BEGINNER', 'BOTH', 3, 12, NULL, 60,
 '1. Stand with light dumbbells by your sides.
2. Raise arms out to the sides to shoulder height with a slight elbow bend.
3. Lower slowly.',
 'Swinging the weights; lifting above shoulder height.',
 'Use light weights. Stop if you feel pinching in the shoulder.', 3.0),
('Band Lateral Raise', 'band-lateral-raise', 'Lateral raise with a band under the feet.', 'SHOULDERS', 'BEGINNER', 'BOTH', 3, 15, NULL, 45,
 '1. Stand on the band and hold the ends.
2. Raise arms out to shoulder height.
3. Lower with control.',
 'Shrugging the shoulders up.',
 'Choose a light band.', 3.0),
('Face Pull', 'face-pull', 'Cable exercise for rear shoulders and posture.', 'SHOULDERS', 'BEGINNER', 'GYM', 3, 15, NULL, 60,
 '1. Set a rope at face height.
2. Pull the rope toward your face, elbows high, hands apart.
3. Return slowly.',
 'Using too much weight and leaning back.',
 'Keep it light and controlled.', 3.0),
-- ---------------- BICEPS ----------------
('Dumbbell Biceps Curl', 'dumbbell-curl', 'Basic curl for the biceps.', 'BICEPS', 'BEGINNER', 'BOTH', 3, 12, NULL, 60,
 '1. Stand with dumbbells at your sides, palms forward.
2. Curl the weights toward your shoulders keeping elbows still.
3. Lower slowly.',
 'Swinging the body; elbows drifting forward.',
 'Keep wrists straight.', 3.0),
('Hammer Curl', 'hammer-curl', 'Neutral-grip curl for biceps and forearms.', 'BICEPS', 'BEGINNER', 'BOTH', 3, 12, NULL, 60,
 '1. Hold dumbbells with palms facing each other.
2. Curl up keeping elbows at your sides.
3. Lower with control.',
 'Using momentum.',
 'A wrist-friendly curl variation.', 3.0),
('Band Biceps Curl', 'band-curl', 'Curl using a resistance band under the feet.', 'BICEPS', 'BEGINNER', 'BOTH', 3, 15, NULL, 45,
 '1. Stand on the band holding the handles.
2. Curl hands toward shoulders.
3. Lower slowly.',
 'Letting elbows move forward.',
 'Check the band for tears.', 2.8),
('Barbell Curl', 'barbell-curl', 'Two-handed curl with a barbell.', 'BICEPS', 'BEGINNER', 'GYM', 3, 10, NULL, 75,
 '1. Hold the bar with palms up, hands shoulder-width.
2. Curl to the chest.
3. Lower all the way.',
 'Leaning back to lift the bar.',
 'Use an EZ bar if straight bars hurt your wrists.', 3.5),
('Cable Curl', 'cable-curl', 'Curl with constant cable tension.', 'BICEPS', 'BEGINNER', 'GYM', 3, 12, NULL, 60,
 '1. Attach a bar to a low pulley.
2. Curl up keeping elbows fixed.
3. Lower slowly.',
 'Rocking the body.',
 'Control the return.', 3.0),
-- ---------------- TRICEPS ----------------
('Bench Dip', 'bench-dip', 'Triceps dip using a chair or bench.', 'TRICEPS', 'INTERMEDIATE', 'BOTH', 3, 10, NULL, 60,
 '1. Sit on the edge of a sturdy chair, hands beside hips.
2. Slide off and lower your body by bending elbows to about 90 degrees.
3. Press back up.',
 'Going too deep; shoulders rolling forward.',
 'Avoid with shoulder or wrist pain. Use a chair that cannot slide.', 3.8),
('Overhead Dumbbell Extension', 'overhead-dumbbell-extension', 'Triceps extension holding one dumbbell overhead.', 'TRICEPS', 'BEGINNER', 'BOTH', 3, 12, NULL, 60,
 '1. Hold one dumbbell with both hands overhead.
2. Lower it behind your head by bending elbows.
3. Extend back up.',
 'Elbows flaring wide; arching the back.',
 'Avoid with elbow or shoulder pain; try band pushdowns instead.', 3.0),
('Cable Triceps Pushdown', 'cable-pushdown', 'Triceps pushdown on a high pulley.', 'TRICEPS', 'BEGINNER', 'GYM', 3, 12, NULL, 60,
 '1. Hold the bar or rope at chest height, elbows at your sides.
2. Push down until arms are straight.
3. Let it rise slowly.',
 'Elbows moving forward; leaning over the bar.',
 'Keep elbows pinned to your sides.', 3.0),
('Band Triceps Pushdown', 'band-pushdown', 'Pushdown with a band anchored above.', 'TRICEPS', 'BEGINNER', 'BOTH', 3, 15, NULL, 45,
 '1. Anchor the band above head height.
2. Push down to straight arms keeping elbows still.
3. Return slowly.',
 'Shrugging shoulders.',
 'Make sure the anchor is secure.', 2.8),
('Diamond Push-up', 'diamond-push-up', 'Close-hand push-up emphasising the triceps.', 'TRICEPS', 'INTERMEDIATE', 'BOTH', 3, 8, NULL, 75,
 '1. Place hands close together under your chest.
2. Lower your chest to your hands.
3. Press back up.',
 'Hips sagging; elbows flaring.',
 'Hard on the wrists; use bench dips or band pushdowns if uncomfortable.', 4.0),
-- ---------------- LEGS ----------------
('Bodyweight Squat', 'bodyweight-squat', 'Fundamental lower-body movement.', 'LEGS', 'BEGINNER', 'BOTH', 3, 15, NULL, 60,
 '1. Stand with feet shoulder-width apart, toes slightly out.
2. Sit back and down as if onto a chair, chest up.
3. Push through your whole foot to stand.',
 'Knees caving inward; heels lifting; rounding the back.',
 'Squat only as low as is pain-free. Use a chair behind you as a depth guide.', 5.0),
('Glute Bridge', 'glute-bridge', 'Hip lift that strengthens glutes and hamstrings with little knee stress.', 'LEGS', 'BEGINNER', 'BOTH', 3, 15, NULL, 45,
 '1. Lie on your back with knees bent and feet flat.
2. Squeeze your glutes and lift hips until body is straight from knees to shoulders.
3. Lower slowly.',
 'Arching the lower back at the top; pushing through toes.',
 'A good option for knee or back sensitivity. Keep the movement smooth.', 3.0),
('Wall Sit', 'wall-sit', 'Isometric hold for the thighs.', 'LEGS', 'BEGINNER', 'BOTH', 3, NULL, 30, 45,
 '1. Lean your back against a wall and slide down until knees are bent (up to 90 degrees).
2. Hold the position, breathing steadily.
3. Slide back up.',
 'Going lower than is comfortable; holding breath.',
 'Use a shallower angle if knees feel strained.', 3.5),
('Reverse Lunge', 'reverse-lunge', 'Lunge stepping backward; easier on the knees than forward lunges.', 'LEGS', 'BEGINNER', 'BOTH', 3, 10, NULL, 60,
 '1. Stand tall, step one foot back.
2. Lower until both knees are bent about 90 degrees.
3. Push through the front heel to return. Alternate legs.',
 'Front knee collapsing inward; leaning forward.',
 'Hold a wall or chair for balance. Avoid with knee pain.', 4.0),
('Goblet Squat', 'goblet-squat', 'Squat holding a dumbbell at the chest; teaches good posture.', 'LEGS', 'BEGINNER', 'BOTH', 3, 12, NULL, 75,
 '1. Hold one dumbbell vertically at your chest.
2. Squat down keeping elbows inside your knees.
3. Stand back up.',
 'Rounding the back; heels lifting.',
 'Squat to a pain-free depth.', 5.0),
('Dumbbell Romanian Deadlift', 'dumbbell-romanian-deadlift', 'Hip hinge for hamstrings and glutes.', 'LEGS', 'INTERMEDIATE', 'BOTH', 3, 10, NULL, 90,
 '1. Stand holding dumbbells in front of your thighs.
2. Push hips back and lower the weights along your legs with a flat back.
3. Stand up by squeezing your glutes.',
 'Rounding the back; bending the knees too much.',
 'Avoid with lower back pain. Start light and learn the hinge.', 5.0),
('Leg Press', 'leg-press', 'Machine press for the whole leg.', 'LEGS', 'BEGINNER', 'GYM', 3, 12, NULL, 90,
 '1. Sit with feet shoulder-width on the platform.
2. Lower the platform until knees are about 90 degrees.
3. Press back up without locking knees.',
 'Lowering so deep the hips lift off the seat; locking knees.',
 'Keep your lower back on the pad.', 5.0),
('Lying Leg Curl', 'leg-curl-machine', 'Machine curl for the hamstrings.', 'LEGS', 'BEGINNER', 'GYM', 3, 12, NULL, 60,
 '1. Lie face down with the pad above your heels.
2. Curl heels toward your glutes.
3. Lower slowly.',
 'Lifting hips off the pad.',
 'Use a moderate weight.', 3.5),
('Leg Extension', 'leg-extension', 'Machine isolation for the quadriceps.', 'LEGS', 'BEGINNER', 'GYM', 3, 12, NULL, 60,
 '1. Sit with the pad on your lower shins.
2. Straighten your legs.
3. Lower slowly.',
 'Kicking up fast; heavy weights.',
 'Avoid or keep very light with knee problems.', 3.5),
('Barbell Back Squat', 'barbell-back-squat', 'Compound barbell squat for strength.', 'LEGS', 'INTERMEDIATE', 'GYM', 3, 8, NULL, 120,
 '1. Set the bar on your upper back, feet shoulder-width.
2. Squat down with chest up and knees tracking over toes.
3. Drive up.',
 'Rounding the back; knees caving.',
 'Use safety bars in a rack. Learn with a light bar or goblet squats first.', 6.0),
('Step-up', 'step-up', 'Stepping onto a stable step or bench.', 'LEGS', 'BEGINNER', 'BOTH', 3, 10, NULL, 60,
 '1. Place one foot fully on a stable step.
2. Step up by pushing through that heel.
3. Step down with control. Alternate legs.',
 'Pushing off the back foot; using a high, unstable step.',
 'Use a low step at first. Avoid with knee pain.', 4.5),
('Calf Raise', 'calf-raise', 'Strengthens the calves and ankles.', 'LEGS', 'BEGINNER', 'BOTH', 3, 15, NULL, 45,
 '1. Stand with feet hip-width, holding a wall for balance.
2. Rise onto the balls of your feet.
3. Lower slowly.',
 'Bouncing; rolling onto the outer foot.',
 'Avoid with ankle injury.', 2.8),
('Clamshell', 'clamshell', 'Side-lying exercise for the outer hips and glutes.', 'LEGS', 'BEGINNER', 'BOTH', 3, 15, NULL, 30,
 '1. Lie on your side with knees bent and feet together.
2. Lift the top knee while keeping feet touching.
3. Lower slowly. Switch sides.',
 'Rolling the hips backward.',
 'Gentle on knees and back.', 2.5),
-- ---------------- ABS / CORE ----------------
('Plank', 'plank', 'Core hold that builds trunk stability.', 'ABS', 'BEGINNER', 'BOTH', 3, NULL, 30, 45,
 '1. Rest on forearms and toes, elbows under shoulders.
2. Keep a straight line from head to heels and squeeze your glutes.
3. Hold while breathing normally.',
 'Hips sagging or too high; holding breath.',
 'Start with 15-20 seconds; drop to knees if needed.', 3.0),
('Dead Bug', 'dead-bug', 'Back-friendly core exercise lying on your back.', 'ABS', 'BEGINNER', 'BOTH', 3, 10, NULL, 45,
 '1. Lie on your back, arms up, knees bent at 90 degrees above hips.
2. Lower one arm and the opposite leg toward the floor while keeping your lower back down.
3. Return and switch sides.',
 'Lower back arching off the floor; moving too fast.',
 'One of the safest core exercises for people with back sensitivity.', 2.8),
('Bird Dog', 'bird-dog', 'Core and back stability on hands and knees.', 'ABS', 'BEGINNER', 'BOTH', 3, 10, NULL, 45,
 '1. Start on hands and knees.
2. Extend one arm forward and the opposite leg back.
3. Hold briefly, return and switch.',
 'Twisting the hips; arching the back.',
 'Move slowly and keep the spine still.', 2.8),
('Crunch', 'crunch', 'Basic abdominal curl.', 'ABS', 'BEGINNER', 'BOTH', 3, 15, NULL, 45,
 '1. Lie on your back with knees bent, hands lightly behind your head.
2. Curl your shoulders off the floor using your abs.
3. Lower slowly.',
 'Pulling on the neck; sitting all the way up.',
 'Avoid with neck or lower back pain; try dead bugs instead.', 2.8),
('Bicycle Crunch', 'bicycle-crunch', 'Rotating crunch for the obliques.', 'ABS', 'INTERMEDIATE', 'BOTH', 3, 16, NULL, 45,
 '1. Lie on your back, hands lightly behind head, legs lifted.
2. Bring one elbow toward the opposite knee while extending the other leg.
3. Alternate sides slowly.',
 'Yanking the neck; going too fast.',
 'Avoid with neck or back pain.', 3.5),
('Side Plank', 'side-plank', 'Side hold for the obliques.', 'ABS', 'BEGINNER', 'BOTH', 3, NULL, 20, 30,
 '1. Lie on your side, forearm under shoulder.
2. Lift hips so your body forms a straight line.
3. Hold, then switch sides.',
 'Hips dropping; shoulder shrugging.',
 'Bend the bottom knee to make it easier. Avoid with shoulder pain.', 3.0),
('Lying Leg Raise', 'leg-raise', 'Lower-ab exercise lifting the legs.', 'ABS', 'INTERMEDIATE', 'BOTH', 3, 10, NULL, 45,
 '1. Lie on your back with legs straight and hands under hips.
2. Raise legs to vertical.
3. Lower slowly without arching.',
 'Lower back lifting off the floor.',
 'Bend knees to make it easier. Avoid with lower back pain.', 3.0),
('Russian Twist', 'russian-twist', 'Seated rotation for the obliques.', 'ABS', 'INTERMEDIATE', 'BOTH', 3, 16, NULL, 45,
 '1. Sit leaning back slightly, knees bent, feet on the floor.
2. Rotate your torso to tap hands beside each hip.
3. Keep the chest lifted.',
 'Rounding the back; twisting only the arms.',
 'Keep feet down. Avoid with lower back pain.', 3.0),
-- ---------------- FULL BODY ----------------
('Burpee', 'burpee', 'High-intensity full-body movement.', 'FULL_BODY', 'INTERMEDIATE', 'BOTH', 3, 8, NULL, 60,
 '1. Squat down and place hands on the floor.
2. Step or jump feet back to a plank, then return.
3. Stand up (add a small jump if comfortable).',
 'Sagging hips in the plank; landing hard.',
 'Step instead of jumping to reduce impact. Avoid with knee, wrist or back pain.', 8.0),
('Dumbbell Thruster', 'dumbbell-thruster', 'Squat into an overhead press.', 'FULL_BODY', 'INTERMEDIATE', 'BOTH', 3, 10, NULL, 90,
 '1. Hold dumbbells at shoulders and squat down.
2. Drive up and press the weights overhead in one motion.
3. Lower to shoulders and repeat.',
 'Pressing before standing; arching the back.',
 'Use light weights. Avoid with knee or shoulder pain.', 6.0),
('Kettlebell Swing', 'kettlebell-swing', 'Explosive hip hinge for glutes, hamstrings and conditioning.', 'FULL_BODY', 'INTERMEDIATE', 'BOTH', 3, 15, NULL, 60,
 '1. Hinge at the hips and hike the kettlebell between your legs.
2. Snap your hips forward to swing it to chest height.
3. Let it fall back and hinge again.',
 'Squatting instead of hinging; lifting with the arms.',
 'Learn the hip hinge first. Avoid with lower back pain.', 7.0),
-- ---------------- CARDIO ----------------
('Brisk Walk', 'brisk-walk', 'Low-impact cardio you can do anywhere.', 'CARDIO', 'BEGINNER', 'BOTH', 1, NULL, 600, 0,
 '1. Walk at a pace where you can talk but not sing.
2. Swing your arms naturally and stand tall.
3. Keep it steady for the full duration.',
 'Going too fast too soon.',
 'Wear supportive shoes. Stay hydrated.', 4.3),
('Incline Treadmill Walk', 'treadmill-walk-incline', 'Walking on an incline for low-impact conditioning.', 'CARDIO', 'BEGINNER', 'BOTH', 1, NULL, 900, 0,
 '1. Start at a comfortable speed with a small incline.
2. Increase incline gradually.
3. Avoid holding the handrails.',
 'Holding the rails tightly; too steep too soon.',
 'Use the safety clip. Lower the incline if calves or back feel strained.', 5.3),
('Stationary Bike', 'stationary-bike', 'Low-impact cycling.', 'CARDIO', 'BEGINNER', 'BOTH', 1, NULL, 900, 0,
 '1. Adjust the seat so your knee is slightly bent at the bottom.
2. Pedal at a steady, comfortable pace.
3. Increase resistance gradually.',
 'Seat too low causing knee strain.',
 'A good joint-friendly option.', 6.8),
('Jumping Jacks', 'jumping-jacks', 'Classic warm-up and cardio move.', 'CARDIO', 'BEGINNER', 'BOTH', 3, NULL, 45, 30,
 '1. Jump feet out while raising arms overhead.
2. Jump back to the start.
3. Keep a steady rhythm.',
 'Landing flat-footed and hard.',
 'Step side-to-side instead of jumping for a low-impact version.', 8.0),
('March in Place', 'march-in-place', 'Gentle, low-impact cardio.', 'CARDIO', 'BEGINNER', 'BOTH', 3, NULL, 60, 20,
 '1. Stand tall and march, lifting knees to a comfortable height.
2. Swing your arms.
3. Increase pace to raise your heart rate.',
 'Slouching.',
 'Suitable for most fitness levels and joint conditions.', 3.5),
('High Knees', 'high-knees', 'Fast running in place with knees lifted.', 'CARDIO', 'INTERMEDIATE', 'BOTH', 3, NULL, 30, 30,
 '1. Run in place driving knees toward hip height.
2. Stay on the balls of your feet.
3. Pump your arms.',
 'Leaning back; landing heavily.',
 'Switch to marching if you have knee or ankle issues.', 8.0),
('Skipping Rope', 'skipping-rope', 'Jump rope intervals for conditioning.', 'CARDIO', 'INTERMEDIATE', 'BOTH', 3, NULL, 60, 45,
 '1. Hold handles at hip height and turn the rope with your wrists.
2. Make small jumps on the balls of your feet.
3. Keep a relaxed rhythm.',
 'Jumping too high; using the arms instead of wrists.',
 'High impact; avoid with knee or ankle issues.', 11.0),
('Mountain Climber', 'mountain-climber', 'Plank position running drill.', 'CARDIO', 'INTERMEDIATE', 'BOTH', 3, NULL, 30, 30,
 '1. Start in a high plank.
2. Drive one knee toward your chest, then switch quickly.
3. Keep hips level.',
 'Hips bouncing up; shoulders behind hands.',
 'Slow it down to a step if wrists or shoulders feel strained.', 8.0),
-- ---------------- STRETCHING ----------------
('Cat-Cow Stretch', 'cat-cow', 'Gentle spinal mobility on hands and knees.', 'STRETCHING', 'BEGINNER', 'BOTH', 1, NULL, 60, 0,
 '1. On hands and knees, arch your back while lifting your head (cow).
2. Round your back while tucking your chin (cat).
3. Move slowly with your breath.',
 'Forcing the range.',
 'Move only within a comfortable range.', 2.3),
('Child Pose', 'childs-pose', 'Relaxing stretch for the back and hips.', 'STRETCHING', 'BEGINNER', 'BOTH', 1, NULL, 45, 0,
 '1. Kneel and sit back on your heels.
2. Fold forward and reach arms ahead.
3. Breathe deeply.',
 'Forcing hips down.',
 'Place a pillow under your hips or knees if needed.', 2.3),
('Seated Hamstring Stretch', 'hamstring-stretch', 'Stretch for the back of the thighs.', 'STRETCHING', 'BEGINNER', 'BOTH', 1, NULL, 45, 0,
 '1. Sit with one leg straight and the other bent.
2. Hinge forward from the hips toward the straight leg.
3. Hold, then switch.',
 'Rounding the back and bouncing.',
 'Stretch to mild tension, never pain.', 2.3),
('Kneeling Hip Flexor Stretch', 'hip-flexor-stretch', 'Opens the front of the hips, helpful after long sitting.', 'STRETCHING', 'BEGINNER', 'BOTH', 1, NULL, 45, 0,
 '1. Kneel on one knee with the other foot in front.
2. Gently shift your hips forward.
3. Hold, then switch sides.',
 'Arching the lower back.',
 'Pad the knee. Do it standing if kneeling hurts.', 2.3),
('Doorway Chest Stretch', 'chest-doorway-stretch', 'Stretch for the chest and front shoulders.', 'STRETCHING', 'BEGINNER', 'BOTH', 1, NULL, 30, 0,
 '1. Place forearms on a door frame at shoulder height.
2. Step forward gently.
3. Hold.',
 'Stepping too far.',
 'Avoid if it causes shoulder pain.', 2.3),
('Standing Quad Stretch', 'quad-stretch', 'Stretch for the front of the thighs.', 'STRETCHING', 'BEGINNER', 'BOTH', 1, NULL, 30, 0,
 '1. Hold a wall for balance.
2. Bend one knee and hold your ankle behind you.
3. Keep knees together; hold, then switch.',
 'Pulling the knee out to the side.',
 'Use a strap if you cannot reach your ankle.', 2.3),
('Cobra Stretch', 'cobra-stretch', 'Gentle back extension and abdominal stretch.', 'STRETCHING', 'BEGINNER', 'BOTH', 1, NULL, 30, 0,
 '1. Lie face down with hands under shoulders.
2. Press up gently, keeping hips down.
3. Hold, then lower.',
 'Pushing too high; shrugging.',
 'Keep it gentle. Skip with lower back pain.', 2.3),
('Cross-body Shoulder Stretch', 'shoulder-stretch', 'Stretch for the back of the shoulder.', 'STRETCHING', 'BEGINNER', 'BOTH', 1, NULL, 30, 0,
 '1. Bring one arm across your chest.
2. Gently pull it closer with the other arm.
3. Hold, then switch.',
 'Rotating the torso.',
 'Mild tension only.', 2.3)
) AS v(name, slug, description, mg, difficulty, location, sets, reps, dur, rest, instructions, mistakes, safety, met)
JOIN muscle_groups mg ON mg.code = v.mg;

-- Required equipment (all listed items are needed). Bodyweight moves use NONE.
INSERT INTO exercise_equipment (exercise_id, equipment_id)
SELECT e.id, q.id
FROM (VALUES
('push-up','NONE'),('incline-push-up','NONE'),('knee-push-up','NONE'),('dumbbell-floor-press','DUMBBELLS'),
('dumbbell-bench-press','DUMBBELLS'),('dumbbell-bench-press','BENCH'),('incline-dumbbell-press','DUMBBELLS'),('incline-dumbbell-press','BENCH'),
('barbell-bench-press','BARBELL'),('barbell-bench-press','BENCH'),('machine-chest-press','MACHINES'),('band-chest-press','RESISTANCE_BAND'),
('cable-fly','CABLE_MACHINE'),('dumbbell-row','DUMBBELLS'),('band-row','RESISTANCE_BAND'),('lat-pulldown','MACHINES'),
('seated-cable-row','CABLE_MACHINE'),('pull-up','PULL_UP_BAR'),('barbell-row','BARBELL'),('superman','YOGA_MAT'),
('band-pull-apart','RESISTANCE_BAND'),('dumbbell-shoulder-press','DUMBBELLS'),('machine-shoulder-press','MACHINES'),('pike-push-up','NONE'),
('lateral-raise','DUMBBELLS'),('band-lateral-raise','RESISTANCE_BAND'),('face-pull','CABLE_MACHINE'),('dumbbell-curl','DUMBBELLS'),
('hammer-curl','DUMBBELLS'),('band-curl','RESISTANCE_BAND'),('barbell-curl','BARBELL'),('cable-curl','CABLE_MACHINE'),
('bench-dip','NONE'),('overhead-dumbbell-extension','DUMBBELLS'),('cable-pushdown','CABLE_MACHINE'),('band-pushdown','RESISTANCE_BAND'),
('diamond-push-up','NONE'),('bodyweight-squat','NONE'),('glute-bridge','YOGA_MAT'),('wall-sit','NONE'),('reverse-lunge','NONE'),
('goblet-squat','DUMBBELLS'),('dumbbell-romanian-deadlift','DUMBBELLS'),('leg-press','MACHINES'),('leg-curl-machine','MACHINES'),
('leg-extension','MACHINES'),('barbell-back-squat','BARBELL'),('step-up','NONE'),('calf-raise','NONE'),('clamshell','YOGA_MAT'),
('plank','YOGA_MAT'),('dead-bug','YOGA_MAT'),('bird-dog','YOGA_MAT'),('crunch','YOGA_MAT'),('bicycle-crunch','YOGA_MAT'),
('side-plank','YOGA_MAT'),('leg-raise','YOGA_MAT'),('russian-twist','YOGA_MAT'),('burpee','NONE'),('dumbbell-thruster','DUMBBELLS'),
('kettlebell-swing','KETTLEBELL'),('brisk-walk','NONE'),('treadmill-walk-incline','TREADMILL'),('stationary-bike','STATIONARY_BIKE'),
('jumping-jacks','NONE'),('march-in-place','NONE'),('high-knees','NONE'),('skipping-rope','SKIPPING_ROPE'),('mountain-climber','NONE'),
('cat-cow','YOGA_MAT'),('childs-pose','YOGA_MAT'),('hamstring-stretch','YOGA_MAT'),('hip-flexor-stretch','YOGA_MAT'),
('chest-doorway-stretch','NONE'),('quad-stretch','NONE'),('cobra-stretch','YOGA_MAT'),('shoulder-stretch','NONE')
) AS v(slug, code)
JOIN exercises e ON e.slug = v.slug
JOIN equipment q ON q.code = v.code;

-- Secondary muscles
INSERT INTO exercise_secondary_muscles (exercise_id, muscle_group_id)
SELECT e.id, m.id
FROM (VALUES
('push-up','TRICEPS'),('push-up','SHOULDERS'),('incline-push-up','TRICEPS'),('knee-push-up','TRICEPS'),
('dumbbell-floor-press','TRICEPS'),('dumbbell-bench-press','TRICEPS'),('dumbbell-bench-press','SHOULDERS'),
('incline-dumbbell-press','SHOULDERS'),('barbell-bench-press','TRICEPS'),('machine-chest-press','TRICEPS'),
('band-chest-press','TRICEPS'),('dumbbell-row','BICEPS'),('band-row','BICEPS'),('lat-pulldown','BICEPS'),
('seated-cable-row','BICEPS'),('pull-up','BICEPS'),('barbell-row','BICEPS'),('band-pull-apart','SHOULDERS'),
('dumbbell-shoulder-press','TRICEPS'),('pike-push-up','TRICEPS'),('face-pull','BACK'),('hammer-curl','BICEPS'),
('diamond-push-up','CHEST'),('bench-dip','SHOULDERS'),('goblet-squat','ABS'),('bodyweight-squat','ABS'),
('dumbbell-romanian-deadlift','BACK'),('barbell-back-squat','ABS'),('plank','SHOULDERS'),('burpee','CARDIO'),
('dumbbell-thruster','SHOULDERS'),('dumbbell-thruster','LEGS'),('kettlebell-swing','LEGS'),('kettlebell-swing','BACK'),
('mountain-climber','ABS'),('superman','LEGS')
) AS v(slug, code)
JOIN exercises e ON e.slug = v.slug
JOIN muscle_groups m ON m.code = v.code;

-- Body areas that should avoid each exercise
INSERT INTO exercise_contraindications (exercise_id, body_area)
SELECT e.id, v.area
FROM (VALUES
('push-up','WRIST'),('push-up','SHOULDER'),('incline-push-up','WRIST'),('knee-push-up','WRIST'),
('dumbbell-bench-press','SHOULDER'),('incline-dumbbell-press','SHOULDER'),('barbell-bench-press','SHOULDER'),
('cable-fly','SHOULDER'),('pull-up','SHOULDER'),('pull-up','ELBOW'),('barbell-row','LOWER_BACK'),('superman','LOWER_BACK'),
('dumbbell-shoulder-press','SHOULDER'),('dumbbell-shoulder-press','NECK'),('machine-shoulder-press','SHOULDER'),
('pike-push-up','SHOULDER'),('pike-push-up','WRIST'),('pike-push-up','NECK'),('lateral-raise','SHOULDER'),
('barbell-curl','WRIST'),('bench-dip','SHOULDER'),('bench-dip','WRIST'),('overhead-dumbbell-extension','ELBOW'),
('overhead-dumbbell-extension','SHOULDER'),('diamond-push-up','WRIST'),('bodyweight-squat','KNEE'),('wall-sit','KNEE'),
('reverse-lunge','KNEE'),('goblet-squat','KNEE'),('dumbbell-romanian-deadlift','LOWER_BACK'),('leg-press','KNEE'),
('leg-press','LOWER_BACK'),('leg-extension','KNEE'),('barbell-back-squat','KNEE'),('barbell-back-squat','LOWER_BACK'),
('step-up','KNEE'),('calf-raise','ANKLE'),('crunch','NECK'),('crunch','LOWER_BACK'),('bicycle-crunch','NECK'),
('bicycle-crunch','LOWER_BACK'),('side-plank','SHOULDER'),('leg-raise','LOWER_BACK'),('russian-twist','LOWER_BACK'),
('burpee','KNEE'),('burpee','WRIST'),('burpee','SHOULDER'),('burpee','LOWER_BACK'),('dumbbell-thruster','KNEE'),
('dumbbell-thruster','SHOULDER'),('kettlebell-swing','LOWER_BACK'),('jumping-jacks','KNEE'),('jumping-jacks','ANKLE'),
('high-knees','KNEE'),('high-knees','ANKLE'),('skipping-rope','KNEE'),('skipping-rope','ANKLE'),
('mountain-climber','WRIST'),('mountain-climber','SHOULDER'),('childs-pose','KNEE'),('hip-flexor-stretch','KNEE'),
('chest-doorway-stretch','SHOULDER'),('cobra-stretch','LOWER_BACK')
) AS v(slug, area)
JOIN exercises e ON e.slug = v.slug;

-- Alternatives (stored in both directions)
WITH pairs(a, b) AS (VALUES
('push-up','incline-push-up'),('push-up','knee-push-up'),('push-up','band-chest-press'),('push-up','dumbbell-floor-press'),
('dumbbell-bench-press','dumbbell-floor-press'),('dumbbell-bench-press','machine-chest-press'),('dumbbell-bench-press','push-up'),
('barbell-bench-press','dumbbell-bench-press'),('barbell-bench-press','machine-chest-press'),('incline-dumbbell-press','machine-chest-press'),
('incline-dumbbell-press','dumbbell-floor-press'),('cable-fly','band-chest-press'),('dumbbell-row','band-row'),
('dumbbell-row','seated-cable-row'),('lat-pulldown','pull-up'),('lat-pulldown','band-row'),('barbell-row','dumbbell-row'),
('barbell-row','seated-cable-row'),('superman','bird-dog'),('dumbbell-shoulder-press','machine-shoulder-press'),
('dumbbell-shoulder-press','pike-push-up'),('dumbbell-shoulder-press','band-pull-apart'),('lateral-raise','band-lateral-raise'),
('face-pull','band-pull-apart'),('dumbbell-curl','hammer-curl'),('dumbbell-curl','band-curl'),('dumbbell-curl','barbell-curl'),
('dumbbell-curl','cable-curl'),('overhead-dumbbell-extension','cable-pushdown'),('overhead-dumbbell-extension','band-pushdown'),
('overhead-dumbbell-extension','bench-dip'),('diamond-push-up','bench-dip'),('diamond-push-up','band-pushdown'),
('bodyweight-squat','glute-bridge'),('bodyweight-squat','wall-sit'),('bodyweight-squat','goblet-squat'),('bodyweight-squat','leg-press'),
('reverse-lunge','step-up'),('reverse-lunge','glute-bridge'),('dumbbell-romanian-deadlift','glute-bridge'),
('dumbbell-romanian-deadlift','leg-curl-machine'),('barbell-back-squat','goblet-squat'),('barbell-back-squat','leg-press'),
('leg-extension','wall-sit'),('step-up','glute-bridge'),('calf-raise','clamshell'),('crunch','dead-bug'),('bicycle-crunch','dead-bug'),
('leg-raise','dead-bug'),('russian-twist','side-plank'),('russian-twist','dead-bug'),('plank','dead-bug'),('plank','bird-dog'),
('side-plank','bird-dog'),('mountain-climber','march-in-place'),('burpee','march-in-place'),('burpee','dumbbell-thruster'),
('jumping-jacks','march-in-place'),('skipping-rope','march-in-place'),('skipping-rope','stationary-bike'),('high-knees','march-in-place'),
('brisk-walk','treadmill-walk-incline'),('brisk-walk','stationary-bike'),('kettlebell-swing','dumbbell-romanian-deadlift'),
('kettlebell-swing','glute-bridge'),('dumbbell-thruster','goblet-squat'),('pull-up','band-row'),('cobra-stretch','cat-cow'),
('childs-pose','cat-cow'),('hip-flexor-stretch','quad-stretch'),('chest-doorway-stretch','shoulder-stretch'),
('bench-dip','band-pushdown'),('pike-push-up','band-lateral-raise')
), both_ways AS (
    SELECT a, b FROM pairs UNION SELECT b, a FROM pairs
)
INSERT INTO exercise_alternatives (exercise_id, alternative_exercise_id)
SELECT x.id, y.id
FROM both_ways p
JOIN exercises x ON x.slug = p.a
JOIN exercises y ON y.slug = p.b
ON CONFLICT DO NOTHING;
