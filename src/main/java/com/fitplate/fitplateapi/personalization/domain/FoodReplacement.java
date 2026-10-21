// 사용자가 식단에서 음식을 바꾼 이력입니다. 다음 추천에서 뺀 음식은 덜, 넣은 음식은 더 고려합니다.
package com.fitplate.fitplateapi.personalization.domain;

import com.fitplate.fitplateapi.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_replacements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodReplacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "replacement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "from_name", nullable = false)
    private String fromName;

    @Column(name = "to_name", nullable = false)
    private String toName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public FoodReplacement(User user, String fromName, String toName, LocalDateTime createdAt) {
        this.user = user;
        this.fromName = fromName;
        this.toName = toName;
        this.createdAt = createdAt;
    }
}
