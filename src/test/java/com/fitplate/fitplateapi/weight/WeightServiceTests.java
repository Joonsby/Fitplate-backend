// 체중 기록의 같은 날짜 덮어쓰기·삭제·기간 조회·목표 비교와 사용자 분리를 검증합니다.
package com.fitplate.fitplateapi.weight;

import com.fitplate.fitplateapi.exception.InvalidRequestException;
import com.fitplate.fitplateapi.exception.ResourceNotFoundException;
import com.fitplate.fitplateapi.mealplan.service.MealPlanOwnership;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.repository.UserProfileRepository;
import com.fitplate.fitplateapi.user.repository.UserRepository;
import com.fitplate.fitplateapi.weight.dto.WeightDtos.WeightSummaryResponse;
import com.fitplate.fitplateapi.weight.service.WeightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({WeightService.class, MealPlanOwnership.class, WeightServiceTests.TestBeans.class})
class WeightServiceTests {
    private static final String USER_KEY = "weight-test-user";
    static final LocalDate TODAY = LocalDate.of(2026, 10, 21);

    @Autowired private WeightService weightService;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        for (String table : List.of("weight_records", "meal_plan_foods", "meal_plan_meals", "meal_plans",
                "favorite_foods", "user_profiles", "users")) {
            jdbc.update("DELETE FROM " + table);
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            User user = userRepository.saveAndFlush(User.create(USER_KEY));
            userProfileRepository.saveAndFlush(UserProfile.builder()
                    .user(user).height(175).weight(70).age(30).gender("MALE").bmi(new BigDecimal("22.86")).build());
        });
    }

    private static BigDecimal kg(String value) {
        return new BigDecimal(value);
    }

    @Test
    void sameDateOverwritesInsteadOfDuplicating() {
        assertThat(weightService.upsert(USER_KEY, TODAY, kg("70.4")).created()).isTrue();
        assertThat(weightService.upsert(USER_KEY, TODAY, kg("69.95")).created()).isFalse();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM weight_records", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT weight FROM weight_records", BigDecimal.class)).isEqualByComparingTo("69.95");
    }

    @Test
    void futureDateIsRejected() {
        assertThatThrownBy(() -> weightService.upsert(USER_KEY, TODAY.plusDays(1), kg("70")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rangeIsSortedWithChangeLatestAndTargetGap() {
        weightService.upsert(USER_KEY, TODAY.minusDays(40), kg("73.0"));
        weightService.upsert(USER_KEY, TODAY, kg("70.5"));
        weightService.upsert(USER_KEY, TODAY.minusDays(6), kg("71.6"));
        weightService.setTargetWeight(USER_KEY, kg("68"));

        WeightSummaryResponse week = weightService.getWeights(USER_KEY, TODAY.minusDays(6), TODAY);

        assertThat(week.records()).extracting(record -> record.date())
                .containsExactly(TODAY.minusDays(6), TODAY);
        assertThat(week.changeInRange()).isEqualByComparingTo("-1.1");
        assertThat(week.latest().weight()).isEqualByComparingTo("70.5");
        assertThat(week.targetWeight()).isEqualByComparingTo("68");
        assertThat(week.toTarget()).isEqualByComparingTo("2.5");

        WeightSummaryResponse older = weightService.getWeights(USER_KEY, TODAY.minusDays(50), TODAY.minusDays(30));
        assertThat(older.records()).hasSize(1);
        assertThat(older.changeInRange()).isNull();
        assertThat(older.latest().date()).isEqualTo(TODAY);
    }

    @Test
    void deleteRemovesOnlyThatDateAndMissingDateIs404() {
        weightService.upsert(USER_KEY, TODAY, kg("70"));
        weightService.upsert(USER_KEY, TODAY.minusDays(1), kg("70.2"));

        weightService.delete(USER_KEY, TODAY);

        assertThat(weightService.getWeights(USER_KEY, TODAY.minusDays(1), TODAY).records()).hasSize(1);
        assertThatThrownBy(() -> weightService.delete(USER_KEY, TODAY)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void targetCanBeClearedAndNeedsProfile() {
        weightService.setTargetWeight(USER_KEY, kg("65"));
        weightService.setTargetWeight(USER_KEY, null);
        assertThat(weightService.getWeights(USER_KEY, TODAY, TODAY).targetWeight()).isNull();

        userRepository.saveAndFlush(User.create("no-profile-user"));
        assertThatThrownBy(() -> weightService.setTargetWeight("no-profile-user", kg("60")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void usersSeeOnlyTheirOwnRecords() {
        weightService.upsert(USER_KEY, TODAY, kg("70"));
        userRepository.saveAndFlush(User.create("other-user"));

        WeightSummaryResponse other = weightService.getWeights("other-user", TODAY.minusDays(7), TODAY);

        assertThat(other.records()).isEmpty();
        assertThat(other.latest()).isNull();
        assertThatThrownBy(() -> weightService.delete("other-user", TODAY)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void tooLongRangeIsRejected() {
        assertThatThrownBy(() -> weightService.getWeights(USER_KEY, TODAY.minusDays(400), TODAY))
                .isInstanceOf(InvalidRequestException.class);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        Clock clock() {
            ZoneId seoul = ZoneId.of("Asia/Seoul");
            return Clock.fixed(ZonedDateTime.of(TODAY.atTime(12, 0), seoul).toInstant(), seoul);
        }
    }
}
