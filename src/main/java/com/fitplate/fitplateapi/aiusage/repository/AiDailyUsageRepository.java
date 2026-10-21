// 하루 AI 사용량 조회입니다. 동시에 들어온 요청이 같은 줄을 덮어쓰지 않도록 잠그고 읽습니다.
package com.fitplate.fitplateapi.aiusage.repository;

import com.fitplate.fitplateapi.aiusage.domain.AiDailyUsage;
import com.fitplate.fitplateapi.aiusage.domain.AiFeature;
import com.fitplate.fitplateapi.user.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDate;
import java.util.Optional;

public interface AiDailyUsageRepository extends JpaRepository<AiDailyUsage, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AiDailyUsage> findByUserAndUsageDateAndFeature(User user, LocalDate usageDate, AiFeature feature);
}
