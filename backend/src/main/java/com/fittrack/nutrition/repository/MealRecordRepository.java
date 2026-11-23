package com.fittrack.nutrition.repository;

import com.fittrack.nutrition.entity.MealRecord;
import com.fittrack.nutrition.entity.MealRecord.MealType;
import com.fittrack.nutrition.entity.MealRecordItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {

    @EntityGraph(attributePaths = {"items"})
    List<MealRecord> findByUserIdAndRecordDate(Long userId, LocalDate date);

    Optional<MealRecord> findByUserIdAndRecordDateAndMealType(Long userId, LocalDate date, MealType type);

    @Query("select i from MealRecordItem i where i.id = :itemId and i.mealRecord.user.id = :userId")
    Optional<MealRecordItem> findItem(@Param("itemId") Long itemId, @Param("userId") Long userId);

    /** [date, kcal, protein, carbs, fat] per day. */
    @Query("""
            select m.recordDate, sum(i.calories), sum(i.proteinG), sum(i.carbsG), sum(i.fatG)
            from MealRecordItem i join i.mealRecord m
            where m.user.id = :userId and m.recordDate between :from and :to
            group by m.recordDate order by m.recordDate
            """)
    List<Object[]> dailyTotals(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
