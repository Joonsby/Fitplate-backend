// 사용자의 날짜별 체중 기록입니다. 사용자·날짜마다 하나입니다.
package com.fitplate.fitplateapi.weight.domain;

import com.fitplate.fitplateapi.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weight_records", uniqueConstraints = {
        @UniqueConstraint(name = "uk_weight_records_user_date", columnNames = {"user_id", "record_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weight_record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public WeightRecord(User user, LocalDate recordDate, BigDecimal weight) {
        this.user = user;
        this.recordDate = recordDate;
        this.weight = weight;
    }

    public void changeWeight(BigDecimal weight) {
        this.weight = weight;
    }
}
