package com.fittrack.admin.repository;

import com.fittrack.admin.dto.AdminDtos.AdminUserRow;
import com.fittrack.billing.entity.BillingEnums.SubscriptionStatus;
import com.fittrack.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Read-only reporting queries for the admin panel. */
public interface AdminQueryRepository extends Repository<User, Long> {

    String ROW = """
            select new com.fittrack.admin.dto.AdminDtos$AdminUserRow(
                u.id, u.email, p.fullName, u.enabled, u.emailVerified, coalesce(p.onboardingCompleted, false),
                pl.code, s.status, s.currentPeriodEnd, u.createdAt, u.lastLoginAt)
            from User u
            left join u.profile p
            left join Subscription s on s.user = u and s.status in :live
            left join s.plan pl
            """;

    String FILTER = """
            where (:q = '' or lower(u.email) like :q or lower(p.fullName) like :q)
              and (:plan = '' or pl.code = :plan)
              and (:status = 'ANY' or (:status = 'ENABLED' and u.enabled = true) or (:status = 'DISABLED' and u.enabled = false))
            """;

    @Query(value = ROW + FILTER + " order by u.createdAt desc",
            countQuery = """
                    select count(u) from User u
                    left join u.profile p
                    left join Subscription s on s.user = u and s.status in :live
                    left join s.plan pl
                    """ + FILTER)
    Page<AdminUserRow> searchUsers(@Param("q") String q, @Param("plan") String plan, @Param("status") String status,
                                   @Param("live") Collection<SubscriptionStatus> live, Pageable pageable);

    @Query(ROW + " where u.id = :id")
    Optional<AdminUserRow> findRow(@Param("id") Long id, @Param("live") Collection<SubscriptionStatus> live);

    @Query("select count(u) from User u")
    long countUsers();

    @Query("select count(u) from User u where u.createdAt >= :since")
    long countCreatedSince(@Param("since") Instant since);

    @Query("select count(u) from User u where u.lastLoginAt >= :since")
    long countActiveSince(@Param("since") Instant since);

    @Query(value = """
            select cast(created_at at time zone 'UTC' as date) as day, count(*) as cnt
            from users where created_at >= :since group by 1 order by 1
            """, nativeQuery = true)
    List<Object[]> signupsByDay(@Param("since") Instant since);
}
