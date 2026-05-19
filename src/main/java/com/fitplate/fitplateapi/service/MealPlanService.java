package com.fitplate.fitplateapi.service;

import com.fitplate.fitplateapi.client.GeminiMealPlanClient;
import com.fitplate.fitplateapi.dto.MealPlanRequest;
import com.fitplate.fitplateapi.dto.MealPlanResponse;
import org.springframework.stereotype.Service;

@Service
public class MealPlanService {

    private final GeminiMealPlanClient geminiMealPlanClient;

    public MealPlanService(GeminiMealPlanClient geminiMealPlanClient) {
        this.geminiMealPlanClient = geminiMealPlanClient;
    }

    public MealPlanResponse generateMealPlan(MealPlanRequest request) {
        return geminiMealPlanClient.generateMealPlan(request);
    }
}