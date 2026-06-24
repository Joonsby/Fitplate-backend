# 🚀 Fitplate 빠른 시작 가이드

Spring Boot 코드 읽기를 처음 시작하는 분들을 위한 **5분 요약본**입니다.

---

## 📱 API 한 눈에 보기

### 요청
```
POST /api/meal-plan
{
  "height": 175,        // 키 (100~300 cm)
  "weight": 70,         // 체중 (20~300 kg)
  "age": 30,            // 나이 (10~120 세)
  "gender": "MALE",     // 성별 (MALE/FEMALE)
  "bodyFatRate": 20.5,  // 체지방률 (선택)
  "goal": "MUSCLE_GAIN", // 목표 (WEIGHT_LOSS/MUSCLE_GAIN/DIET_BALANCE)
}
```

### 응답
```
200 OK
{
  "days": [
    {
      "dayNumber": 1,
      "breakfast": { "name": "...", "calories": 400, "protein": 25.5, ... },
      "lunch": { "name": "...", "calories": 650, "protein": 35.0, ... },
      "dinner": { "name": "...", "calories": 550, "protein": 42.0, ... }
    },
    ...
  ]
}
```

---

## 🎯 코드 읽는 순서 (초보자용)

### 1단계: 프로젝트 시작 이해하기
📄 파일: `FitplateApiApplication.java`
```java
@SpringBootApplication  // ← 이 어노테이션이 핵심!
public class FitplateApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(FitplateApiApplication.class, args);
        // 이 한 줄로 서버가 시작됨!
    }
}
```
**핵심**: `@SpringBootApplication`이 있으면 Spring Boot 앱이 시작됩니다.

---

### 2단계: API 엔드포인트 이해하기
📄 파일: `MealPlanController.java`
```java
@RestController
@RequestMapping("/api/meal-plan")  // 기본 URL
public class MealPlanController {
    
    @PostMapping  // POST /api/meal-plan
    public ResponseEntity<MealPlanResponse> generateMealPlan(
        @Valid @RequestBody MealPlanRequest request  // 입력 검증
    ) {
        // 1. 서비스 호출
        MealPlanResponse response = mealPlanService.generateMealPlan(request);
        
        // 2. 200 OK 상태코드로 응답
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
```
**핵심**: `@PostMapping`으로 POST 요청을 받고, Service에 처리를 위임합니다.

---

### 3단계: 비즈니스 로직 이해하기
📄 파일: `MealPlanService.java`
```java
@Service
@Transactional  // DB 작업이 하나의 트랜잭션으로 처리됨
public class MealPlanService {
    
    public MealPlanResponse generateMealPlan(MealPlanRequest request) {
        // Step 1: 칼로리 계산
        int bmr = calculateBmr(request);          // 기초대사량
        int tdee = calculateTdee(bmr);            // 일일 소비 칼로리
        int targetCalories = calculateTargetCalories(tdee, request.getGoal());
        
        // Step 2: 영양소 계산
        int proteinGram = calculateProteinGram(request);
        int fatGram = calculateFatGram(targetCalories);
        int carbsGram = calculateCarbsGram(targetCalories, proteinGram, fatGram);
        
        // Step 3: AI 호출해서 식단 생성
        MealPlanResponse response = geminiMealPlanClient.generateMealPlan(request);
        
        // Step 4: 사용자 정보 조회/생성
        User user = userRepository.findByTossUserKey(MOCK_USER_KEY)
                .orElseGet(() -> userRepository.save(new User(...)));
        
        // Step 5: MealPlan 엔티티 생성 후 DB에 저장
        MealPlan mealPlan = new MealPlan(...);
        mealPlanRepository.save(mealPlan);
        
        // Step 6: 클라이언트에 응답 반환
        return response;
    }
}
```
**핵심**: Service는 모든 비즈니스 로직을 담합니다.

---

### 4단계: 데이터 모델 이해하기
📄 파일: `MealPtan.java`
```java
@Entity  // 이 클래스가 DB 테이블과 연결됨
@Table(name = "meal_plans")
public class MealPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mealPlanId;  // Primary Key, 자동증가 (1, 2, 3, ...)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // User와 다대일 관계
    
    private String goal;  // WEIGHT_LOSS 등
    private Integer targetCalories;  // 목표 칼로리
    private Integer bmr;  // 기초대사량
    private Integer tdee;  // 일일 소비 칼로리
    private Integer proteinGram;  // 단백질
    // ... 기타 필드들
}
```
**핵심**: Entity는 DB 테이블의 각 열(Column)을 필드로 가집니다.

---

### 5단계: 데이터 요청/응답 이해하기
📄 파일: `MealPlanRequest.java`, `MealPlanResponse.java`

**Request (클라이언트 → 서버)**
```java
public class MealPlanRequest {
    @NotNull
    @Min(value = 100)
    @Max(value = 300)
    private Integer height;  // 입력 검증
}
```

**Response (서버 → 클라이언트)**
```java
public class MealPlanResponse {
    private List<MealDayResponse> days;  // 각 날짜의 식단
}
```
**핵심**: DTO는 계층 간 데이터 전달 객체입니다.

---

## 🔄 실행 흐름 (가장 중요!)

```
❶ 클라이언트가 JSON 데이터와 함께 POST 요청 전송
   └─ URL: /api/meal-plan
   └─ Body: { height: 175, weight: 70, ... }

❷ Controller가 요청을 받음
   └─ @Valid로 입력값 검증 (키는 100~300, 체중은 20~300 등)
   └─ 검증 실패 → 400 Bad Request 반환 (GlobalExceptionHandler)
   └─ 검증 성공 → Service 호출

❸ Service가 비즈니스 로직 실행
   └─ 칼로리 계산 (BMR, TDEE, 목표 칼로리)
   └─ 영양소 계산 (단백질, 탄수화물, 지방)
   └─ Gemini AI API 호출 → 식단 생성 받음
   └─ Repository를 통해 DB에 저장
   └─ 응답 데이터 반환

❹ Controller가 응답을 받아 클라이언트에 반환
   └─ 200 OK + JSON 데이터

❺ 에러 발생 시?
   └─ GlobalExceptionHandler가 예외 처리
   └─ 통일된 형식의 에러 응답 반환 (ErrorResponse)
```

---

## 🗂️ 파일 위치 참고

```
src/main/java/com/fitplate/fitplateapi/

FitplateApiApplication.java
├─ main이 여기 있음

mealplan/
├─ controller/MealPlanController.java   ← 요청 받기
├─ service/MealPlanService.java         ← 로직 처리
├─ domain/MealPlan.java                 ← DB 연결
├─ dto/
│  ├─ MealPlanRequest.java  (입력)
│  ├─ MealPlanResponse.java  (출력)
│  └─ MealDayResponse.java, MealResponse.java
└─ repository/MealPlanRepository.java    ← DB 접근

user/
├─ domain/User.java
└─ repository/UserRepository.java

ai/
└─ GeminiMealPlanClient.java    ← AI API 호출

global/
├─ cors/CorsConfig.java         ← CORS 설정
└─ error/
   ├─ GlobalExceptionHandler.java
   └─ ErrorResponse.java
```

---

## 💻 주요 코드 패턴

### 1️⃣ 의존성 주입 (Dependency Injection)
```java
@RestController
public class MealPlanController {
    private final MealPlanService mealPlanService;  // ← 주입받을 필드
    
    // 생성자를 통해 Spring이 자동으로 주입
    public MealPlanController(MealPlanService mealPlanService) {
        this.mealPlanService = mealPlanService;
    }
}
```

### 2️⃣ Spring Data JPA 쿼리
```java
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    // 메서드명으로 쿼리 자동 생성!
    // (SQL을 손으로 작성할 필요 없음)
    List<MealPlan> findByUserOrderByCreatedAtDesc(User user);
    //              ↑ 자동으로 WHERE user_id = ? ORDER BY created_at DESC
}
```

### 3️⃣ 트랜잭션 관리
```java
@Transactional  // ← 이 메서드의 모든 DB 작업은 하나로 묶임
public MealPlanResponse generateMealPlan(MealPlanRequest request) {
    // ...
    // 만약 도중에 예외 발생 → 모든 DB 작업 롤백 (All or Nothing)
}
```

### 4️⃣ 예외 처리
```java
@RestControllerAdvice  // 전역 예외 처리
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)  // 특정 예외 처리
    public ResponseEntity<ErrorResponse> handleValidationException(...) {
        // 400 Bad Request 반환
    }
}
```

---

## 🎓 반드시 알아야 할 5가지

1. **@SpringBootApplication**: 앱 시작점
2. **@RestController**: REST API 컨트롤러
3. **@Service**: 비즈니스 로직
4. **@Repository**: 데이터 접근
5. **@Entity**: DB 테이블과 연결

---

## 🚀 다음 단계

1. IDE에서 `MealPlanController.java` 열어서 코드 읽기
2. `MealPlanService.java`의 계산 로직 이해하기
3. `MealPlan.java` Entity의 필드 인식하기
4. 실제로 프로젝트를 실행해서 API 호출해보기
5. 응답 JSON 구조 확인하기

---

## 📚 추가 학습 자료

- **Spring Boot 공식 문서**: https://spring.io/projects/spring-boot
- **JPA 가이드**: https://spring.io/projects/spring-data-jpa
- **REST API 설계**: https://restfulapi.net/
- **HTTP 상태 코드**: https://developer.mozilla.org/ko/docs/Web/HTTP/Status

---

**이 가이드로 기본을 이해하고, 코드의 상세 주석을 읽으며 학습하세요!** 💪

