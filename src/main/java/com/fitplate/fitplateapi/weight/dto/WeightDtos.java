// 체중 기록 API 의 요청·응답 모양입니다.
package com.fitplate.fitplateapi.weight.dto;

import com.fitplate.fitplateapi.weight.domain.WeightRecord;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class WeightDtos {

    private WeightDtos() {
    }

    public record WeightRecordResponse(LocalDate date, BigDecimal weight) {
        public static WeightRecordResponse from(WeightRecord record) {
            return new WeightRecordResponse(record.getRecordDate(), record.getWeight());
        }
    }

    /**
     * records 는 기간 안의 기록(날짜 오름차순), latest 는 기간과 상관없는 가장 최근 기록이다.
     * changeInRange 는 기간 안 마지막 - 첫 기록(2개 미만이면 null),
     * toTarget 은 최근 체중 - 목표 체중(둘 중 하나라도 없으면 null)이다.
     */
    public record WeightSummaryResponse(
            List<WeightRecordResponse> records,
            WeightRecordResponse latest,
            BigDecimal targetWeight,
            BigDecimal changeInRange,
            BigDecimal toTarget
    ) {
    }

    public record WeightUpsertRequest(
            @NotNull(message = "체중은 필수입니다")
            @DecimalMin(value = "20.0", message = "체중은 20kg 이상이어야 합니다")
            @DecimalMax(value = "300.0", message = "체중은 300kg 이하여야 합니다")
            BigDecimal weight
    ) {
    }

    /** targetWeight 가 null 이면 목표 체중을 해제한다. */
    public record TargetWeightRequest(
            @DecimalMin(value = "20.0", message = "목표 체중은 20kg 이상이어야 합니다")
            @DecimalMax(value = "300.0", message = "목표 체중은 300kg 이하여야 합니다")
            BigDecimal targetWeight
    ) {
    }
}
