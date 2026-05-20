# 🔧 Lombok 어노테이션 완벽 가이드

Spring Boot 프로젝트에서 자주 사용되는 **Lombok 라이브러리**에 대한 설명입니다.

> **Lombok이란?** 반복적인 Java 코드를 자동으로 생성해주는 라이브러리입니다.

---

## 📌 Lombok 핵심 어노테이션 5가지

### 1️⃣ @Getter

**역할**: getter 메서드 자동 생성

❌ **Lombok 없이**
```java
public class User {
    private String name;
    private int age;
    
    // 손으로 작성해야 함
    public String getName() {
        return this.name;
    }
    
    public int getAge() {
        return this.age;
    }
}
```

✅ **Lombok 사용**
```java
@Getter
public class User {
    private String name;
    private int age;
    // getter 메서드 자동 생성!
}

// 사용
User user = new User();
user.getName();  // ← 자동으로 생성된 메서드
```

---

### 2️⃣ @Setter

**역할**: setter 메서드 자동 생성

✅ **사용 예**
```java
@Setter
@Getter
public class User {
    private String name;
    private int age;
}

// 사용
user.setName("홍길동");
user.getAge();  // 30
```

⚠️ **주의**: Entity에서는 보통 @Setter를 사용하지 않음 (불리는 이유)

---

### 3️⃣ @NoArgsConstructor

**역할**: 파라미터 없는 생성자 자동 생성

✅ **사용 예**
```java
@NoArgsConstructor
public class User {
    private String name;
    private int age;
}

// 사용 - 파라미터 없이 생성 가능
User user = new User();
```

📌 **Entity에서 필수!**
```java
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 필수
public class MealPlan {
    // JPA가 DB에서 데이터를 가져올 때
    // 반사(Reflection)를 이용해 이 생성자로 객체 생성
}
```

---

### 4️⃣ @AllArgsConstructor

**역할**: 모든 필드를 파라미터로 하는 생성자 자동 생성

✅ **사용 예**
```java
@AllArgsConstructor
public class User {
    private String name;
    private int age;
}

// 사용 - 모든 필드 값으로 생성
User user = new User("홍길동", 30);
```

---

### 5️⃣ @Builder

**역할**: 빌더 패턴 자동 생성 (매우 편함!)

❌ **Builder 없이**
```java
// 많은 필드가 있을 때 생성자 사용은 불편
User user = new User("홍길동", 30, true, "서울", "010-1234-5678", ...);
// ↑ 각 파라미터가 뭔지 헷갈림
```

✅ **@Builder 사용**
```java
@Builder
public class User {
    private String name;
    private int age;
    private String address;
    private String phone;
}

// 사용 - 명확하고 편함!
User user = User.builder()
    .name("홍길동")
    .age(30)
    .address("서울")
    .phone("010-1234-5678")
    .build();
```

---

## 🎯 Fitplate 프로젝트에서의 사용

### MealPlanRequest (DTO)
```java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanRequest {
    // 이 4개 어노테이션으로 충분한 메서드들을 자동 생성!
    
    @NotNull  // 검증 어노테이션 (Lombok 아님)
    private Integer height;
    
    @NotNull
    private Integer weight;
}

// 사용
MealPlanRequest request = MealPlanRequest.builder()
    .height(175)
    .weight(70)
    .build();
```

### MealPlan (Entity)
```java
@Entity
@Table(name = "meal_plans")
@Getter  // 데이터 조회만 필요 (수정은 비즈니스 로직에서)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mealPlanId;
    
    private String goal;
    private Integer targetCalories;
    // 기타 필드들...
}

// 사용
MealPlan mealPlan = new MealPlan(...);  // 파라미터 생성자로 생성
String goal = mealPlan.getGoal();  // 자동 생성된 getter로 조회
```

---

## 📊 어노테이션 조합 패턴

### 패턴 1️⃣: DTO (데이터 전달 객체)
```java
@Getter           // 데이터 조회
@Builder          // 객체 생성 편의
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanRequest {
    // ...
}
```

### 패턴 2️⃣: Entity (데이터 저장)
```java
@Entity
@Getter           // 데이터만 조회 (수정은 Service에서)
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 필수
public class MealPlan {
    // ...
}
```

### 패턴 3️⃣: 간단한 POJO 클래스
```java
@Getter
@Setter
public class SimpleData {
    private String field1;
    private int field2;
}
```

---

## 🤖 자동 생성되는 코드 예시

### 원본 코드
```java
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String name;
    private int age;
}
```

### 컴파일 후 자동 생성되는 코드 (내부)
```java
public class User {
    private String name;
    private int age;
    
    // @Getter 생성
    public String getName() {
        return this.name;
    }
    
    public int getAge() {
        return this.age;
    }
    
    // @NoArgsConstructor 생성
    public User() {
    }
    
    // @AllArgsConstructor 생성
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    // @Builder 생성
    public static UserBuilder builder() {
        return new UserBuilder();
    }
    
    public static class UserBuilder {
        private String name;
        private int age;
        
        public UserBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public UserBuilder age(int age) {
            this.age = age;
            return this;
        }
        
        public User build() {
            return new User(this.name, this.age);
        }
    }
}
```

**보이나요?** Lombok 어노테이션 하나만으로 이 모든 코드가 자동 생성됩니다! 🚀

---

## 🔍 Fitplate 코드에서 실제 사용

### Controller에서 요청 받기
```java
@PostMapping
public ResponseEntity<MealPlanResponse> generateMealPlan(
    @Valid @RequestBody MealPlanRequest request  // ← Lombok의 @Getter 사용됨
)
```

Jackson이 JSON을 MealPlanRequest 객체로 변환할 때:
1. `@NoArgsConstructor`로 빈 객체 생성
2. 각 필드의 setter로 값 설정 (@Setter는 없지만 내부적으로 필요)
3. 또는 빌더 패턴으로 생성

### Service에서 데이터 사용
```java
public MealPlanResponse generateMealPlan(MealPlanRequest request) {
    // @Getter로 자동 생성된 메서드 사용
    int height = request.getHeight();
    int weight = request.getWeight();
    String goal = request.getGoal();
    // ...
}
```

### 응답 생성
```java
MealPlanResponse response = MealPlanResponse.builder()  // @Builder
    .days(mealDays)
    .build();

// 또는 Entity에서
MealPlan mealPlan = new MealPlan(...);  // @AllArgsConstructor 생성자
String goal = mealPlan.getGoal();  // @Getter
```

---

## 🚫 Lombok 사용할 때 주의사항

### 1️⃣ Entity에서는 @Setter 피하기
```java
// ❌ 나쁜 예
@Entity
@Getter
@Setter  // ← 위험! 누구나 Entity를 수정할 수 있음
public class MealPlan {
    // ...
}

// ✅ 좋은 예
@Entity
@Getter  // 조회만 가능
public class MealPlan {
    // 수정은 Service 계층의 명시적인 메서드로만
}
```

### 2️⃣ @NoArgsConstructor에 access 지정하기
```java
// ❌ 나쁜 예
@Entity
@NoArgsConstructor  // public 생성자가 노출됨
public class MealPlan {
    // ...
}

// ✅ 좋은 예
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // protected로 제한
public class MealPlan {
    // JPA만 사용 가능, 다른 곳에서는 못 씀
}
```

### 3️⃣ 순환 참조 주의
```java
// ❌ 주의!
@Data  // @Getter, @Setter, @ToString, @EqualsAndHashCode...
@Entity
public class User {
    @OneToMany
    private List<MealPlan> mealPlans;
}

@Data
@Entity
public class MealPlan {
    @ManyToOne
    private User user;
}

// User.toString() → mealPlans → MealPlan.toString() → user → ...
// 무한 루프! StackOverflowError 발생
```

---

## 💡 정리: 언제 어떤 어노테이션?

| 상황 | 추천 조합 | 이유 |
|------|---------|------|
| DTO (데이터 전달) | `@Getter` `@Builder` `@NoArgsConstructor` `@AllArgsConstructor` | 요청/응답 데이터 |
| Entity | `@Getter` `@NoArgsConstructor(access=PROTECTED)` | 데이터 보호 |
| 간단한 클래스 | `@Getter` `@Setter` | 빠른 개발 |
| JPA Repository 조회용 | `@Getter` `@AllArgsConstructor` | 읽기 전용 데이터 |

---

## 🎓 핵심 요점

```
Lombok = 반복 코드 자동 생성 도구

@Getter    → getter 메서드
@Setter    → setter 메서드
@NoArgsConstructor → 파라미터 없는 생성자
@AllArgsConstructor → 모든 파라미터 생성자
@Builder   → 빌더 패턴
```

**Lombok을 잘 사용하면 코드 라인 수를 60%까지 줄일 수 있습니다!** ✨

---

## 🔗 더 알아보기

- **Lombok 공식 홈페이지**: https://projectlombok.org/
- **Lombok 기능 전체**: https://projectlombok.org/features/all
- **Lombok 주의사항**: https://projectlombok.org/features/experimental


