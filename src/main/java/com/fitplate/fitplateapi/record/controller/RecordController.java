package com.fitplate.fitplateapi.record.controller;

import com.fitplate.fitplateapi.auth.jwt.JwtTokenProvider;
import com.fitplate.fitplateapi.record.dto.RecordsResponse;
import com.fitplate.fitplateapi.record.service.DailyRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Tag(name = "식단 기록", description = "날짜별 섭취 완료 기록과 영양 통계")
public class RecordController {

    private final DailyRecordService dailyRecordService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(
            summary = "날짜별 기록과 기간 요약",
            description = """
                    from~to(최대 93일) 날짜마다 지정된 식단, 끼니별 섭취 여부, 완료율,
                    목표·계획·실제 섭취 영양 합계를 돌려줍니다.

                    status 는 NO_PLAN(식단 없음), UPCOMING(아직 오지 않은 날), NOT_RECORDED(체크 없음),
                    PARTIAL, COMPLETE 입니다. 체크한 끼니가 없는 날의 eaten 은 0 이 아니라 null 입니다.
                    summary 의 평균은 한 끼 이상 체크한 날로만 계산합니다.
                    """
    )
    @GetMapping
    public ResponseEntity<RecordsResponse> getRecords(
            @RequestHeader("Authorization") String authorization,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String tossUserKey = jwtTokenProvider.getTossUserKey(jwtTokenProvider.resolveToken(authorization));
        return ResponseEntity.ok(dailyRecordService.getRecords(tossUserKey, from, to));
    }
}
