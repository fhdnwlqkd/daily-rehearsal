# Daily Rehearsal MVP Database Schema

이 문서는 Daily Rehearsal MVP에서 DB에 저장할 최소 설정 스키마를 정의한다.

MVP에서는 체험별로 달라지는 "맥락 수집 구조"만 DB에 둔다. 옷 리스트/옷 이미지, LLM prompt, 정량 rule, 결과지 템플릿은 DB에서 제외한다.

## 설계 원칙

- 사용자는 세션 시작 시 하나의 체험 type을 선택한다.
- 선택된 `experienceType`은 세션이 끝날 때까지 유지된다.
- 체험 type별로 받아야 하는 사용자 맥락은 `context_slot`으로 정의한다.
- LLM은 DB에 정의된 `slot_key` 기준으로 사용자 발화에서 값을 추출한다.
- MVP에서는 LLM confidence 점수를 사용하지 않는다.
- 필수 slot의 값이 `null`이거나 비어 있으면 부족한 맥락으로 판단한다.

## DB에 남기는 테이블

MVP에서 필요한 테이블은 다음 4개로 제한한다.

```text
experience_type
context_slot
context_option
context_slot_option
```

## experience_type

사용자가 처음 선택하는 체험 종류이다. 예를 들면 발표, 소개팅, 면접 같은 단위다.

| 필드 | 설명 | 필요 이유 |
| --- | --- | --- |
| id | DB 내부 PK | 다른 테이블에서 체험 type을 참조하기 위해 필요 |
| code | 내부 식별자 | API, Redis, LLM prompt, 로그에서 같은 체험을 안정적으로 식별하기 위해 필요 |
| display_name | 사용자에게 보이는 이름 | 체험 선택 화면에 표시하기 위해 필요 |
| active | 노출 여부 | 개발 중인 체험을 숨기거나 비활성화하기 위해 필요 |
| sort_order | 표시 순서 | 체험 선택 화면의 순서를 제어하기 위해 필요 |

### code 예시

`code`는 사용자에게 보이는 문구가 아니라 시스템 내부에서 사용하는 안정적인 이름이다.

```text
PRESENTATION
DATE
INTERVIEW
DAILY_RESET
```

예를 들어 사용자에게 보이는 이름이 `발표`에서 `발표 리허설`로 바뀌어도 `code=PRESENTATION`은 유지한다.

## context_slot

체험 type별로 사용자에게 받아야 하는 맥락 항목이다. 이 테이블이 MVP 설정 DB의 핵심이다.

| 필드 | 설명 | 필요 이유 |
| --- | --- | --- |
| id | DB 내부 PK | slot을 고유하게 구분하기 위해 필요 |
| experience_type_id | 이 slot이 속한 체험 type FK | 발표/소개팅/면접마다 필요한 맥락이 다르기 때문에 필요 |
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

발표 체험 예:

```text
presentation_topic
presentation_place
anxiety_point
desired_persona
speaking_habit
```

소개팅 체험 예:

```text
meeting_place
desired_first_impression
opponent_mood
awkward_moment
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
presentation_topic = TEXT
formality_level = SINGLE_SELECT
anxiety_type = MULTI_SELECT
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

예를 들어 발표 체험의 장소 유형 slot에는 `meeting_room`, `classroom`, `auditorium`만 연결하고, 소개팅 체험의 장소 유형 slot에는 `restaurant`, `cafe`, `outdoor`만 연결할 수 있다.

## Redis 세션 저장 모델

세션 진행 상태와 외부 API callback 결과는 빠른 조회를 위해 Redis에 저장한다.

```json
{
  "sessionId": "uuid",
  "experienceType": "PRESENTATION",
  "selectedOutfitId": "presentation_jacket_01",
  "status": "REHEARSAL_READY",
  "vtonStatus": "COMPLETED",
  "contextStatus": "COMPLETED",
  "baseSnapshot": {
    "imageUrl": "object-storage-url",
    "poseContext": {
      "fullBodyVisible": true,
      "shoulderTilt": 0.04,
      "centered": true,
      "stableSeconds": 2.3
    }
  },
  "transcript": "내일 팀 발표가 있는데 질문을 받으면 말이 꼬일 것 같아요.",
  "userContext": {
    "presentation_topic": "팀 프로젝트 발표",
    "anxiety_point": "질문을 받으면 말이 꼬일까 봐 걱정됨"
  },
  "finalUserContext": {
    "presentation_topic": "팀 프로젝트 발표",
    "presentation_place": "회의실",
    "anxiety_point": "질문을 받으면 말이 꼬일까 봐 걱정됨",
    "desired_persona": "차분하고 신뢰감 있는 태도"
  },
  "followUpAttempt": 0,
  "vtonResult": {
    "imageUrl": "vton-result-url"
  },
  "feedbackResult": {},
  "finalResult": {}
}
```

## DB에서 제외하는 항목

### 옷 리스트와 옷 이미지

옷 리스트와 옷 사진은 MVP 설정 DB에서 관리하지 않는다.

관리 위치:

```text
별도 에셋/이미지 저장소
```

세션에는 사용자가 선택한 옷의 id 또는 asset reference만 저장한다.

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
pose-stability-rule
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

새 체험 type을 추가할 때는 공통 파이프라인을 유지하고 DB 설정만 추가한다.

1. `experience_type`에 새 체험 type을 추가한다.
2. `context_slot`에 그 체험에서 받아야 할 맥락을 정의한다.
3. 객관식이 필요한 slot은 `context_option`을 재사용하거나 새로 추가한다.
4. `context_slot_option`으로 slot과 option을 연결한다.
5. prompt 파일은 `experienceType`과 `context_slot` 목록을 입력으로 받아 체험별 차이를 처리한다.

코드가 달라지는 지점은 새로운 외부 API, 새로운 관찰 지표, 새로운 정량 rule처럼 파이프라인 자체가 늘어날 때로 제한한다.
