package com.fittrack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fittrack.reminder.repository.ReminderRepository;
import com.fittrack.reminder.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Workouts, activity, nutrition, videos and reminders end-to-end against the seeded catalog. */
class FeatureModulesIntegrationTest extends AbstractIntegrationTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ReminderRepository reminderRepository;

    @Test
    void workoutPlanSessionAndPersonalRecords() throws Exception {
        String token = onboardedUser(List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"),
                List.of(Map.of("bodyArea", "KNEE", "severity", "MODERATE")));

        JsonNode plan = data(call(get("/api/v1/me/workout-plan"), token).andExpect(status().isOk()));
        assertThat(plan.get("days")).hasSize(7);
        long training = 0;
        for (JsonNode d : plan.get("days")) {
            if (!d.get("restDay").asBoolean()) {
                training++;
                for (JsonNode e : d.get("exercises")) {
                    // knee limitation: no squats, lunges, step-ups or leg press
                    assertThat(e.at("/exercise/slug").asText()).isNotIn("bodyweight-squat", "reverse-lunge", "step-up", "leg-press", "goblet-squat");
                }
            }
        }
        assertThat(training).isEqualTo(4); // beginner cap
        assertThat(plan.get("explanations").size()).isGreaterThan(2);

        // Find the most recent training day within the last week and log it.
        LocalDate today = LocalDate.now(ZONE);
        JsonNode cal = data(call(get("/api/v1/me/workouts/calendar").param("from", today.minusDays(6).toString())
                .param("to", today.toString()), token).andExpect(status().isOk()));
        JsonNode trainingDay = null;
        for (JsonNode e : cal) if (!e.get("restDay").asBoolean()) trainingDay = e;
        assertThat(trainingDay).isNotNull();
        String date = trainingDay.get("date").asText();
        long planDayId = trainingDay.get("planDayId").asLong();

        JsonNode day = data(call(post("/api/v1/me/workouts/start").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("date", date, "planDayId", planDayId))), token).andExpect(status().isOk()));
        assertThat(day.get("status").asText()).isEqualTo("IN_PROGRESS");
        long sessionId = day.get("sessionId").asLong();
        JsonNode first = day.get("exercises").get(0);
        long exId = first.at("/exercise/id").asLong();
        boolean timed = first.hasNonNull("durationSec"); // null fields are omitted from responses

        call(put("/api/v1/me/workouts/sessions/" + sessionId + "/sets").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("sets", List.of(
                        timed ? Map.of("exerciseId", exId, "setNumber", 1, "durationSec", 45, "completed", true)
                              : Map.of("exerciseId", exId, "setNumber", 1, "repsDone", 12, "weightKg", 8, "completed", true))))), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exercises[0].loggedSets[0].completed").value(true));

        call(post("/api/v1/me/workouts/sessions/" + sessionId + "/complete").contentType(MediaType.APPLICATION_JSON)
                .content("{\"perceivedEffort\":6}"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.caloriesEst").isNumber());

        call(get("/api/v1/me/personal-records"), token).andExpect(jsonPath("$.data.length()").value(1));
        call(get("/api/v1/me/workouts/history"), token).andExpect(jsonPath("$.data.totalElements").value(1));
        call(get("/api/v1/me/workouts/summary"), token).andExpect(jsonPath("$.data.totalCompleted").value(1));

        // Free/trial users: swapping a plan exercise needs CUSTOM_WORKOUTS (trial = Premium, so allowed)
        long planExerciseId = plan.get("days").findValues("exercises").stream()
                .filter(n -> n.size() > 0).findFirst().orElseThrow().get(0).get("id").asLong();
        call(put("/api/v1/me/workout-plan/exercises/" + planExerciseId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"exerciseId\":" + idOf(token, "dead-bug") + "}"), token).andExpect(status().isOk());
    }

    @Test
    void activityWaterSleepNutritionVideosAndReminders() throws Exception {
        String token = onboardedUser(List.of("MONDAY", "WEDNESDAY", "FRIDAY"), List.of());
        LocalDate today = LocalDate.now(ZONE);

        // Steps
        call(put("/api/v1/me/activity/" + today).contentType(MediaType.APPLICATION_JSON).content("{\"steps\":7200}"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.distanceM").isNumber())
                .andExpect(jsonPath("$.data.stepGoal").isNumber());
        call(put("/api/v1/me/activity/" + today.plusDays(1)).contentType(MediaType.APPLICATION_JSON).content("{\"steps\":10}"), token)
                .andExpect(status().isUnprocessableEntity());
        call(get("/api/v1/me/activity/summary").param("days", "7"), token)
                .andExpect(jsonPath("$.data.history.length()").value(7))
                .andExpect(jsonPath("$.data.totalSteps").value(7200));

        // Water
        call(post("/api/v1/me/water").contentType(MediaType.APPLICATION_JSON).content("{\"amountMl\":250}"), token).andExpect(status().isOk());
        JsonNode water = data(call(post("/api/v1/me/water").contentType(MediaType.APPLICATION_JSON).content("{\"amountMl\":500}"), token));
        assertThat(water.get("totalMl").asInt()).isEqualTo(750);
        assertThat(water.get("goalMl").asInt()).isGreaterThan(1500);
        long entryId = water.get("entries").get(0).get("id").asLong();
        call(delete("/api/v1/me/water/" + entryId), token).andExpect(jsonPath("$.data.totalMl").value(500));
        call(put("/api/v1/me/water/goal/" + today).contentType(MediaType.APPLICATION_JSON)
                .content("{\"goalMl\":3500,\"reason\":\"Hot day\"}"), token).andExpect(jsonPath("$.data.goalMl").value(3500));

        // Sleep (bed before midnight, wake next morning)
        call(put("/api/v1/me/sleep/" + today).contentType(MediaType.APPLICATION_JSON)
                .content("{\"bedTime\":\"23:00\",\"wakeTime\":\"06:30\",\"quality\":4,\"restDay\":false}"), token)
                .andExpect(jsonPath("$.data.durationMin").value(450));
        call(get("/api/v1/me/recovery"), token).andExpect(jsonPath("$.data.status").isString());

        // Nutrition: vegetarian user never sees chicken; meals add up
        JsonNode foods = data(call(get("/api/v1/foods").param("q", "paneer").param("diet", "VEGETARIAN"), token));
        long paneerId = -1;
        for (JsonNode f : foods.get("content")) if (f.get("name").asText().equals("Paneer")) paneerId = f.get("id").asLong();
        assertThat(paneerId).isPositive(); // 100 g serving, 18 g protein
        call(get("/api/v1/foods").param("q", "chicken").param("diet", "VEGETARIAN"), token)
                .andExpect(jsonPath("$.data.totalElements").value(0));
        call(post("/api/v1/me/meals/" + today + "/LUNCH/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"foodId\":" + paneerId + ",\"servings\":1.5}"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totals.proteinG").value(27))
                .andExpect(jsonPath("$.data.targets.calories").isNumber());
        JsonNode sugg = data(call(get("/api/v1/me/nutrition/suggestions").param("mealType", "BREAKFAST"), token));
        assertThat(sugg).isNotEmpty();
        sugg.forEach(s -> assertThat(s.at("/food/dietType").asText()).isIn("VEGAN", "VEGETARIAN"));

        // Videos: seeded library with instructions
        call(get("/api/v1/videos").param("category", "WARM_UP"), token)
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].instructions").isString());

        // Recommendations
        call(get("/api/v1/me/recommendations"), token)
                .andExpect(jsonPath("$.data.targets.calorieTargetKcal").isNumber())
                .andExpect(jsonPath("$.data.items.length()").value(org.hamcrest.Matchers.greaterThan(5)));

        // Reminders: create, then fire it as the scheduler would
        JsonNode reminder = data(call(post("/api/v1/me/reminders").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("type", "WATER", "title", "Drink water", "time", "09:00",
                        "days", List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"),
                        "intervalMinutes", 120, "endTime", "19:00", "channel", "IN_APP", "enabled", true))), token)
                .andExpect(status().isCreated()));
        assertThat(reminder.get("nextFireAt").isTextual()).isTrue();
        var entity = reminderRepository.findById(reminder.get("id").asLong()).orElseThrow();
        notificationService.fire(entity.getId(), entity.getNextFireAt());
        call(get("/api/v1/me/notifications/unread-count"), token).andExpect(jsonPath("$.data.unread").value(1));
        call(post("/api/v1/me/notifications/read-all"), token).andExpect(status().isOk());
        call(get("/api/v1/me/notifications/unread-count"), token).andExpect(jsonPath("$.data.unread").value(0));
    }

    // ------------------------------------------------------------------------------------------

    private long idOf(String token, String slug) throws Exception {
        for (JsonNode e : data(call(get("/api/v1/exercises"), token))) {
            if (e.get("slug").asText().equals(slug)) return e.get("id").asLong();
        }
        throw new AssertionError("exercise not found: " + slug);
    }

    private String onboardedUser(List<String> days, List<Map<String, String>> limitations) throws Exception {
        MvcResult reg = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("fullName", "Feature Tester", "email",
                                "f-" + UUID.randomUUID() + "@example.com", "password", "Secret123", "acceptTerms", true,
                                "timeZone", "Asia/Kolkata"))))
                .andExpect(status().isCreated()).andReturn();
        String token = json.readTree(reg.getResponse().getContentAsString()).at("/data/accessToken").asText();
        call(post("/api/v1/me/onboarding").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "fullName", "Feature Tester", "dateOfBirth", "1992-03-10", "gender", "FEMALE", "heightCm", 162,
                "currentWeightKg", 72, "targetWeightKg", 64, "timeZone", "Asia/Kolkata",
                "assessment", Map.ofEntries(
                        Map.entry("fitnessLevel", "BEGINNER"), Map.entry("primaryGoal", "WEIGHT_LOSS"),
                        Map.entry("workoutLocation", "HOME"), Map.entry("availableDays", days),
                        Map.entry("sessionMinutes", 45), Map.entry("equipmentCodes", List.of("DUMBBELLS", "YOGA_MAT")),
                        Map.entry("dietPreference", "VEGETARIAN"), Map.entry("allergies", "peanuts"),
                        Map.entry("limitations", limitations), Map.entry("activityLevel", "LIGHTLY_ACTIVE"),
                        Map.entry("sleepHours", 7))))), token)
                .andExpect(status().isCreated());
        return token;
    }

    private ResultActions call(MockHttpServletRequestBuilder req, String token) throws Exception {
        return mvc.perform(req.header("Authorization", "Bearer " + token));
    }

    private JsonNode data(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString()).get("data");
    }
}
