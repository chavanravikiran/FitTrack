package com.fittrack.auth.service;

import com.fittrack.auth.dto.AuthDtos.AuthResult;
import com.fittrack.auth.dto.AuthDtos.RegisterRequest;
import com.fittrack.billing.entity.Subscription;
import com.fittrack.billing.entity.SubscriptionPlan;
import com.fittrack.billing.service.SubscriptionService;
import com.fittrack.common.exception.DuplicateResourceException;
import com.fittrack.config.AppProperties;
import com.fittrack.security.CurrentUserService;
import com.fittrack.security.JwtService;
import com.fittrack.user.entity.Role;
import com.fittrack.user.entity.RoleName;
import com.fittrack.user.entity.User;
import com.fittrack.user.repository.RoleRepository;
import com.fittrack.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private CurrentUserService currentUserService;
    @Mock private SubscriptionService subscriptionService;
    @Mock private EmailVerificationService emailVerificationService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, authenticationManager,
                jwtService, refreshTokenService, currentUserService, subscriptionService, emailVerificationService,
                new AppProperties(), Clock.systemUTC());
    }

    @Test
    void registerRecordsTermsStartsTrialAndSendsVerification() {
        Role role = new Role();
        role.setName(RoleName.ROLE_USER);
        SubscriptionPlan premium = new SubscriptionPlan();
        premium.setCode("PREMIUM");
        Subscription trial = new Subscription();
        trial.setPlan(premium);

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Secret123")).thenReturn("HASH");
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(refreshTokenService.issue(any(), anyString())).thenReturn("refresh");
        when(subscriptionService.current(any())).thenReturn(trial);

        AuthResult result = authService.register(
                new RegisterRequest("  Jane Doe ", "  Jane@Example.COM ", "Secret123", "Asia/Kolkata", true), "JUnit");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("HASH");
        assertThat(saved.getProfile().getFullName()).isEqualTo("Jane Doe");
        assertThat(saved.getTermsAcceptedAt()).isNotNull();
        assertThat(saved.getTermsVersion()).isNotBlank();
        verify(subscriptionService).startForNewUser(saved);
        verify(emailVerificationService).sendVerification(saved);
        assertThat(result.refreshToken()).isEqualTo("refresh");
        assertThat(result.response().user().premium()).isTrue();
        assertThat(result.response().user().planCode()).isEqualTo("PREMIUM");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Jane", "JANE@example.com", "Secret123", null, true), null))
                .isInstanceOf(DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }
}
