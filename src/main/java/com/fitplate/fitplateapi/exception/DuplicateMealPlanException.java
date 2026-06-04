package com.fitplate.fitplateapi.exception;

public class DuplicateMealPlanException extends RuntimeException {
    public DuplicateMealPlanException() {
        super("이미 저장된 식단입니다.");
    }
}