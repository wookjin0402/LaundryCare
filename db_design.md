# Laundry Care 데이터베이스 구조 설계도

### 1. Users (사용자 정보)
- email: 타입(String), 사용자 이메일 주소
- nickname: 타입(String), 사용자 닉네임
- createdAt: 타입(Timestamp), 계정 생성 일시

### 2. Clothes (의류 및 스캔 정보)
- clothesId: 타입(String), 의류 고유 번호
- ownerId: 타입(String), 소유자 이름표 (Users의 uid와 연결)
- category: 타입(String), 의류 종류 (상의, 하의 등)
- imageUrl: 타입(String), 의류 전체 사진 경로
- labelImageUrl: 타입(String), 세탁 라벨 근접 사진 경로
- material: 타입(String), 의류 소재 (면, 울, 폴리 등)
- laundrySymbols: 타입(Array), 인식된 세탁 기호 ID 리스트
- wearCount: 타입(Number), 마지막 세탁 후 착용 횟수
- lifeScore: 타입(Number), 의류 상태 점수 (0~100)
- lastWashedAt: 타입(Timestrap), 최종 세탁 완료 일시
- status: 타입(String), 정상, 세탁필요, 관리주의

### 3. LaundryGuide (의류 관리법 사전 가이드)
- name: 타입(String), 기호 명칭 (예: 손세탁)
- description: 타입(String), 상세 세탁 방법 안내
- category: 타입(String), 세탁, 건조, 다림질, 표백 등 분류
- tips: 타입(String), 소재별 관리 꿀팁
- caution: 타입(String), 절대로 해서는 안 되는 주의사항
- imageUrl: 타입(String), 해당 세탁 기호의 아이콘 이미지 주소