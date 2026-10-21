// 사용자가 하루에 AI 기능을 부른 횟수입니다. 사용자·날짜·기능마다 한 줄입니다.
package com.fitplate.fitplateapi.aiusage.domain;

import com.fitplate.fitplateapi.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "ai_daily_usage", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ai_daily_usage_user_date_feature", columnNames = {"user_id", "usage_date", "feature"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiDailyUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private AiFeature feature;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount;

    public AiDailyUsage(User user, LocalDate usageDate, AiFeature feature) {
        this.user = user;
        this.usageDate = usageDate;
        this.feature = feature;
        this.usedCount = 0;
    }

    public void increase() {
        usedCount++;
    }

    public void decrease() {
        if (usedCount > 0) {
            usedCount--;
        }
    }
}
