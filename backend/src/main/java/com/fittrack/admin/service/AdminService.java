package com.fittrack.admin.service;

import com.fittrack.admin.dto.AdminDtos.AdminStats;
import com.fittrack.admin.dto.AdminDtos.AdminUserDetail;
import com.fittrack.admin.dto.AdminDtos.AdminUserRow;
import com.fittrack.admin.dto.AdminDtos.DayCount;
import com.fittrack.admin.dto.AdminDtos.GrantRequest;
import com.fittrack.admin.dto.AdminDtos.PlanUpdateRequest;
import com.fittrack.admin.dto.AdminDtos.SubscriptionHistoryDto;
import com.fittrack.admin.repository.AdminQueryRepository;
import com.fittrack.auth.service.RefreshTokenService;
import com.fittrack.billing.dto.BillingDtos.FeatureDto;
import com.fittrack.billing.dto.BillingDtos.PaymentDto;
import com.fittrack.billing.dto.BillingDtos.PlanDto;
import com.fittrack.billing.entity.BillingEnums.BillingCycle;
import com.fittrack.billing.entity.BillingEnums.PaymentStatus;
import com.fittrack.billing.entity.BillingEnums.SubscriptionStatus;
import com.fittrack.billing.entity.PlanFeature;
import com.fittrack.billing.entity.Subscription;
import com.fittrack.billing.entity.SubscriptionPlan;
import com.fittrack.billing.gateway.PaymentGateway;
import com.fittrack.billing.mapper.BillingMapper;
import com.fittrack.billing.repository.PaymentRepository;
import com.fittrack.billing.repository.SubscriptionPlanRepository;
import com.fittrack.billing.repository.SubscriptionRepository;
import com.fittrack.billing.service.SubscriptionService;
import com.fittrack.common.api.PageResponse;
import com.fittrack.common.exception.BadRequestException;
import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.security.CurrentUserService;
import com.fittrack.user.entity.User;
import com.fittrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminQueryRepository adminQueries;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionService subscriptionService;
    private final RefreshTokenService refreshTokenService;
    private final PaymentGateway gateway;
    private final BillingMapper billingMapper;
    private final CurrentUserService currentUser;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AdminStats stats() {
        Instant now = clock.instant();
        Instant since = now.minus(Duration.ofDays(30));

        long mrr = subscriptionRepository.findActivePaidWithCycle().stream()
                .mapToLong(s -> s.getBillingCycle() == BillingCycle.YEARLY
                        ? Math.round(s.getPlan().getPriceYearlyMinor() / 12.0)
                        : s.getPlan().getPriceMonthlyMinor())
                .sum();

        List<DayCount> signups = adminQueries.signupsByDay(since).stream()
                .map(r -> new DayCount(toLocalDate(r[0]), ((Number) r[1]).longValue()))
                .toList();

        return new AdminStats(
                adminQueries.countUsers(),
                adminQueries.countCreatedSince(since),
                adminQueries.countActiveSince(since),
                subscriptionRepository.countLivePaidPlans(Set.of(SubscriptionStatus.ACTIVE)),
                subscriptionRepository.countByStatus(SubscriptionStatus.TRIALING),
                paymentRepository.sumAmount(PaymentStatus.PAID, since),
                mrr,
                "INR",
                gateway.isConfigured(),
                signups);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserRow> users(String query, String planCode, String status, int page, int size) {
        String q = StringUtils.hasText(query) ? "%" + query.trim().toLowerCase(Locale.ROOT) + "%" : "";
        String plan = StringUtils.hasText(planCode) ? planCode.trim().toUpperCase(Locale.ROOT) : "";
        String st = switch (status == null ? "ANY" : status.toUpperCase(Locale.ROOT)) {
            case "ENABLED", "DISABLED" -> status.toUpperCase(Locale.ROOT);
            default -> "ANY";
        };
        return PageResponse.of(adminQueries.searchUsers(q, plan, st, SubscriptionService.LIVE, PageRequest.of(page, size)),
                r -> r);
    }

    @Transactional(readOnly = true)
    public AdminUserDetail userDetail(Long id) {
        User user = findUser(id);
        AdminUserRow row = adminQueries.findRow(id, SubscriptionService.LIVE).orElseThrow();
        List<SubscriptionHistoryDto> subs = subscriptionRepository.findByUserIdOrderByCreatedAtDesc(id).stream()
                .map(this::toHistory).toList();
        List<PaymentDto> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(id, PageRequest.of(0, 50))
                .map(billingMapper::toDto).getContent();
        return new AdminUserDetail(row, user.getRoles().stream().map(r -> r.getName().name()).sorted().toList(),
                user.getTermsVersion(), user.getTermsAcceptedAt(), subs, payments);
    }

    @Transactional
    public AdminUserRow setEnabled(Long id, boolean enabled) {
        if (id.equals(currentUser.userId())) {
            throw new BusinessRuleException("You can't disable your own account");
        }
        User user = findUser(id);
        if (!enabled && user.isAdmin()) {
            throw new BusinessRuleException("Remove the admin role before disabling an administrator");
        }
        user.setEnabled(enabled);
        if (!enabled) {
            refreshTokenService.revokeAllForUser(id); // sign them out everywhere
        }
        log.info("Admin {} set user {} enabled={}", currentUser.userId(), id, enabled);
        return adminQueries.findRow(id, SubscriptionService.LIVE).orElseThrow();
    }

    @Transactional
    public AdminUserRow grant(Long id, GrantRequest request) {
        findUser(id);
        SubscriptionPlan plan = planRepository.findByCode(request.planCode().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + request.planCode()));
        if (plan.isFree()) {
            throw new BadRequestException("Use 'revoke' to move a user to the free plan");
        }
        String note = StringUtils.hasText(request.note()) ? request.note().trim()
                : "Granted by admin " + currentUser.userId();
        subscriptionService.grant(id, plan, request.days(), note);
        log.info("Admin {} granted {} days of {} to user {}", currentUser.userId(), request.days(), plan.getCode(), id);
        return adminQueries.findRow(id, SubscriptionService.LIVE).orElseThrow();
    }

    @Transactional
    public AdminUserRow revoke(Long id) {
        findUser(id);
        subscriptionService.revertToFree(id, "Revoked by admin " + currentUser.userId());
        log.info("Admin {} moved user {} to the free plan", currentUser.userId(), id);
        return adminQueries.findRow(id, SubscriptionService.LIVE).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<PlanDto> plans() {
        return planRepository.findAllByOrderBySortOrderAsc().stream().map(billingMapper::toDto).toList();
    }

    @Transactional
    public PlanDto updatePlan(String code, PlanUpdateRequest req) {
        SubscriptionPlan plan = planRepository.findByCode(code.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + code));
        if (plan.isFree()) {
            if (req.priceMonthlyMinor() != 0 || req.priceYearlyMinor() != 0) {
                throw new BusinessRuleException("The free plan must stay free");
            }
            if (!req.active()) {
                throw new BusinessRuleException("The free plan can't be deactivated - it's every user's fallback");
            }
        } else if (req.active() && req.priceMonthlyMinor() == 0 && req.priceYearlyMinor() == 0) {
            throw new BusinessRuleException("An active paid plan needs at least one price");
        }

        Set<PlanFeature> features = new HashSet<>();
        for (FeatureDto f : req.features()) {
            if (f.code() == null) {
                throw new BadRequestException("Feature code is required");
            }
            if (f.limit() != null && f.limit() < 0) {
                throw new BadRequestException("Feature limits can't be negative");
            }
            if (!features.add(new PlanFeature(f.code(), f.limit()))) {
                throw new BadRequestException("Feature listed twice: " + f.code());
            }
        }

        plan.setName(req.name().trim());
        plan.setDescription(StringUtils.hasText(req.description()) ? req.description().trim() : null);
        plan.setPriceMonthlyMinor(req.priceMonthlyMinor());
        plan.setPriceYearlyMinor(req.priceYearlyMinor());
        plan.setActive(req.active());
        plan.getFeatures().clear();
        plan.getFeatures().addAll(features);
        log.info("Admin {} updated plan {}", currentUser.userId(), plan.getCode());
        return billingMapper.toDto(plan);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentDto> payments(PaymentStatus status, int page, int size) {
        PageRequest pr = PageRequest.of(page, size);
        return PageResponse.of(status == null
                ? paymentRepository.findAllByOrderByCreatedAtDesc(pr)
                : paymentRepository.findByStatusOrderByCreatedAtDesc(status, pr), billingMapper::toDto);
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private SubscriptionHistoryDto toHistory(Subscription s) {
        return new SubscriptionHistoryDto(s.getId(), s.getPlan().getCode(), s.getStatus(), s.getSource(),
                s.getBillingCycle(), s.getCurrentPeriodStart(), s.getCurrentPeriodEnd(), s.getEndedAt(), s.getNotes());
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate d) return d;
        if (o instanceof Date d) return d.toLocalDate();
        return LocalDate.parse(o.toString());
    }
}
