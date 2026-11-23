package com.fittrack.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fittrack.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthAndOnboardingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    @Test
    void fullRegistrationRefreshAndOnboardingFlow() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";

        // Register
        MvcResult reg = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "fullName", "Asha Kumar", "email", email,
                                "password", "Secret123", "timeZone", "Asia/Kolkata", "acceptTerms", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.onboardingCompleted").value(false))
                .andReturn();
        String access = body(reg).at("/data/accessToken").asText();
        Cookie refresh = reg.getResponse().getCookie(RefreshCookieFactory.COOKIE_NAME);
        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();

        // Duplicate email
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "fullName", "Other", "email", email.toUpperCase(), "password", "Secret123", "acceptTerms", true))))
                .andExpect(status().isConflict());

        // Authenticated call
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));

        // Refresh rotates the cookie; replaying the old one is rejected
        MvcResult refreshed = mvc.perform(post("/api/v1/auth/refresh").cookie(refresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        Cookie rotated = refreshed.getResponse().getCookie(RefreshCookieFactory.COOKIE_NAME);
        assertThat(rotated.getValue()).isNotEqualTo(refresh.getValue());
        mvc.perform(post("/api/v1/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
        // ...and reuse detection revoked the whole family, including the rotated token
        mvc.perform(post("/api/v1/auth/refresh").cookie(rotated)).andExpect(status().isUnauthorized());

        // Onboarding with an unsafe target (BMI < 18.5) is refused
        mvc.perform(post("/api/v1/me/onboarding").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(onboarding("40"))))
                .andExpect(status().isUnprocessableEntity());

        // Valid onboarding
        mvc.perform(post("/api/v1/me/onboarding").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(onboarding("68"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.data.currentWeightKg").value(78.5))
                .andExpect(jsonPath("$.data.targetWeightKg").value(68.0));

        mvc.perform(get("/api/v1/me/assessment").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableDays.length()").value(3))
                // equipment is returned in catalog display order
                .andExpect(jsonPath("$.data.equipment[0].code").value("YOGA_MAT"))
                .andExpect(jsonPath("$.data.equipment[1].code").value("DUMBBELLS"))
                .andExpect(jsonPath("$.data.limitations[0].bodyArea").value("KNEE"));
    }

    @Test
    void usersCannotSeeEachOthersRecords() throws Exception {
        String tokenA = registerAndGetToken();
        String tokenB = registerAndGetToken();

        MvcResult created = mvc.perform(post("/api/v1/me/goals").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("goalType", "DAILY_STEPS", "targetValue", 8000))))
                .andExpect(status().isCreated())
                .andReturn();
        long goalId = body(created).at("/data/id").asLong();

        mvc.perform(get("/api/v1/me/goals/" + goalId).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/me/goals/" + goalId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void validationAndAuthErrorsUseStandardEnvelope() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("fullName", "X", "email", "bad", "password", "weak"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").isArray());

        mvc.perform(get("/api/v1/me/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "nobody@example.com", "password", "Wrong1234"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        // Forgot password never reveals whether the account exists
        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "nobody@example.com"))))
                .andExpect(status().isOk());
    }

    private String registerAndGetToken() throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "fullName", "Test User", "email", "u-" + UUID.randomUUID() + "@example.com",
                                "password", "Secret123", "acceptTerms", true))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(r).at("/data/accessToken").asText();
    }

    private Map<String, Object> onboarding(String targetWeight) {
        return Map.of(
                "fullName", "Asha Kumar",
                "dateOfBirth", "1995-04-12",
                "gender", "FEMALE",
                "heightCm", 165,
                "currentWeightKg", 78.5,
                "targetWeightKg", new java.math.BigDecimal(targetWeight),
                "timeZone", "Asia/Kolkata",
                "assessment", Map.ofEntries(
                        Map.entry("fitnessLevel", "BEGINNER"),
                        Map.entry("primaryGoal", "WEIGHT_LOSS"),
                        Map.entry("workoutLocation", "HOME"),
                        Map.entry("availableDays", java.util.List.of("MONDAY", "WEDNESDAY", "FRIDAY")),
                        Map.entry("sessionMinutes", 30),
                        Map.entry("equipmentCodes", java.util.List.of("DUMBBELLS", "YOGA_MAT")),
                        Map.entry("dietPreference", "VEGETARIAN"),
                        Map.entry("limitations", java.util.List.of(Map.of("bodyArea", "KNEE", "severity", "MILD"))),
                        Map.entry("activityLevel", "LIGHTLY_ACTIVE"),
                        Map.entry("sleepHours", 7)));
    }

    private JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }
}
