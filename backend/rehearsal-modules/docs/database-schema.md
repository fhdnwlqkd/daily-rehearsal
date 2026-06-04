# Daily Rehearsal MVP Database Schema

이 문서는 Daily Rehearsal MVP에서 DB에 저장할 최소 설정 스키마를 정의한다.

MVP에서는 P1에서 채울 "맥락 slot 구조"만 DB에 둔다. 옷 리스트/옷 이미지, LLM prompt, 정량 rule, 결과지 템플릿은 DB에서 제외한다.

## 설계 원칙

- 사용자는 세션 시작 시 상황 카드를 선택하지 않는다.
- 내일의 상황 맥락은 별도 분류 호출이 아니라 `situation_type` slot으로 추출한다.
- P1에서 받아야 하는 사용자 맥락은 active `context_slot`으로 정의한다.
- LLM은 DB에 정의된 `slot_key` 기준으로 사용자 발화에서 값을 추출한다.
- MVP에서는 LLM confidence 점수를 사용하지 않는다.
- 필수 slot의 값이 `null`이거나 비어 있으면 부족한 맥락으로 판단한다.

## DB에 남기는 테이블

MVP에서 필요한 테이블은 다음 3개로 제한한다.

```text
context_slot
context_option
context_slot_option
```

## context_slot

P1에서 사용자에게 받아야 하는 맥락 항목이다. 이 테이블이 MVP 설정 DB의 핵심이다.
slot은 코드 enum이 아니라 DB 설정이다. 새 slot을 추가하거나 기존 slot을 끄면, 서버는 active slot 목록으로 LLM Structured Outputs JSON Schema를 다시 생성한다.

| 필드 | 설명 | 필요 이유 |
| --- | --- | --- |
| id | DB 내부 PK | slot을 고유하게 구분하기 위해 필요 |
| slot_key | LLM 결과와 서버 저장에 쓰는 내부 key | LLM이 반환한 값을 어떤 맥락으로 저장할지 식별하기 위해 필요 |
| label | 사람이 읽는 이름 | 관리자, 디버깅, 결과 확인에서 slot 의미를 이해하기 위해 필요 |
| slot_type | 값의 형태 | LLM 출력 형식과 객관식 option 사용 여부를 정하기 위해 필요 |
| required | 필수 여부 | 부족한 맥락인지 판단하기 위해 필요 |
| priority | 부족한 slot이 여러 개일 때 질문 우선순위 | 추가 질문은 1회만 하므로 중요한 slot을 먼저 묻기 위해 필요 |
| follow_up_question | 해당 slot이 부족할 때 사용할 추가 질문 | LLM이 질문을 마음대로 만들지 않게 하기 위해 필요 |
| default_value | 끝까지 비어 있을 때 적용할 기본값 | 1분 체험이 멈추지 않도록 fallback을 제공하기 위해 필요 |
| extraction_hint | LLM이 이 slot을 추출할 때 참고할 설명 | 비슷한 맥락끼리 혼동하지 않도록 돕기 위해 필요 |
| active | 사용 여부 | 특정 slot을 코드 수정 없이 켜고 끄기 위해 필요 |

### slot_key 예시

`slot_key`는 enum이 아니라 DB에 저장되는 문자열 key다. LLM은 이 key와 같은 이름으로 JSON을 반환한다.

P1 active slot 예:

```text
situation_type
place_context
anxiety_point
desired_persona
critical_moment
opponent_mood
outfit_direction
route_risk
change_action
conversation_style
```

### slot_type 예시

```text
TEXT
SINGLE_SELECT
MULTI_SELECT
SCALE
BOOLEAN
```

예:

```text
situation_type = SINGLE_SELECT
critical_moment = TEXT
formality_level = SINGLE_SELECT
anxiety_type = MULTI_SELECT
```

## P1 공통 active context_slot

P1 MVP에서는 상황별로 다른 질문지를 먼저 고르지 않는다. 사용자의 첫 transcript를 아래 공통 active slot으로 추출하고, `situation_type` 값에 따라 리허설 장면과 피드백 표현을 다르게 만든다.

| slot_key | 필수 | 역할 |
| --- | --- | --- |
| situation_type | true | 발표/소개팅/면접/일상 정돈 등 상황 분류 |
| critical_moment | true | 리허설할 결정적 순간 |
| desired_persona | true | 되고 싶은 태도/인상 |
| anxiety_point | false | 걱정/불안 포인트 |
| place_context | false | 장소/공간 분위기 |
| opponent_context | false | 상대/청중 분위기 |
| outfit_direction | false | 복장 방향 |
| change_action | false | 내일 바꿀 행동 |

### 충분한 맥락의 기준

MVP에서는 LLM confidence 점수를 사용하지 않는다. 충분한 맥락은 다음 기준으로 판단한다.

```text
required=true인 공통 slot이 모두 null/empty가 아니면 충분하다.
required slot 중 하나라도 null/empty이면 follow-up 후보가 된다.
follow-up은 최대 1회만 수행한다.
follow-up 이후에도 비어 있는 slot은 default_value 또는 fallback 값으로 채운다.
```

## context_option

여러 체험과 slot에서 재사용할 수 있는 객관식 선택지다.

| 필드 | 설명 | 필요 이유 |
| --- | --- | --- |
| id | DB 내부 PK | option을 고유하게 구분하기 위해 필요 |
| option_group | 선택지 묶음 이름 | 격식 수준, 장소 유형, 불안 유형처럼 option을 그룹화하기 위해 필요 |
| option_key | 내부 식별자 | LLM 결과, 서버 저장, 로그에서 같은 option을 안정적으로 식별하기 위해 필요 |
| label | 사용자/관리자에게 보이는 이름 | 화면 표시와 디버깅을 위해 필요 |
| active | 사용 여부 | 특정 option을 코드 수정 없이 숨기기 위해 필요 |
| sort_order | 표시 순서 | 같은 group 안의 option 노출 순서를 제어하기 위해 필요 |

### context_option 예시

```text
option_group=formality_level
- option_key=casual, label=캐주얼
- option_key=smart_casual, label=스마트 캐주얼
- option_key=business, label=비즈니스
- option_key=formal, label=격식 있음
```

```text
option_group=anxiety_type
- option_key=nervous, label=긴장됨
- option_key=awkward, label=어색함
- option_key=hard_question, label=예상 질문이 걱정됨
- option_key=low_energy, label=기운이 없음
```

## context_slot_option

특정 `context_slot`에서 사용할 수 있는 `context_option`을 연결하는 조인 테이블이다.

| 필드 | 설명 | 필요 이유 |
| --- | --- | --- |
| context_slot_id | context_slot FK | 어떤 slot에 연결되는 option인지 알기 위해 필요 |
| context_option_id | context_option FK | 해당 slot에서 허용할 option을 지정하기 위해 필요 |

예를 들어 `situation_type` slot에는 `presentation`, `date`, `interview`, `daily_reset` 같은 option을 연결하고, `outfit_direction` slot에는 `casual`, `smart_casual`, `business`, `formal` 같은 option을 연결할 수 있다.

## Redis 세션 저장 모델

세션 진행 상태와 프론트가 확정한 transcript/context, Decart preview metadata는 빠른 조회를 위해 Redis에 저장한다.

```json
{
  "sessionId": "uuid",
  "selectedOutfitId": "presentation_jacket_01",
  "status": "REHEARSAL_READY",
  "vtonPreviewStatus": "CONNECTED",
  "contextStatus": "COMPLETED",
  "decartPreview": {
    "sessionId": "decart-session-id",
    "model": "lucy-2.1-vton",
    "referenceImageUrl": "https://asset-store/outfits/presentation_jacket_01.png"
  },
  "transcript": "내일 팀 발표가 있는데 질문을 받으면 말이 꼬일 것 같아요.",
  "userContext": {
    "situation_type": "presentation",
    "anxiety_point": "질문을 받으면 말이 꼬일까 봐 걱정됨"
  },
  "finalUserContext": {
    "situation_type": "presentation",
    "place_context": "회의실",
    "anxiety_point": "질문을 받으면 말이 꼬일까 봐 걱정됨",
    "desired_persona": "차분하고 신뢰감 있는 태도",
    "critical_moment": "예상 질문을 받는 순간"
  },
  "followUpAttempt": 0,
  "feedbackResult": {},
  "finalResult": {}
}
```

## LLM Structured Outputs schema 생성

context 추출은 고정 DTO에 맞추지 않는다. 서버는 P1 active `context_slot`을 읽고 매 요청마다 JSON Schema를 만든다.

생성 규칙:

- `slots` object의 property는 active slot의 `slot_key`로 만든다.
- Structured Outputs에서 key 누락을 막기 위해 active slot key는 모두 `required`에 넣는다.
- 값이 비어 있을 수 있는 slot은 type을 `["string", "null"]`, `["number", "null"]`, `["boolean", "null"]`처럼 null 허용으로 만든다.
- `additionalProperties: false`를 사용해 DB에 없는 slot key가 나오지 않게 한다.
- `missingRequiredSlotKeys`는 required slot 중 값이 비어 있는 key만 담는다.
- `followUpQuestion`은 부족한 required slot이 있고 follow-up 횟수가 남아 있을 때만 문자열로 채운다.
- `readyForSimulation`은 required slot이 모두 채워졌거나 follow-up 횟수를 모두 사용했을 때 true다.

출력 envelope:

```json
{
  "slots": {
    "critical_moment": "예상 질문을 받는 순간",
    "desired_persona": "차분하고 신뢰감 있는 태도"
  },
  "missingRequiredSlotKeys": [],
  "followUpQuestion": null,
  "readyForSimulation": true
}
```

## DB에서 제외하는 항목

### 옷 리스트와 옷 이미지

옷 리스트와 옷 사진은 MVP 설정 DB에서 관리하지 않는다.

관리 위치:

```text
별도 에셋/이미지 저장소
```

세션에는 사용자가 선택한 옷의 id, reference image URL, Decart preview session metadata만 저장한다.
사용자 카메라 stream과 VTON media는 프론트에서 Decart WebRTC로 직접 전달하며 백엔드를 거치지 않는다.

### Prompt 파일

LLM의 역할, 출력 형식, 말투, 판단 방식은 DB가 아니라 prompt 파일로 관리한다.

```text
context-extraction.md
follow-up-question.md
simulation-dialogue.md
feedback-generation.md
result-card.md
```

### Code/rule

정량적으로 계산 가능한 판정은 DB나 LLM이 아니라 code/rule로 처리한다.

```text
gesture-navigation-rule
voice-volume-rule
speech-rate-rule
silence-duration-rule
response-length-rule
```

### MVP에서 보류하는 DB 테이블

아래 테이블은 지금 만들지 않는다.

```text
experience_template
simulation_scene
feedback_field
feedback_rule
vton_guide
result_template
```

보류 이유:

- 시뮬레이션 장면과 결과지 구성은 아직 자주 바뀔 가능성이 높다.
- 피드백 정량 판정은 code/rule로 두는 편이 테스트하기 쉽다.
- VTON 관련 옷 정보는 별도 에셋 관리 영역에서 다룬다.
- MVP에서는 prompt 파일과 고정 UI만으로 충분히 실험할 수 있다.

## 체험별 확장 방식

새 상황 유형을 추가할 때는 공통 파이프라인을 유지하고 slot/option 설정만 추가한다.

1. `situation_type`의 `context_option`에 새 상황 option을 추가한다.
2. 새 상황에서도 공통으로 받을 맥락은 active `context_slot`으로 유지한다.
3. 객관식이 필요한 slot은 `context_option`을 재사용하거나 새로 추가한다.
4. `context_slot_option`으로 slot과 option을 연결한다.
5. prompt 파일은 `finalUserContext.situation_type`과 active `context_slot` 값을 입력으로 받아 표현 차이를 처리한다.

코드가 달라지는 지점은 새로운 외부 API, 새로운 관찰 지표, 새로운 정량 rule처럼 파이프라인 자체가 늘어날 때로 제한한다.
