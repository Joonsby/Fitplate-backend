// 음식 분류 집합을 "PORK,NUTS" 같은 쉼표 구분 문자열 컬럼으로 저장합니다.
package com.fitplate.fitplateapi.user.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class FoodCategorySetConverter implements AttributeConverter<Set<FoodCategory>, String> {

    @Override
    public String convertToDatabaseColumn(Set<FoodCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        return EnumSet.copyOf(categories).stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<FoodCategory> convertToEntityAttribute(String value) {
        EnumSet<FoodCategory> categories = EnumSet.noneOf(FoodCategory.class);
        if (value == null || value.isBlank()) {
            return categories;
        }
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .forEach(name -> categories.add(FoodCategory.valueOf(name)));
        return categories;
    }
}
