package com.fitplate.fitplateapi.weight.controller;

import com.fitplate.fitplateapi.auth.jwt.JwtTokenProvider;
import com.fitplate.fitplateapi.weight.dto.WeightDtos.TargetWeightRequest;
import com.fitplate.fitplateapi.weight.dto.WeightDtos.WeightRecordResponse;
import com.fitplate.fitplateapi.weight.dto.WeightDtos.WeightSummaryResponse;
import com.fitplate.fitplateapi.weight.dto.WeightDtos.WeightUpsertRequest;
import com.fitplate.fitplateapi.weight.service.WeightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/weights")
@RequiredArgsConstructor
@Tag(name = "체중 기록", description = "날짜별 체중 기록과 목표 체중")
public class WeightController {

    private final WeightService weightService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "체중 기록 조회", description = """
            from~to(최대 400일)의 체중 기록과 가장 최근 기록, 목표 체중,
            기간 안 변화량(마지막-첫 기록), 목표까지 남은 양(최근-목표)을 돌려줍니다.
            """)
    @GetMapping
    public ResponseEntity<WeightSummaryResponse> getWeights(
            @RequestHeader("Authorization") String authorization,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(weightService.getWeights(userKey(authorization), from, to));
    }

    @Operation(summary = "체중 기록·수정", description = """
            날짜의 체중을 기록합니다. 같은 날짜에 이미 기록이 있으면 덮어씁니다(200), 새로 만들면 201 입니다.
            아직 오지 않은 날짜는 400 입니다.
            """)
    @PutMapping("/{date}")
    public ResponseEntity<WeightRecordResponse> upsertWeight(
            @RequestHeader("Authorization") String authorization,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody WeightUpsertRequest request
    ) {
        WeightService.UpsertResult result = weightService.upsert(userKey(authorization), date, request.weight());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.record());
    }

    @Operation(summary = "체중 기록 삭제", description = "날짜의 체중 기록을 지웁니다. 기록이 없으면 404 입니다.")
    @DeleteMapping("/{date}")
    public ResponseEntity<Void> deleteWeight(
            @RequestHeader("Authorization") String authorization,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        weightService.delete(userKey(authorization), date);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "목표 체중 설정", description = "목표 체중을 정합니다. targetWeight 가 null 이면 해제합니다.")
    @PutMapping("/target")
    public ResponseEntity<Map<String, BigDecimal>> setTargetWeight(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody TargetWeightRequest request
    ) {
        BigDecimal saved = weightService.setTargetWeight(userKey(authorization), request.targetWeight());
        return ResponseEntity.ok(java.util.Collections.singletonMap("targetWeight", saved));
    }

    private String userKey(String authorization) {
        return jwtTokenProvider.getTossUserKey(jwtTokenProvider.resolveToken(authorization));
    }
}
