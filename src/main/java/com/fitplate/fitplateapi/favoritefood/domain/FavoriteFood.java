package com.fitplate.fitplateapi.favoritefood.domain;

import com.fitplate.fitplateapi.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "favorite_foods",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_favorite_foods_user_name_amount",
                        columnNames = {"user_id", "food_name", "amount"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FavoriteFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_food_id")
    private Long favoriteFoodId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "food_name", nullable = false, length = 100)
    private String foodName;

    @Column(name = "amount", length = 50)
    private String amount;

    private Integer calories;

    @Column(precision = 8, scale = 2)
    private BigDecimal carbohydrate;

    @Column(precision = 8, scale = 2)
    private BigDecimal protein;

    @Column(precision = 8, scale = 2)
    private BigDecimal fat;

    @Column(name = "shopping_category", length = 50)
    private String shoppingCategory;

    @Column(name = "shopping_keyword", length = 100)
    private String shoppingKeyword;

    @Column(name = "source_food_id", length = 100)
    private String sourceFoodId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}