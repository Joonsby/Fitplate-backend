# ResourceNotFoundException 마이그레이션 가이드

## 개요

기존의 `MealPlanNotFoundException`, `UserNotFoundException`, `UserProfileNotFoundException` 세 개의 Exception 클래스를 하나의 통합 `ResourceNotFoundException` 클래스로 개선했습니다.

## 💡 핵심 개선사항

### 이전 구조 (레거시)
```
MealPlanNotFoundException (특정)
UserNotFoundException (특정)
UserProfileNotFoundException (특정)
... 새로운 리소스 추가 시마다 새로운 Exception 클래스 필요
```

### 새로운 구조 (통합)
```
ResourceNotFoundException (일반)
- 첫 번째 파라미터: Object resourceId (ID, Key 등)
- 두 번째 파라미터: String message (사용자 정의 메시지)
```

## 📋 사용 예시

### 기존 코드 (레거시 - 여전히 작동함)
```java
// MealPlan 찾을 수 없을 때
if (mealPlan == null) {
    throw new MealPlanNotFoundException(mealPlanId);
}

// User 찾을 수 없을 때
if (user == null) {
    throw new UserNotFoundException(userId);
}

// UserProfile 찾을 수 없을 때
if (userProfile == null) {
    throw new UserProfileNotFoundException(userKey);
}
```

### 새로운 코드 (권장)
```java
// MealPlan 찾을 수 없을 때
if (mealPlan == null) {
    throw new ResourceNotFoundException(mealPlanId, "식단을 찾을 수 없습니다");
}

// User 찾을 수 없을 때
if (user == null) {
    throw new ResourceNotFoundException(userId, "사용자를 찾을 수 없습니다");
}

// UserProfile 찾을 수 없을 때
if (userProfile == null) {
    throw new ResourceNotFoundException(userKey, "사용자 프로필을 찾을 수 없습니다");
}

// 새로운 리소스 추가 (Nutrition, Menu 등)
if (nutrition == null) {
    throw new ResourceNotFoundException(nutritionId, "영양소 정보를 찾을 수 없습니다");
}
```

## 🎯 마이그레이션 체크리스트

1. **기존 코드 유지**: 기존 Exception 클래스들은 여전히 작동합니다
2. **새로운 코드 작성**: 새로운 기능은 `ResourceNotFoundException` 사용
3. **점진적 마이그레이션**: 코드 수정 시 기회를 활용해 점진적으로 전환

### 마이그레이션 대상 파일들
```
src/main/java/com/fitplate/fitplateapi/
├── mealplan/
│   └── service/MealPlanService.java (MealPlanNotFoundException → ResourceNotFoundException)
├── user/
│   └── service/UserProfileService.java (UserNotFoundException, UserProfileNotFoundException → ResourceNotFoundException)
```

## 🔧 GlobalExceptionHandler 처리

새로운 `ResourceNotFoundException`은 자동으로 다음과 같이 처리됩니다:

```
ResourceNotFoundException 발생
          ↓
GlobalExceptionHandler.handleResourceNotFound()
          ↓
404 Not Found + ErrorResponse JSON
    {
      "status": 404,
      "message": "[message]: [resourceId]",
      "timestamp": "2024-05-20T10:30:15",
      "path": "/api/..."
    }
```

## 📝 메시지 형식

`ResourceNotFoundException`은 자동으로 메시지를 다음 형식으로 조합합니다:

```java
"[message]: [resourceId]"
```

**예시:**
- `ResourceNotFoundException(123, "식단을 찾을 수 없습니다")` → `"식단을 찾을 수 없습니다: 123"`
- `ResourceNotFoundException("user-key-123", "사용자 프로필을 찾을 수 없습니다")` → `"사용자 프로필을 찾을 수 없습니다: user-key-123"`

## ✅ 장점

1. **코드 중복 제거**: 모든 "Not Found" 상황을 하나의 Exception으로 처리
2. **스케일 가능**: 새로운 리소스 추가 시 Exception 클래스 생성 불필요
3. **유지보수 용이**: 일관된 패턴으로 예외 처리
4. **호환성**: 기존 코드와 새로운 코드 모두 지원

## 🚀 다음 단계

### 즉시 적용 (새 코드)
- 새로운 기능 개발 시 `ResourceNotFoundException` 사용

### 점진적 마이그레이션 (기존 코드)
- 리팩토링 시 기존 Exception들을 `ResourceNotFoundException`으로 변경
- 완전한 전환 후 레거시 Exception 클래스들 제거 검토

## 📚 참고

- **ResourceNotFoundException**: `com.fitplate.fitplateapi.exception.ResourceNotFoundException`
- **상태 코드**: 항상 404 Not Found
- **GlobalExceptionHandler**: `com.fitplate.fitplateapi.global.error.GlobalExceptionHandler`
- **ErrorResponse**: `com.fitplate.fitplateapi.global.error.ErrorResponse`

