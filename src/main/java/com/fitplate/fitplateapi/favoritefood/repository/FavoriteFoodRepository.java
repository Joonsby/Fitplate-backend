package com.fitplate.fitplateapi.favoritefood.repository;

import com.fitplate.fitplateapi.favoritefood.domain.FavoriteFood;
import com.fitplate.fitplateapi.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteFoodRepository extends JpaRepository<FavoriteFood, Long> {

    List<FavoriteFood> findByUserOrderByCreatedAtDesc(User user);

    Optional<FavoriteFood> findByUserAndFoodNameAndAmount(
            User user,
            String foodName,
            String amount
    );

    Optional<FavoriteFood> findByFavoriteFoodIdAndUser(
            Long favoriteFoodId,
            User user
    );
}