package com.fittrack.user.service;

import com.fittrack.config.AppProperties;
import com.fittrack.user.entity.RoleName;
import com.fittrack.user.entity.User;
import com.fittrack.user.entity.UserProfile;
import com.fittrack.user.repository.RoleRepository;
import com.fittrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/** Creates the first administrator from ADMIN_EMAIL / ADMIN_PASSWORD if it doesn't exist yet. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final AppProperties props;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = props.getAdmin().getEmail();
        String password = props.getAdmin().getPassword();
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return;
        }
        email = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setEmailVerified(true);
        admin.getRoles().add(roleRepository.findByName(RoleName.ROLE_USER).orElseThrow());
        admin.getRoles().add(roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow());
        UserProfile profile = new UserProfile();
        profile.setFullName("FitTrack Admin");
        admin.setProfile(profile);
        userRepository.save(admin);
        log.info("Created bootstrap admin account {}", email);
    }
}
