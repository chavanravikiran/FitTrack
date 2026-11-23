package com.fittrack.nutrition.service;

import com.fittrack.assessment.entity.AssessmentEnums.DietPreference;
import com.fittrack.common.api.PageResponse;
import com.fittrack.common.exception.BusinessRuleException;
import com.fittrack.common.exception.ResourceNotFoundException;
import com.fittrack.nutrition.dto.NutritionDtos.AddItemRequest;
import com.fittrack.nutrition.dto.NutritionDtos.DailyTotalsDto;
import com.fittrack.nutrition.dto.NutritionDtos.DayNutritionDto;
import com.fittrack.nutrition.dto.NutritionDtos.FoodDto;
import com.fittrack.nutrition.dto.NutritionDtos.FoodRequest;
import com.fittrack.nutrition.dto.NutritionDtos.MealDto;
import com.fittrack.nutrition.dto.NutritionDtos.MealItemDto;
import com.fittrack.nutrition.dto.NutritionDtos.SuggestionDto;
import com.fittrack.nutrition.dto.NutritionDtos.Totals;
import com.fittrack.nutrition.entity.MealRecord;
import com.fittrack.nutrition.entity.MealRecord.MealType;
import com.fittrack.nutrition.entity.MealRecordItem;
import com.fittrack.nutrition.entity.NutritionFood;
import com.fittrack.nutrition.entity.NutritionFood.Category;
import com.fittrack.nutrition.entity.NutritionFood.DietType;
import com.fittrack.nutrition.repository.MealRecordRepository;
import com.fittrack.nutrition.repository.NutritionFoodRepository;
import com.fittrack.recommendation.FitnessContext;
import com.fittrack.recommendation.FitnessContextService;
import com.fittrack.recommendation.RecommendationService;
import com.fittrack.recommendation.dto.RecommendationDtos.DailyTargets;
import com.fittrack.security.CurrentUserService;
import com.fittrack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NutritionService {

    public static final String DISCLAIMER = "Calories and macros are approximate estimates for typical home-cooked "
            + "servings, not medical or dietary advice. Portion sizes and recipes vary.";

    private static final Map<String, List<String>> ALLERGEN_KEYWORDS = Map.of(
            "peanut", List.of("peanut"),
            "nut", List.of("almond", "walnut", "peanut", "cashew"),
            "milk", List.of("milk", "paneer", "curd", "dahi", "yogurt", "lassi", "buttermilk", "ghee", "whey", "tea with milk"),
            "lactose", List.of("milk", "paneer", "curd", "dahi", "yogurt", "lassi", "buttermilk", "whey", "tea with milk"),
            "dairy", List.of("milk", "paneer", "curd", "dahi", "yogurt", "lassi", "buttermilk", "ghee", "whey", "tea with milk"),
            "egg", List.of("egg", "omelette"),
            "gluten", List.of("chapati", "roti", "phulka", "paratha", "bread", "daliya", "upma", "samosa", "biscuit"),
            "wheat", List.of("chapati", "roti", "phulka", "paratha", "bread", "daliya", "upma", "samosa", "biscuit"),
            "soy", List.of("soya", "tofu"),
            "fish", List.of("fish", "prawn"));

    private final NutritionFoodRepository foodRepository;
    private final MealRecordRepository mealRepository;
    private final UserRepository userRepository;
    private final FitnessContextService contextService;
    private final RecommendationService recommendationService;
    private final CurrentUserService currentUser;

    // ---------------------------------------------------------------- foods

    @Transactional(readOnly = true)
    public PageResponse<FoodDto> searchFoods(String q, Category category, DietType diet, int page, int size) {
        Long userId = currentUser.userId();
        String query = StringUtils.hasText(q) ? "%" + q.trim().toLowerCase(Locale.ROOT) + "%" : "";
        Set<DietType> diets = diet == null ? EnumSet.allOf(DietType.class) : compatible(diet);
        return PageResponse.of(foodRepository.search(userId, query, category == null ? "" : category.name(), diets,
                PageRequest.of(page, size)), NutritionService::toDto);
    }

    @Transactional
    public FoodDto createCustomFood(FoodRequest req) {
        NutritionFood f = new NutritionFood();
        apply(f, req);
        f.setSystem(false);
        f.setOwner(userRepository.getReferenceById(currentUser.userId()));
        return toDto(foodRepository.save(f));
    }

    /** Custom foods are deactivated (not deleted) because past meal logs reference them. */
    @Transactional
    public void deleteCustomFood(Long id) {
        NutritionFood f = foodRepository.findByIdAndOwnerId(id, currentUser.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("Food", id));
        f.setActive(false);
    }

    // ---- admin: system foods ----

    @Transactional(readOnly = true)
    public PageResponse<FoodDto> systemFoods(int page, int size) {
        return PageResponse.of(foodRepository.findBySystemTrueOrderByNameAsc(PageRequest.of(page, size)), NutritionService::toDto);
    }

    @Transactional
    public FoodDto createSystemFood(FoodRequest req) {
        NutritionFood f = new NutritionFood();
        apply(f, req);
        f.setSystem(true);
        return toDto(foodRepository.save(f));
    }

    @Transactional
    public FoodDto updateSystemFood(Long id, FoodRequest req, boolean active) {
        NutritionFood f = foodRepository.findById(id).filter(NutritionFood::isSystem)
                .orElseThrow(() -> ResourceNotFoundException.of("Food", id));
        apply(f, req);
        f.setActive(active);
        return toDto(f);
    }

    // ---------------------------------------------------------------- meals

    @Transactional(readOnly = true)
    public DayNutritionDto day(LocalDate requestedDate) {
        Long userId = currentUser.userId();
        LocalDate date = requestedDate == null ? contextService.today(userId) : requestedDate;
        Map<MealType, MealRecord> records = mealRepository.findByUserIdAndRecordDate(userId, date).stream()
                .collect(Collectors.toMap(MealRecord::getMealType, Function.identity()));

        List<MealDto> meals = new ArrayList<>();
        int kcal = 0, p = 0, c = 0, f = 0;
        for (MealType type : MealType.values()) {
            MealRecord r = records.get(type);
            List<MealItemDto> items = r == null ? List.of() : r.getItems().stream().map(NutritionService::toDto).toList();
            Totals t = totals(r == null ? List.of() : r.getItems());
            kcal += t.calories(); p += t.proteinG(); c += t.carbsG(); f += t.fatG();
            meals.add(new MealDto(type, items, t));
        }
        Totals total = new Totals(kcal, p, c, f);
        DailyTargets targets = recommendationService.targetsFor(userId);
        Totals target = targets.calorieTargetKcal() == null ? null
                : new Totals(targets.calorieTargetKcal(), (targets.proteinMinG() + targets.proteinMaxG()) / 2,
                targets.carbsG(), targets.fatG());
        return new DayNutritionDto(date, meals, total, target, insights(total, target, targets), DISCLAIMER);
    }

    @Transactional
    public DayNutritionDto addItem(LocalDate date, MealType type, AddItemRequest req) {
        Long userId = currentUser.userId();
        validateDate(date, contextService.today(userId));
        NutritionFood food = foodRepository.findUsable(req.foodId(), userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Food", req.foodId()));
        MealRecord meal = mealRepository.findByUserIdAndRecordDateAndMealType(userId, date, type).orElseGet(() -> {
            MealRecord m = new MealRecord();
            m.setUser(userRepository.getReferenceById(userId));
            m.setRecordDate(date);
            m.setMealType(type);
            return mealRepository.save(m);
        });
        if (meal.getItems().size() >= 30) {
            throw new BusinessRuleException("A meal can have at most 30 items");
        }
        MealRecordItem item = new MealRecordItem();
        item.setMealRecord(meal);
        item.setFood(food);
        item.applyServings(req.servings());
        meal.getItems().add(item);
        mealRepository.saveAndFlush(meal);
        return day(date);
    }

    @Transactional
    public DayNutritionDto updateItem(Long itemId, BigDecimal servings) {
        MealRecordItem item = mealRepository.findItem(itemId, currentUser.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("Meal item", itemId));
        item.applyServings(servings);
        mealRepository.flush();
        return day(item.getMealRecord().getRecordDate());
    }

    @Transactional
    public DayNutritionDto deleteItem(Long itemId) {
        MealRecordItem item = mealRepository.findItem(itemId, currentUser.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("Meal item", itemId));
        MealRecord meal = item.getMealRecord();
        LocalDate date = meal.getRecordDate();
        meal.getItems().remove(item);
        if (meal.getItems().isEmpty()) {
            mealRepository.delete(meal);
        }
        mealRepository.flush();
        return day(date);
    }

    @Transactional(readOnly = true)
    public List<DailyTotalsDto> summary(int days) {
        if (days < 1 || days > 366) {
            throw new BusinessRuleException("Choose between 1 and 366 days");
        }
        Long userId = currentUser.userId();
        LocalDate to = contextService.today(userId);
        LocalDate from = to.minusDays(days - 1L);
        Map<LocalDate, Totals> byDate = mealRepository.dailyTotals(userId, from, to).stream().collect(Collectors.toMap(
                r -> (LocalDate) r[0],
                r -> new Totals(round(r[1]), round(r[2]), round(r[3]), round(r[4]))));
        List<DailyTotalsDto> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            out.add(new DailyTotalsDto(d, byDate.getOrDefault(d, Totals.ZERO)));
        }
        return out;
    }

    /** Foods matching the user's diet preference, excluding declared allergens, ranked by protein density. */
    @Transactional(readOnly = true)
    public List<SuggestionDto> suggestions(MealType mealType) {
        Long userId = currentUser.userId();
        FitnessContext ctx = contextService.load(userId);
        DietPreference pref = ctx.assessment() == null ? DietPreference.NON_VEGETARIAN : ctx.assessment().getDietPreference();
        Set<DietType> diets = compatible(DietType.valueOf(pref.name()));
        List<String> blocked = blockedKeywords(ctx.assessment() == null ? null : ctx.assessment().getAllergies());

        Set<Category> categories = switch (mealType) {
            case BREAKFAST -> EnumSet.of(Category.GRAINS, Category.EGGS, Category.DAIRY, Category.FRUITS, Category.PULSES);
            case LUNCH, DINNER -> EnumSet.of(Category.GRAINS, Category.PULSES, Category.VEGETABLES, Category.PREPARED,
                    Category.MEAT, Category.SEAFOOD, Category.DAIRY);
            case EVENING_SNACK -> EnumSet.of(Category.SNACKS, Category.FRUITS, Category.NUTS_SEEDS, Category.BEVERAGES, Category.DAIRY);
        };
        return foodRepository.forSuggestions(userId, categories, diets).stream()
                .filter(f -> blocked.stream().noneMatch(k -> f.getName().toLowerCase(Locale.ROOT).contains(k)))
                .filter(f -> f.getCalories().signum() > 0)
                .sorted(Comparator.comparingDouble((NutritionFood f) ->
                        -f.getProteinG().doubleValue() * 4 / f.getCalories().doubleValue()).thenComparing(NutritionFood::getName))
                .limit(8)
                .map(f -> new SuggestionDto(toDto(f), reason(f)))
                .toList();
    }

    // ---------------------------------------------------------------- helpers

    /** Which foods fit a diet: vegans see vegan foods; vegetarians also see vegetarian; eggetarians add eggs. */
    static Set<DietType> compatible(DietType diet) {
        return switch (diet) {
            case VEGAN -> EnumSet.of(DietType.VEGAN);
            case VEGETARIAN -> EnumSet.of(DietType.VEGAN, DietType.VEGETARIAN);
            case EGGETARIAN -> EnumSet.of(DietType.VEGAN, DietType.VEGETARIAN, DietType.EGGETARIAN);
            case NON_VEGETARIAN -> EnumSet.allOf(DietType.class);
        };
    }

    static List<String> blockedKeywords(String allergies) {
        if (!StringUtils.hasText(allergies)) return List.of();
        String text = allergies.toLowerCase(Locale.ROOT);
        return ALLERGEN_KEYWORDS.entrySet().stream()
                .filter(e -> text.contains(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .distinct()
                .toList();
    }

    private static String reason(NutritionFood f) {
        double proteinShare = f.getProteinG().doubleValue() * 4 / f.getCalories().doubleValue();
        if (proteinShare >= 0.3) return "High in protein";
        if (f.getCategory() == Category.FRUITS || f.getCategory() == Category.VEGETABLES) return "Fibre, vitamins and minerals";
        if (f.getCalories().doubleValue() <= 120) return "Light option";
        return "Balanced choice";
    }

    private static List<String> insights(Totals total, Totals target, DailyTargets t) {
        List<String> out = new ArrayList<>();
        if (total.calories() == 0) {
            out.add("Log your meals to see calories and macros for the day.");
            return out;
        }
        if (target != null) {
            if (total.proteinG() < t.proteinMinG()) {
                out.add("Protein is below your range (" + t.proteinMinG() + "-" + t.proteinMaxG()
                        + " g). Add dal, paneer, curd, eggs, soya or chicken to a meal.");
            } else {
                out.add("Nice - your protein is in range.");
            }
            int diff = total.calories() - target.calories();
            if (diff > 300) out.add("You're about " + diff + " kcal above today's estimate. That's okay occasionally - aim for balance over the week.");
            else if (diff < -500) out.add("You're well below today's estimate. Eating too little can leave you tired - include a balanced snack.");
        }
        return out;
    }

    private static void apply(NutritionFood f, FoodRequest req) {
        f.setName(req.name().trim());
        f.setCategory(req.category());
        f.setDietType(req.dietType());
        f.setServingQty(req.servingQty());
        f.setServingUnit(req.servingUnit().trim());
        f.setCalories(req.calories());
        f.setProteinG(req.proteinG());
        f.setCarbsG(req.carbsG());
        f.setFatG(req.fatG());
    }

    private static Totals totals(List<MealRecordItem> items) {
        BigDecimal k = BigDecimal.ZERO, p = BigDecimal.ZERO, c = BigDecimal.ZERO, f = BigDecimal.ZERO;
        for (MealRecordItem i : items) {
            k = k.add(i.getCalories());
            p = p.add(i.getProteinG());
            c = c.add(i.getCarbsG());
            f = f.add(i.getFatG());
        }
        return new Totals(round(k), round(p), round(c), round(f));
    }

    private static int round(Object n) {
        return n == null ? 0 : new BigDecimal(n.toString()).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private static void validateDate(LocalDate date, LocalDate today) {
        if (date.isAfter(today.plusDays(1))) {
            throw new BusinessRuleException("You can't log meals that far in the future");
        }
        if (ChronoUnit.DAYS.between(date, today) > 366) {
            throw new BusinessRuleException("Meals can be logged for the past year only");
        }
    }

    public static FoodDto toDto(NutritionFood f) {
        return new FoodDto(f.getId(), f.getName(), f.getCategory(), f.getDietType(), f.getServingQty(), f.getServingUnit(),
                f.getCalories(), f.getProteinG(), f.getCarbsG(), f.getFatG(), !f.isSystem());
    }

    private static MealItemDto toDto(MealRecordItem i) {
        NutritionFood f = i.getFood();
        return new MealItemDto(i.getId(), f.getId(), f.getName(), i.getServings(), f.getServingQty(), f.getServingUnit(),
                i.getCalories(), i.getProteinG(), i.getCarbsG(), i.getFatG());
    }
}
