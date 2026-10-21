// 사용자가 선호·제외로 고를 수 있는 음식 분류와 음식 이름 판별 키워드입니다.
package com.fitplate.fitplateapi.user.domain;

import java.util.List;

public enum FoodCategory {
    PORK("돼지고기", "돼지", "삼겹", "목살", "항정", "제육", "돈까스", "돈가스", "베이컨", "햄", "소시지", "수육", "보쌈", "족발", "순대"),
    BEEF("소고기", "소고기", "쇠고기", "한우", "우둔", "홍두깨", "차돌", "사태", "양지", "육회", "소불고기"),
    CHICKEN("닭고기", "닭", "치킨"),
    SEAFOOD("생선·해산물", "생선", "연어", "고등어", "참치", "새우", "오징어", "조개", "굴", "명태", "황태", "북어",
            "대구", "갈치", "꽁치", "삼치", "광어", "가자미", "전복", "문어", "낙지", "홍합", "꼬막", "게살", "대게",
            "멸치", "어묵", "해물", "해산물"),
    EGG("달걀", "달걀", "계란", "메추리알", "에그"),
    DAIRY("우유·유제품", "우유", "요거트", "요구르트", "치즈", "버터", "크림", "유제품", "라떼", "그릭"),
    SOY("콩·두부", "두부", "콩", "두유", "된장", "청국장", "낫또", "에다마메"),
    NUTS("견과류", "견과", "아몬드", "호두", "땅콩", "캐슈", "피스타치오", "잣", "마카다미아", "피칸", "헤이즐넛"),
    WHEAT("밀가루·빵", "밀가루", "빵", "토스트", "파스타", "스파게티", "국수", "라면", "우동", "면", "시리얼", "베이글",
            "또띠아", "토르티야", "크래커", "통밀");

    private final String label;
    private final List<String> keywords;

    FoodCategory(String label, String... keywords) {
        this.label = label;
        this.keywords = List.of(keywords);
    }

    public String getLabel() {
        return label;
    }

    /** 음식 이름이나 검색 키워드에 이 분류의 키워드가 들어 있으면 true. 공백은 무시한다. */
    public boolean matches(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replace(" ", "");
        return keywords.stream().anyMatch(normalized::contains);
    }
}
