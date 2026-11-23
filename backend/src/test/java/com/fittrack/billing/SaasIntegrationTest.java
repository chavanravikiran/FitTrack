package com.fittrack.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fittrack.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SaasIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    @Test
    void trialEmailVerificationCheckoutAndWebhookFlow() throws Exception {
        String email = "buyer-" + UUID.randomUUID() + "@example.com";
        String token = register(email);

        // New accounts start on a 7-day Premium trial
        mvc.perform(get("/api/v1/me/subscription").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planCode").value("PREMIUM"))
                .andExpect(jsonPath("$.data.status").value("TRIALING"))
                .andExpect(jsonPath("$.data.daysRemaining").value(7));

        when(paymentGateway.isConfigured()).thenReturn(true);
        when(paymentGateway.publicKey()).thenReturn("rzp_test_key");

        // Checkout requires a verified email
        mvc.perform(post("/api/v1/me/billing/checkout").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("planCode", "PREMIUM", "billingCycle", "MONTHLY"))))
                .andExpect(status().isUnprocessableEntity());

        // Verify email using the token from the (mocked) email
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).send(eq(email), contains("Confirm"), body.capture());
        Matcher m = Pattern.compile("token=([A-Za-z0-9_%-]+)").matcher(body.getValue());
        assertThat(m.find()).isTrue();
        mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("token", URLDecoder.decode(m.group(1), StandardCharsets.UTF_8)))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.emailVerified").value(true));

        // Checkout: server decides the amount (read current prices - other tests may edit plans)
        JsonNode premium = body(mvc.perform(get("/api/v1/plans")).andReturn()).at("/data/1");
        long monthly = premium.get("priceMonthlyMinor").asLong();
        long yearly = premium.get("priceYearlyMinor").asLong();
        when(paymentGateway.createOrder(anyLong(), anyString(), anyString(), anyMap())).thenReturn("order_A", "order_B");
        mvc.perform(post("/api/v1/me/billing/checkout").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("planCode", "PREMIUM", "billingCycle", "MONTHLY"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value("order_A"))
                .andExpect(jsonPath("$.data.amountMinor").value(monthly))
                .andExpect(jsonPath("$.data.keyId").value("rzp_test_key"));

        // A forged signature is rejected
        when(paymentGateway.verifyPaymentSignature(any(), any(), any())).thenReturn(false);
        when(paymentGateway.verifyPaymentSignature("order_A", "pay_A", "good-sig")).thenReturn(true);
        mvc.perform(post("/api/v1/me/billing/verify").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("orderId", "order_A", "paymentId", "pay_A", "signature", "forged"))))
                .andExpect(status().isBadRequest());

        // Genuine payment activates Premium for a month
        JsonNode sub = body(mvc.perform(post("/api/v1/me/billing/verify").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("orderId", "order_A", "paymentId", "pay_A", "signature", "good-sig"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.source").value("PAYMENT"))
                .andReturn()).at("/data");
        Instant end1 = Instant.parse(sub.get("currentPeriodEnd").asText());
        assertThat(Duration.between(Instant.now(), end1).toDays()).isBetween(27L, 31L);

        // Replaying the verification is idempotent (no double extension)
        JsonNode replay = body(mvc.perform(post("/api/v1/me/billing/verify").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("orderId", "order_A", "paymentId", "pay_A", "signature", "good-sig"))))
                .andExpect(status().isOk()).andReturn()).at("/data");
        assertThat(Instant.parse(replay.get("currentPeriodEnd").asText())).isEqualTo(end1);

        // Yearly renewal confirmed only via webhook extends from the current end date
        mvc.perform(post("/api/v1/me/billing/checkout").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("planCode", "PREMIUM", "billingCycle", "YEARLY"))))
                .andExpect(jsonPath("$.data.amountMinor").value(yearly));
        String hook = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_B","order_id":"order_B"}}}}""";
        when(paymentGateway.verifyWebhookSignature(hook, "hook-sig")).thenReturn(true);
        mvc.perform(post("/api/v1/webhooks/razorpay").header("X-Razorpay-Signature", "hook-sig")
                        .contentType(MediaType.APPLICATION_JSON).content(hook))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/webhooks/razorpay").header("X-Razorpay-Signature", "bad")
                        .contentType(MediaType.APPLICATION_JSON).content(hook))
                .andExpect(status().isBadRequest());

        JsonNode afterRenewal = body(mvc.perform(get("/api/v1/me/subscription").header("Authorization", bearer(token)))
                .andReturn()).at("/data");
        Instant end2 = Instant.parse(afterRenewal.get("currentPeriodEnd").asText());
        assertThat(Duration.between(end1, end2).toDays()).isBetween(364L, 366L);

        mvc.perform(get("/api/v1/me/billing/payments").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].status").value("PAID"))
                .andExpect(jsonPath("$.data.content[1].status").value("PAID"));
    }

    @Test
    void adminPanelEntitlementsAndAccountDeletion() throws Exception {
        String email = "member-" + UUID.randomUUID() + "@example.com";
        String user = register(email);
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        long userId = body(mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(user))).andReturn())
                .at("/data/id").asLong();

        // Admin-only API
        mvc.perform(get("/api/v1/admin/stats").header("Authorization", bearer(user))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/stats").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").isNumber());

        // Free plan: 3 active goals max -> 4th is 402 Payment Required
        mvc.perform(post("/api/v1/admin/users/" + userId + "/revoke").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planCode").value("FREE"));
        for (var g : new Object[][]{{"DAILY_STEPS", 8000}, {"DAILY_WATER", 2500}, {"SLEEP_HOURS", 8}}) {
            createGoal(user, (String) g[0], (Integer) g[1]).andExpect(status().isCreated());
        }
        createGoal(user, "WEEKLY_WORKOUTS", 3).andExpect(status().isPaymentRequired());
        // Replacing an existing goal type is still allowed on Free
        createGoal(user, "DAILY_STEPS", 9000).andExpect(status().isCreated());

        // Admin grants Premium -> limit lifted
        mvc.perform(post("/api/v1/admin/users/" + userId + "/grant").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("planCode", "PREMIUM", "days", 30, "note", "Support goodwill"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planCode").value("PREMIUM"));
        createGoal(user, "WEEKLY_WORKOUTS", 3).andExpect(status().isCreated());

        // Search finds the user; detail shows subscription history
        mvc.perform(get("/api/v1/admin/users").param("q", email.substring(0, 12)).param("plan", "PREMIUM")
                        .header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value(email));
        mvc.perform(get("/api/v1/admin/users/" + userId).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.data.subscriptions.length()").value(3));

        // Disable blocks sign-in; enable restores it
        mvc.perform(patch("/api/v1/admin/users/" + userId + "/status").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "Secret123"))))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/users/" + userId + "/status").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isOk());

        // Plan editing: prices change publicly; the free plan must stay free
        mvc.perform(put("/api/v1/admin/plans/FREE").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Free", "priceMonthlyMinor", 100, "priceYearlyMinor", 0,
                                "active", true, "features", java.util.List.of()))))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/v1/admin/plans/PREMIUM").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Premium", "description", "All features",
                                "priceMonthlyMinor", 24900, "priceYearlyMinor", 249900, "active", true,
                                // keep the full feature set: other test classes share this database
                                "features", java.util.List.of(Map.of("code", "ACTIVE_GOALS"), Map.of("code", "PDF_REPORTS"),
                                        Map.of("code", "ADVANCED_ANALYTICS"), Map.of("code", "PROGRESS_PHOTOS"),
                                        Map.of("code", "CUSTOM_WORKOUTS"), Map.of("code", "DATA_EXPORT"))))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/plans"))
                .andExpect(jsonPath("$.data[1].priceMonthlyMinor").value(24900));

        // Self-service deletion removes the account
        mvc.perform(delete("/api/v1/me/account").header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("password", "Secret123", "confirmation", "DELETE"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "Secret123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationRequiresTermsAcceptance() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("fullName", "No Terms", "email", "nt-" + UUID.randomUUID() + "@x.com",
                                "password", "Secret123", "acceptTerms", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("acceptTerms"));
    }

    private org.springframework.test.web.servlet.ResultActions createGoal(String token, String type, int target) throws Exception {
        return mvc.perform(post("/api/v1/me/goals").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("goalType", type, "targetValue", target))));
    }

    private String register(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("fullName", "Test User", "email", email,
                                "password", "Secret123", "acceptTerms", true))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(r).at("/data/accessToken").asText();
    }

    private String login(String email, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(r).at("/data/accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }
}
