package com.fittrack.user.service;

import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.security.CurrentUserService;
import com.fittrack.storage.StorageService;
import com.fittrack.user.entity.User;
import com.fittrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Self-service account deletion (right to erasure). All personal data is removed via ON DELETE CASCADE
 * foreign keys; uploaded files are deleted once the transaction commits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storage;
    private final CurrentUserService currentUser;

    @Transactional
    public void deleteMyAccount(String password) {
        User user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", currentUser.userId()));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessRuleException("Password is incorrect");
        }
        if (user.isAdmin()) {
            throw new BusinessRuleException("Administrator accounts can't be deleted from the app. Ask another admin to remove your admin role first.");
        }

        List<String> files = new ArrayList<>();
        if (user.getProfile() != null && user.getProfile().getPhotoPath() != null) {
            files.add(user.getProfile().getPhotoPath());
        }
        Long id = user.getId();
        userRepository.delete(user);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                files.forEach(storage::delete);
                log.info("Deleted account {} and {} stored file(s)", id, files.size());
            }
        });
    }
}
