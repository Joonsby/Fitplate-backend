# Fitplate 프로젝트 흐름 가이드

Spring Boot 초보자를 위한 상세한 프로젝트 흐름 설명입니다. 😊

## 📌 프로젝트 개요

**Fitplate**는 사용자의 신체 정보(키, 체중, 나이, 성별)와 목표(체중감량, 근육증가 등)를 받아
Google Gemini AI를 이용해 맞춤형 한국식 식단을 생성해주는 Spring Boot 백엔드 프로젝트입니다.

---

## 🔄 API 요청 흐름 (가장 중요!)

```
┌─────────────────────┐
│  클라이언트 (프론트엔드)  │
│  React 앱            │  
└──────────┬───────────┘
           │ HTTP POST 요청
           │ URL: http://localhost:8080/api/meal-plan
           │ Body: { height: 175, weight: 70, age: 30, ... }
           ▼
┌─────────────────────────────────────────────────────────┐
│  Controller (MealPlanController)                         │
│  역할: HTTP 요청 받기 + 데이터 검증                        │
│  1. @PostMapping("/") 어노테이션으로 POST 요청 감지       │
│  2. @Valid로 입력 데이터 검증 (키: 100~300, 체중: 20~300) │
│  3. Service에서 비즈니스 로직 처리                       │
└──────────┬───────────────────────────────────────────────┘
           │ 검증된 데이터를 service에 전달
           ▼
┌─────────────────────────────────────────────────────────┐
│  Service (MealPlanService)                              │
│  역할: 비즈니스 로직 처리                                  │
│                                                           │
│  실행 단계:                                               │
│  1️⃣  BMR 계산 (기초대사량)                                │
│  2️⃣  TDEE 계산 (일일 소비 칼로리)                         │
│  3️⃣  목표에 따른 칼로리 조정 (체중감량: -400, 근육증가: +300) │
│  4️⃣  영양소 분배 (단백질, 탄수화물, 지방)                  │
│  5️⃣  Gemini AI에 요청 (식단 생성)                         │
│  6️⃣  User DB에서 사용자 조회 (없으면 생성)                │
│  7️⃣  MealPlan DB에 저장                                   │
│  8️⃣  결과 반환                                            │
└──────────┬─────────────────────────────────────────────┬─┘
           │                                          │
           │ (필요시)                                │
           ▼                                          ▼
┌──────────────────────┐                ┌──────────────────────┐
│  Gemini AI Client    │                │  Repository (DB)     │
│  (GeminiMealPlanClient)              │  - MealPlanRepository│
│                      │                │  - UserRepository    │
│  역할:               │                │                      │
│  - AI에 프롬프트 전송 │                │  역할:               │
│  - 식단 생성 요청     │                │  - DB와 상호작용     │
│  - JSON 응답 수신    │                │  - 저장, 조회 등     │
│                      │                │                      │
│ http://...google.api │                │ MySQL (meal_plans,   │
│                      │                │        users, ...)   │
└──────────┬───────────┘                └──────────┬───────────┘
           │ 식단 데이터 반환                       │ 데이터 저장/조회
           │ { days: [...] }                     │ 성공/실패
           └────────────────────┬──────────────────┘
                                │
                                ▼
                        ┌────────────────┐
                        │  결과 반환      │
                        │  200 OK        │
                        │  Body: 식단 데이터
                        └────────┬───────┘
                                 │
                                 ▼
                        ┌─────────────────┐
                        │  클라이언트 수신  │
                        │  (프론트엔드)    │
                        │  화면에 표시    │
                        └─────────────────┘
```

---

## 📂 프로젝트 폴더 구조 및 각 역할

```
src/main/java/com/fitplate/fitplateapi/
│
├─ FitplateApiApplication.java
│  ├─ 설명: Spring Boot 애플리케이션 시작점
│  ├─ 역할: main() 메서드에서 애플리케이션 실행
│  └─ 주석: 상세한 설명 포함 ✅
│
├─ mealplan/
│  ├─ controller/
│  │  └─ MealPlanController.java
│  │     ├─ 설명: HTTP API 엔드포인트
│  │     ├─ endpoint: POST /api/meal-plan
│  │     ├─ 역할: 요청 받기 + 검증 + Service 호출
│  │     └─ 주석: API 흐름 상세 설명 ✅
│  │
│  ├─ service/
│  │  └─ MealPlanService.java
│  │     ├─ 설명: 비즈니스 로직
│  │     ├─ 역할: 칼로리 계산 + AI 호출 + DB 저장
│  │     ├─ 메서드:
│  │     │  - generateMealPlan(): 전체 로직 실행
│  │     │  - calculateBmr(): 기초대사량 계산
│  │     │  - calculateTdee(): 일일 소비 칼로리 계산
│  │     │  - calculateTargetCalories(): 목표 칼로리 조정
│  │     │  - calculateProteinGram(): 단백질 계산
│  │     │  - calculateFatGram(): 지방 계산
│  │     │  - calculateCarbsGram(): 탄수화물 계산
│  │     └─ 주석: 각 계산 공식 상세 설명 ✅
│  │
│  ├─ domain/
│  │  └─ MealPlan.java
│  │     ├─ 설명: 식단 데이터 Entity (DB 테이블과 연결)
│  │     ├─ 필드:
│  │     │  - mealPlanId: PK (자동증가)
│  │     │  - user: 사용자 (FK)
│  │     │  - goal: 목표
│  │     │  - durationDays: 기간
│  │     │  - height, weight, age, gender: 신체정보
│  │     │  - targetCalories, bmr, tdee: 칼로리 정보
│  │     │  - proteinGram, carbsGram, fatGram: 영양소
│  │     │  - aiResponseJson: AI 응답 (JSON)
│  │     │  - startedAt, expiresAt: 시작/종료 시간
│  │     │  - createdAt, updatedAt: 생성/수정 시간 (자동)
│  │     └─ 주석: 각 필드 상세 설명 ✅
│  │
│  ├─ dto/
│  │  ├─ MealPlanRequest.java
│  │  │  └─ 설명: 클라이언트 요청 데이터 (입력)
│  │  │     필드: height, weight, age, gender, bodyFatRate, goal, periodDays
│  │  │     검증: @NotNull, @Min, @Max 등
│  │  │     주석: ✅
│  │  │
│  │  ├─ MealPlanResponse.java
│  │  │  └─ 설명: API 응답 데이터 (출력)
│  │  │     필드: days (List<MealDayResponse>)
│  │  │     주석: ✅
│  │  │
│  │  ├─ MealDayResponse.java
│  │  │  └─ 설명: 하루 식단
│  │  │     필드: dayNumber, breakfast, lunch, dinner
│  │  │     주석: ✅
│  │  │
│  │  └─ MealResponse.java
│  │     └─ 설명: 한 끼니 음식 정보
│  │        필드: name, calories, protein, carbohydrate, fat
│  │        주석: ✅
│  │
│  └─ repository/
│     └─ MealPlanRepository.java
│        ├─ 설명: 식단 데이터 접근 객체
│        ├─ 역할: DB 조회/저장/수정/삭제
│        ├─ 메서드:
│        │  - save(): INSERT/UPDATE
│        │  - findByUserOrderByCreatedAtDesc(): 특정 사용자 식단 조회
│        │  - findFirstByUserAndExpiresAtAfterOrderByCreatedAtDesc(): 유효한 식단 조회
│        └─ 주석: 각 메서드 상세 설명 ✅
│
├─ user/
│  ├─ domain/
│  │  └─ User.java
│  │     ├─ 설명: 사용자 데이터 Entity
│  │     ├─ 필드:
│  │     │  - userId: PK (자동증가)
│  │     │  - tossUserKey: Toss 사용자 키 (UNIQUE)
│  │     │  - nickname: 닉네임
│  │     │  - createdAt, updatedAt: 생성/수정 시간
│  │     └─ 주석: ✅
│  │
│  └─ repository/
│     └─ UserRepository.java
│        ├─ 설명: 사용자 데이터 접근 객체
│        ├─ 메서드:
│        │  - findByTossUserKey(): Toss 키로 사용자 조회
│        └─ 주석: ✅
│
├─ ai/
│  └─ GeminiMealPlanClient.java
│     ├─ 설명: Google Gemini AI와 통신
│     ├─ 역할:
│     │  1. 프롬프트 생성 (사용자 정보 포함)
│     │  2. JSON 스키마 정의 (AI 응답 형식 강제)
│     │  3. HTTP POST 요청으로 Gemini API 호출
│     │  4. 응답 파싱 및 변환
│     ├─ 메서드:
│     │  - generateMealPlan(): 전체 로직
│     │  - buildPrompt(): 프롬프트 생성
│     │  - buildMealPlanSchema(): JSON 스키마 정의
│     └─ 주석: 각 단계 상세 설명 ✅
│
└─ global/
   ├─ cors/
   │  └─ CorsConfig.java
   │     ├─ 설명: CORS(Cross-Origin Resource Sharing) 설정
   │     ├─ 역할: 프론트엔드에서 백엔드 API 호출 허용
   │     ├─ 설정:
   │     │  - /api/** 경로 허용
   │     │  - localhost:5173 도메인 허용
   │     │  - GET, POST, PUT, DELETE, PATCH 허용
   │     └─ 주석: CORS 개념 및 설정 상세 설명 ✅
   │
   └─ error/
      ├─ GlobalExceptionHandler.java
      │  ├─ 설명: 전역 예외 처리
      │  ├─ 역할: 모든 에러를 통일된 형식으로 처리
      │  ├─ 처리 예외:
      │  │  - MethodArgumentNotValidException: 입력 검증 실패 (400)
      │  │  - Exception: 기타 모든 예외 (500)
      │  └─ 주석: ✅
      │
      └─ ErrorResponse.java
         ├─ 설명: 에러 응답 DTO
         ├─ 필드: status, message, timestamp, path
         └─ 주석: ✅
```

---

## 🔐 데이터베이스 테이블

### users 테이블
```sql
CREATE TABLE users (
  user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  toss_user_key VARCHAR(100) NOT NULL UNIQUE,
  nickname VARCHAR(50),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);
```

### meal_plans 테이블
```sql
CREATE TABLE meal_plans (
  meal_plan_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  goal VARCHAR(20) NOT NULL,
  duration_days INT NOT NULL,
  height_cm INT NOT NULL,
  weight_kg INT NOT NULL,
  age INT NOT NULL,
  gender VARCHAR(10) NOT NULL,
  target_calories INT NOT NULL,
  bmr INT NOT NULL,
  tdee INT NOT NULL,
  protein_gram INT NOT NULL,
  carbs_gram INT NOT NULL,
  fat_gram INT NOT NULL,
  ai_response_json JSON NOT NULL,
  started_at DATETIME NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

---

## 💡 핵심 개념 설명

### 1️⃣ MVC 패턴
- **M (Model)**: Entity/Domain + DTO (데이터 구조)
- **V (View)**: 프론트엔드 (React)
- **C (Controller)**: MealPlanController (요청/응답 처리)

### 2️⃣ 계층 구조
```
Controller (요청 받기)
    ↓
Service (비즈니스 로직)
    ↓
Repository (데이터 접근)
    ↓
Database
```

### 3️⃣ 의존성 주입 (Dependency Injection)
```java
// ❌ 안 좋은 방식
private MealPlanService service = new MealPlanService();

// ✅ 좋은 방식 (Spring이 자동으로 주입)
private final MealPlanService service;
public MealPlanController(MealPlanService service) {
    this.service = service;
}
```

### 4️⃣ Transactional
```java
@Transactional  // 이 메서드의 모든 DB 작업은 하나의 트랜잭션
public MealPlanResponse generateMealPlan(...) {
    // 만약 도중에 예외 발생하면 모든 DB 작업 롤백 (All or Nothing)
}
```

### 5️⃣ @Valid 검증
```java
@PostMapping
public ResponseEntity<MealPlanResponse> generateMealPlan(
    @Valid @RequestBody MealPlanRequest request  // 자동 검증
)
```

---

## 🧮 칼로리 계산 공식

### BMR (기초대사량) - Mifflin-St Jeor 공식
```
남성: 10*체중 + 6.25*키 - 5*나이 + 5
여성: 10*체중 + 6.25*키 - 5*나이 - 161
```

### TDEE (일일 총 소비 칼로리)
```
TDEE = BMR × 활동계수 (기본값 1.35)
```

### 목표 칼로리
```
체중감량: TDEE - 400 (주당 약 0.5kg 감량)
근육증가: TDEE + 300 (근육 성장)
유지: TDEE
```

### 영양소 분배
```
단백질: 체중(kg) × 1.6g
지방: 목표칼로리 × 25% ÷ 9 (1g = 9kcal)
탄수화물: (목표칼로리 - 단백질칼로리 - 지방칼로리) ÷ 4 (1g = 4kcal)
```

---

## 🚀 예시: API 호출부터 응답까지

### 1. 클라이언트 요청
```json
POST /api/meal-plan
Content-Type: application/json

{
  "height": 175,
  "weight": 70,
  "age": 30,
  "gender": "MALE",
  "bodyFatRate": 20.5,
  "goal": "MUSCLE_GAIN",
  "periodDays": 7
}
```

### 2. Controller에서 처리
1. @Valid로 입력 검증 통과 ✅
2. MealPlanService.generateMealPlan() 호출

### 3. Service에서 처리
- BMR = 10*70 + 6.25*175 - 5*30 + 5 = **1649 kcal**
- TDEE = 1649 * 1.35 = **2226 kcal**
- 목표 칼로리 = 2226 + 300 = **2526 kcal**
- 단백질 = 70 * 1.6 = **112g**
- 지방 = 2526 * 0.25 / 9 = **70g**
- 탄수화물 = (2526 - 112*4 - 70*9) / 4 = **262g**

### 4. Gemini AI 호출
프롬프트 전송 → AI가 한국식 식단 생성 → JSON 응답

### 5. DB 저장
MealPlan 엔티티를 DB에 저장

### 6. 클라이언트 응답
```json
200 OK
{
  "days": [
    {
      "dayNumber": 1,
      "breakfast": {
        "name": "계란말이, 흰쌀밥, 미역국",
        "calories": 400,
        "protein": 25.5,
        "carbohydrate": 45.0,
        "fat": 12.0
      },
      "lunch": {
        "name": "불고기덮밥, 미소된장국",
        "calories": 650,
        "protein": 35.0,
        "carbohydrate": 70.0,
        "fat": 20.0
      },
      "dinner": {
        "name": "생선까스, 감자튀김, 샐러드",
        "calories": 550,
        "protein": 42.0,
        "carbohydrate": 45.0,
        "fat": 15.0
      }
    },
    ...
  ]
}
```

---

## 🛠️ 주요 Spring Boot 어노테이션

| 어노테이션 | 역할 | 예시 |
|-----------|------|------|
| `@SpringBootApplication` | 앱 시작점 표시 | 메인 클래스 |
| `@RestController` | REST API 컨트롤러 | MealPlanController |
| `@RequestMapping` | 기본 URL | `/api/meal-plan` |
| `@PostMapping` | POST 메서드 매핑 | `/` |
| `@RequestBody` | 요청 본문 매핑 | `MealPlanRequest` |
| `@Valid` | 입력 검증 | `@Valid @RequestBody` |
| `@Service` | 비즈니스 로직 | MealPlanService |
| `@Component` | Spring 빈 등록 | GeminiMealPlanClient |
| `@Repository` | 데이터 접근 | MealPlanRepository |
| `@Entity` | DB 엔티티 | MealPlan |
| `@Table` | 테이블명 | `@Table(name="meal_plans")` |
| `@Id` | Primary Key | `mealPlanId` |
| `@GeneratedValue` | 자동증가 | `GenerationType.IDENTITY` |
| `@Column` | 컬럼 속성 | `nullable=false` |
| `@ManyToOne` | 다대일 관계 | User와 MealPlan |
| `@JoinColumn` | Foreign Key | `user_id` |
| `@Transactional` | 트랜잭션 관리 | `generateMealPlan()` |
| `@Configuration` | 설정 클래스 | CorsConfig |
| `@Value` | 설정값 주입 | `${gemini.api-key}` |
| `@ExceptionHandler` | 예외 처리 | 에러 응답 |
| `@LoadArgsConstructor` (Lombok) | 생성자 자동생성 | 코드 단축 |
| `@Getter` (Lombok) | Getter 자동생성 | getter 메서드 제거 |
| `@Builder` (Lombok) | 빌더 패턴 | 객체 생성 편의 |

---

## 🔗 데이터 흐름 요약

```
사용자 요청
  ↓
Controller: 검증 (@Valid)
  ↓
Service: 계산 + AI 호출
  ↓
Repository: DB 저장
  ↓
응답 반환
```

---

## 📝 모든 파일에 추가된 주석

프로젝트의 **모든 주요 Java 파일**에 상세한 주석이 추가되었습니다:

✅ FitplateApiApplication.java
✅ MealPlanController.java
✅ MealPlanService.java
✅ MealPlan.java
✅ User.java
✅ MealPlanRequest.java
✅ MealPlanResponse.java
✅ MealDayResponse.java
✅ MealResponse.java
✅ MealPlanRepository.java
✅ UserRepository.java
✅ GlobalExceptionHandler.java
✅ ErrorResponse.java
✅ CorsConfig.java
✅ GeminiMealPlanClient.java

각 파일을 IDE에서 열어 코드를 읽어보면, 각 라인이 무엇을 하는지 자세히 이해할 수 있습니다! 🎓

---

## 🎯 추가 학습 포인트

1. **ORM (Object-Relational Mapping)**: JPA가 Java 객체와 DB 테이블을 자동으로 매핑
2. **REST API**: HTTP 메서드(GET, POST, PUT, DELETE)를 이용한 리소스 조작
3. **DTO 패턴**: 계층 간 데이터 전달을 위한 객체
4. **Validation**: 입력 데이터의 유효성 검증
5. **Exception Handling**: 예외 처리 및 에러 응답

---

**이 문서와 코드의 주석을 함께 읽으며 학습하시면 Spring Boot의 기본 구조를 확실히 이해할 수 있습니다!** 💪

