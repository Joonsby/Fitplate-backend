// 날짜별 체중 기록(같은 날짜는 덮어쓰기)과 목표 체중을 관리합니다.
package com.fitplate.fitplateapi.weight.service;

import com.fitplate.fitplateapi.exception.InvalidRequestException;
import com.fitplate.fitplateapi.exception.ResourceNotFoundException;
import com.fitplate.fitplateapi.mealplan.service.MealPlanOwnership;
import com.fitplate.fitplateapi.user.domain.User;
import com.fitplate.fitplateapi.user.domain.UserProfile;
import com.fitplate.fitplateapi.user.repository.UserProfileRepository;
import com.fitplate.fitplateapi.weight.domain.WeightRecord;
import com.fitplate.fitplateapi.weight.dto.WeightDtos.WeightRecordResponse;
import com.fitplate.fitplateapi.weight.dto.WeightDtos.WeightSummaryResponse;
import com.fitplate.fitplateapi.weight.repository.WeightRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeightService {

    static final int MAX_DAYS = 400;

    private final WeightRecordRepository weightRecordRepository;
    private final UserProfileRepository userProfileRepository;
    private final MealPlanOwnership ownership;
    private final Clock clock;

    @Transactional(readOnly = true)
    public WeightSummaryResponse getWeights(String tossUserKey, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new InvalidRequestException("조회 기간이 올바르지 않습니다. 한 번에 " + MAX_DAYS + "일까지 조회할 수 있습니다.");
        }
        User user = ownership.findUser(tossUserKey);
        List<WeightRecordResponse> records = weightRecordRepository
                .findByUserAndRecordDateBetweenOrderByRecordDateAsc(user, from, to)
                .stream().map(WeightRecordResponse::from).toList();
        WeightRecordResponse latest = weightRecordRepository.findFirstByUserOrderByRecordDateDesc(user)
                .map(WeightRecordResponse::from).orElse(null);
        BigDecimal targetWeight = userProfileRepository.findByUser(user)
                .map(UserProfile::getTargetWeight).orElse(null);

        BigDecimal changeInRange = records.size() < 2 ? null
                : records.get(records.size() - 1).weight().subtract(records.get(0).weight());
        BigDecimal toTarget = latest == null || targetWeight == null ? null
                : latest.weight().subtract(targetWeight);
        return new WeightSummaryResponse(records, latest, targetWeight, changeInRange, toTarget);
    }

    /** 같은 날짜에 기록이 있으면 덮어쓴다. 새로 만들었으면 created=true. */
    @Transactional
    public UpsertResult upsert(String tossUserKey, LocalDate date, BigDecimal weight) {
        if (date.isAfter(LocalDate.now(clock))) {
            throw new InvalidRequestException("아직 오지 않은 날짜의 체중은 기록할 수 없어요.");
        }
        User user = ownership.findUser(tossUserKey);
        BigDecimal rounded = weight.setScale(2, RoundingMode.HALF_UP);
        return weightRecordRepository.findByUserAndRecordDate(user, date)
                .map(existing -> {
                    existing.changeWeight(rounded);
                    return new UpsertResult(WeightRecordResponse.from(existing), false);
                })
                .orElseGet(() -> new UpsertResult(
                        WeightRecordResponse.from(weightRecordRepository.save(new WeightRecord(user, date, rounded))), true));
    }

    @Transactional
    public void delete(String tossUserKey, LocalDate date) {
        User user = ownership.findUser(tossUserKey);
        WeightRecord record = weightRecordRepository.findByUserAndRecordDate(user, date)
                .orElseThrow(() -> new ResourceNotFoundException(date, "해당 날짜의 체중 기록이 없습니다"));
        weightRecordRepository.delete(record);
    }

    /** null 이면 목표 체중을 해제한다. 내 정보(키·몸무게)를 먼저 입력해야 한다. */
    @Transactional
    public BigDecimal setTargetWeight(String tossUserKey, BigDecimal targetWeight) {
        User user = ownership.findUser(tossUserKey);
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new InvalidRequestException("먼저 내 정보를 입력해 주세요."));
        BigDecimal value = targetWeight == null ? null : targetWeight.setScale(2, RoundingMode.HALF_UP);
        profile.updateTargetWeight(value);
        return value;
    }

    public record UpsertResult(WeightRecordResponse record, boolean created) {
    }
}
