package com.fittrack.nutrition.repository;

import com.fittrack.nutrition.entity.NutritionFood;
import com.fittrack.nutrition.entity.NutritionFood.Category;
import com.fittrack.nutrition.entity.NutritionFood.DietType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NutritionFoodRepository extends JpaRepository<NutritionFood, Long> {

    /** System foods plus the user's own custom foods, filtered. Empty-string params mean "any". */
    @Query("""
            select f from NutritionFood f left join f.owner o
            where f.active = true
              and (f.system = true or o.id = :userId)
              and (:q = '' or lower(f.name) like :q)
              and (:category = '' or str(f.category) = :category)
              and f.dietType in :diets
            order by f.system asc, f.name asc
            """)
    Page<NutritionFood> search(@Param("userId") Long userId, @Param("q") String q, @Param("category") String category,
                               @Param("diets") Collection<DietType> diets, Pageable pageable);

    @Query("""
            select f from NutritionFood f left join f.owner o
            where f.active = true and (f.system = true or o.id = :userId)
              and f.category in :categories and f.dietType in :diets
            """)
    List<NutritionFood> forSuggestions(@Param("userId") Long userId, @Param("categories") Collection<Category> categories,
                                       @Param("diets") Collection<DietType> diets);

    @Query("select f from NutritionFood f left join f.owner o where f.id = :id and f.active = true and (f.system = true or o.id = :userId)")
    Optional<NutritionFood> findUsable(@Param("id") Long id, @Param("userId") Long userId);

    Optional<NutritionFood> findByIdAndOwnerId(Long id, Long ownerId);

    Page<NutritionFood> findBySystemTrueOrderByNameAsc(Pageable pageable);
}
