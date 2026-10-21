// 칼로리와 탄수화물·단백질·지방 합계입니다. 영양소는 소수 첫째 자리까지 반올림합니다.
package com.fitplate.fitplateapi.record.dto;

public record NutritionTotals(int calories, double protein, double carbohydrate, double fat) {

    public static final NutritionTotals ZERO = new NutritionTotals(0, 0, 0, 0);

    public NutritionTotals plus(int calories, double protein, double carbohydrate, double fat) {
        return new NutritionTotals(this.calories + calories, this.protein + protein,
                this.carbohydrate + carbohydrate, this.fat + fat);
    }

    public NutritionTotals plus(NutritionTotals other) {
        return plus(other.calories, other.protein, other.carbohydrate, other.fat);
    }

    public NutritionTotals dividedBy(int count) {
        return new NutritionTotals(Math.round((float) calories / count), protein / count, carbohydrate / count, fat / count)
                .rounded();
    }

    public NutritionTotals rounded() {
        return new NutritionTotals(calories, round(protein), round(carbohydrate), round(fat));
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
