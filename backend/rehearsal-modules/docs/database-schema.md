# Daily Rehearsal State and Static Config

이 문서는 현재 목표 플로우에서 어떤 데이터를 코드 enum/정적 매핑으로 둘지, 어떤 데이터를 세션 저장소에 누적할지 정리한다.

현재 방향은 전시용 단일 플로우다. 운영 중 화면이 멈추지 않는 것이 우선이므로, slot 질문과 default는 코드에서 고정하고 AI는 답변 해석과 정규화만 담당한다.

## 1. 저장 전략

### 정적 설정

다음 값은 enum 또는 정적 매핑으로 관리한다.

- `SituationType`: 소개팅, 발표, 미팅 등 타입 목록
- 타입별 브리핑 문구와 예시 답변
- 타입별 slot 목록
- slot별 질문 문구
- slot별 default 값
- slot별 값 타입
- 타입별 outfit 목록
- outfit별 thumbnail, displayName, Decart parameter
- 타입별 시뮬레이션 턴 수 `N`
- 타입별 첫 상대 발화 fallback
- 타입별 다음 발화 fallback

정적 설정으로 두는 이유:

- 전시 당일 운영 중 설정 DB 장애 영향을 줄인다.
- 질문 문구를 AI가 임의 생성하지 않게 한다.
- fallback을 명확하게 보장한다.
- 상황 타입 수와 slot 수가 작아 코드 관리 비용이 낮다.

### 세션/컨텍스트 저장소

다음 값은 세션 단위로 저장한다.

- `sessionId`
- `status`
- `situationType`
- `attempt`
- `partialContext`
- `finalContext`
- `missingSlotKeys`
- `selectedOutfitId`
- `simulationTurn`
- `conversationHistory`
- `turnEvaluations`
- `videoUrl`
- `ticket`
- `downloadUrl`

하루용 단일 인스턴스 전시라면 인메모리 저장소나 단순 DB 하나로 충분하다. 여러 인스턴스나 재시작 복구가 필요하면 Redis 또는 RDB를 사용한다.

## 2. SituationType 예시

```text
date
presentation
meeting
interview
important_conversation
daily_reset
```

각 타입은 다음 속성을 가진다.

```json
{
  "key": "date",
  "label": "소개팅",
  "gestureOrder": 1,
  "briefingTitle": "내일의 소개팅을 짧게 말해주세요",
  "exampleAnswers": [
    "내일 소개팅이 있는데 첫 인사가 어색할까 봐 걱정돼요."
  ]
}
```

## 3. Slot Definition

slot은 타입별로 필요한 context 항목이다.

slot 정의는 다음 정보를 가진다.

| 필드 | 설명 |
| --- | --- |
| `slotKey` | context 저장 key |
| `valueType` | `TEXT`, `SINGLE_SELECT` 등 값 타입 |
| `required` | 시뮬레이션 시작 전 필수 여부 |
| `question` | 빈 값일 때 사용자에게 보여줄 고정 질문 |
| `defaultValue` | AI 실패 또는 attempt 초과 시 채울 값 |
| `options` | `SINGLE_SELECT`일 때 허용 값 |

예시:

```json
{
  "slotKey": "desired_persona",
  "valueType": "SINGLE_SELECT",
  "required": true,
  "question": "내일 어떤 모습으로 보이고 싶나요?",
  "defaultValue": "calm_confident",
  "options": ["calm_confident", "warm_natural", "sharp_prepared"]
}
```

## 4. Context 수집 규칙

1. 세션 시작 시 `situation_type`은 타입 선택 결과로 고정한다.
2. 브리핑 transcript를 AI에 보내 slot 값을 JSON으로 정규화한다.
3. AI 응답에서 채워진 slot은 세션 context에 누적한다.
4. 빈 required slot이 있으면 해당 slot의 고정 질문을 리스트로 반환한다.
5. 사용자는 재질문 리스트에 한 번에 답변한다.
6. 한 답변에서 여러 slot이 채워질 수 있다.
7. `max_attempt`는 재질문 라운드 전체 횟수다.
8. `max_attempt`를 넘으면 남은 빈 slot은 default로 채운다.
9. AI 호출 실패 시 target slot은 default로 채운다.

## 5. Outfit Static Config

outfit은 타입과 context에 따라 노출할 수 있는 정적 후보 목록이다.

```json
{
  "outfitId": "date_neat_casual_01",
  "situationTypes": ["date"],
  "displayName": "단정한 캐주얼",
  "thumbnailUrl": "/assets/outfits/date_neat_casual_01.png",
  "decartParams": {
    "prompt": "neat casual outfit for a warm first impression",
    "referenceImageKey": "date_neat_casual_01"
  },
  "sortOrder": 1
}
```

첫 번째 outfit은 default로 즉시 적용한다.

## 6. Simulation State

시뮬레이션은 고정 턴 수 `N`을 가진다.

세션에는 다음 정보를 누적한다.

```json
{
  "currentTurn": 2,
  "maxTurn": 3,
  "conversationHistory": [
    {
      "turnNo": 1,
      "opponentLine": "처음 뵙네요. 오는 길 괜찮으셨어요?",
      "userTranscript": "네, 조금 일찍 나와서 여유 있게 도착했어요.",
      "success": true,
      "feedback": "첫 문장이 짧고 자연스럽습니다."
    }
  ]
}
```

LLM은 stateless로 보고 매 호출마다 전체 context와 history를 전달한다.

## 7. Ticket State

티켓은 최종 결과물이다.

```json
{
  "ticketId": "uuid",
  "sessionId": "uuid",
  "videoUrl": "https://...",
  "downloadUrl": "https://...",
  "ticket": {
    "situationLabel": "소개팅",
    "selectedOutfitName": "단정한 캐주얼",
    "summary": "처음 인사를 자연스럽게 시작하기"
  }
}
```

QR에는 `downloadUrl`을 인코딩한다.
