# Prompt and Rule Responsibility Draft

이 문서는 Daily Rehearsal MVP에서 어떤 설정을 DB에 저장하고, 어떤 로직을 prompt 파일로 관리하며, 어떤 판단을 code/rule로 고정할지 구분한다.

## 기본 원칙

- DB는 체험 운영자가 바꿀 수 있어야 하는 구조화 설정을 저장한다.
- Prompt 파일은 LLM의 역할, 출력 형식, 말투, 판단 기준을 버전 관리한다.
- Code/rule은 정량 지표처럼 LLM에게 맡기면 흔들리는 판정을 담당한다.
- 옷 리스트와 옷 이미지는 별도 에셋/이미지 관리 영역에서 다룬다. MVP DB schema에는 옷 선택지 테이블을 두지 않는다.

## DB에 둘 것

### context_slot

P1에서 사용자의 음성 브리핑으로 채워야 하는 맥락이다.
내일의 상황 맥락은 별도 분류 호출이 아니라 `situation_type` slot으로 다룬다.

역할:
- LLM이 어떤 값을 추출해야 하는지 알려준다.
- 어떤 맥락이 필수인지 판단한다.
- 부족한 맥락이 있을 때 추가 질문을 만들 기준을 제공한다.
- 비어 있는 값에 기본값을 적용할 수 있게 한다.

예:
- 내일의 상황 맥락
- 장소/공간 맥락
- 결정적 순간
- 되고 싶은 태도
- 상대 분위기

P1 MVP 공통 active slot:

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

### context_option

여러 체험에서 재사용 가능한 객관식 선택지이다.

역할:
- `context_slot.slot_type`이 `SINGLE_SELECT` 또는 `MULTI_SELECT`일 때 선택지를 제공한다.
- 격식 수준, 장소 유형, 불안 유형처럼 여러 체험에서 반복되는 값을 재사용한다.

예:
- `formality_level`: casual, smart_casual, business, formal
- `anxiety_type`: nervous, awkward, hard_question, low_energy
- `place_type`: meeting_room, restaurant, classroom, office

### context_slot_option

context slot과 option을 연결한다.

역할:
- 같은 option group을 여러 체험/slot에서 재사용할 수 있게 한다.
- 체험별 slot마다 허용되는 선택지를 다르게 제한한다.

## Prompt 파일로 둘 것

Prompt 파일은 `docs/prompts` 또는 실제 구현 시 `src/main/resources/prompts` 같은 위치에서 관리한다.

### context-extraction.md

사용자 음성 브리핑을 구조화된 context로 바꾸는 프롬프트이다.
이 단계에서 slot 추출, 부족 slot 판정, follow-up question 생성을 한 번에 처리한다.

입력:
- 프론트 STT가 확정한 transcript text
- DB에서 가져온 `context_slot` 목록
- 각 slot의 required 여부, type, option, extraction hint

역할:
- transcript에서 slot별 값을 추출한다.
- `critical_moment` slot은 결정적 순간 리허설을 구성하는 기준으로 사용한다.
- `situation_type` slot은 발표/소개팅/면접/일상 정돈 등 상황별 표현을 고르는 기준으로 사용한다.
- 판단할 수 없는 값은 억지로 채우지 않고 `null`로 둔다.
- slot key를 DB 정의와 동일하게 반환한다.
- 부족한 required slot 목록과 follow-up question을 같은 응답에서 반환한다.
- MVP에서는 confidence 점수를 반환하지 않는다.

출력 예:

```json
{
  "slots": {
    "situation_type": {
      "value": "presentation"
    },
    "critical_moment": {
      "value": "예상 질문을 받는 순간"
    },
    "audience_mood": {
      "value": null
    }
  },
  "missingRequiredSlotKeys": [],
  "followUpQuestion": null,
  "readyForSimulation": true
}
```

### Structured Outputs 적용 방식

Java 서버는 active `context_slot` 목록으로 OpenAI Structured Outputs JSON Schema를 런타임 생성한다.

원칙:
- 고정 Java enum/DTO에 slot key를 박지 않는다.
- slot 추가/삭제는 DB의 `active` 상태와 schema 재생성으로 처리한다.
- Java에서는 Jackson `ObjectNode` 등으로 JSON Schema를 만들고, OpenAI 요청의 `response_format`/`text.format`에 `json_schema`와 `strict: true`를 설정한다.
- schema의 `slots.properties`는 active slot key로 동적으로 구성한다.
- `additionalProperties: false`를 설정해 정의되지 않은 slot이 나오지 않게 한다.
- 모든 active slot key를 `required`에 넣고, 비어 있는 값은 `null`로 표현한다.
- 후속 질문은 별도 LLM 호출을 하지 않고 같은 응답의 `followUpQuestion`으로 받는다.

### follow-up-question.md

부족한 맥락을 사용자에게 묻는 fallback 프롬프트이다.
기본 흐름에서는 `context-extraction.md`의 Structured Outputs 응답이 `followUpQuestion`까지 함께 반환한다.

입력:
- 부족한 required slot 목록
- 각 slot의 `followUpQuestion`
- 현재까지 채워진 context
- 최대 질문 횟수

역할:
- 부족한 slot을 하나씩 캐묻지 않고 짧은 묶음 질문으로 만든다.
- 전시장 체험이 늘어지지 않도록 질문을 1회로 제한한다.
- 질문 문구는 DB의 slot 질문을 우선 사용하고, 자연스러운 연결만 LLM이 담당한다.

예:

```text
발표 장면이 거의 완성됐어요.
마지막으로 발표 공간과 가장 걱정되는 순간만 짧게 말해주세요.
```

### simulation-dialogue.md

시뮬레이터 단계에서 AI 상대의 첫 한마디나 장면을 구성하는 프롬프트이다.

입력:
- 최종 사용자 context
- 최종 사용자 context의 `situation_type`
- 최종 사용자 context의 `critical_moment`
- 사용자가 선택한 옷 또는 Decart preview metadata
- 체험 목표

역할:
- `situation_type`과 `critical_moment`에 어울리는 상호작용 장면을 만든다.
- 사용자가 짧게 답할 수 있는 한 문장 또는 짧은 상황을 생성한다.
- 평가보다 리허설 감각을 우선한다.

예:
- 발표: 예상 질문 한 문장
- 소개팅: 첫 인사 또는 어색한 침묵 상황
- 면접: 자기소개 이후 꼬리 질문

### feedback-generation.md

리허설 응답에 대한 정성 피드백을 생성하는 프롬프트이다.

입력:
- 최종 사용자 context
- 사용자 리허설 응답 텍스트
- code/rule에서 계산한 정량 지표
- 피드백해야 하는 항목 목록

역할:
- 사용자의 답변이 상황에 맞는지 판단한다.
- 점수보다 내일 바로 바꿀 수 있는 행동 중심으로 피드백한다.
- 정량 rule 결과를 뒤집지 않고, 그 결과를 자연어로 설명한다.

예:
- "목소리가 작다"는 rule 결과가 있으면 목소리 개선 피드백을 생성한다.
- 답변 구조가 불명확하면 첫 문장을 더 짧게 잡는 제안을 한다.

### result-card.md

최종 변화 카드/결과지를 생성하는 프롬프트이다.

입력:
- 최종 사용자 context
- 사용자가 선택한 outfit/Decart preview metadata
- 피드백 결과
- `finalUserContext.situation_type`

역할:
- 긴 리포트가 아니라 짧은 변화 카드로 결과를 정리한다.
- 오늘 바꿀 행동, 내일 유지할 태도, If-Then 카드를 만든다.
- 상황별 표현 차이는 `situation_type` 조건으로 처리한다.

출력 예:

```json
{
  "changeAction": "발표 15분 전에 첫 슬라이드를 열어두기",
  "keepAttitude": "질문을 먼저 인정하고 천천히 답하기",
  "ifThen": "예상 밖 질문이 나오면, 한 문장으로 질문을 다시 정리하고 답하기"
}
```

## Code/rule로 둘 것

Code/rule은 LLM이 아니라 서버 또는 클라이언트에서 계산 가능한 정량 판정이다.

### gesture-navigation-rule

역할:
- 프론트에서 outfit switching 제스처를 안정적으로 판정한다.
- 좌/우 swipe, open-palm dwell 같은 단순 제스처만 사용한다.
- Decart `rt.set({ prompt, image })` 호출이 과도하게 발생하지 않도록 debounce/cooldown을 둔다.

판정 예:
- 오른손 좌/우 이동 거리가 threshold를 넘으면 `NEXT_OUTFIT` 또는 `PREV_OUTFIT`
- 손바닥이 일정 시간 유지되면 `CONFIRM_OUTFIT`
- confidence가 낮거나 cooldown 중이면 gesture를 무시

### voice-volume-rule

역할:
- 사용자의 리허설 발화 음량이 너무 작은지 판단한다.

판정 예:
- 평균 음량이 threshold보다 낮으면 `voice_volume=LOW`
- 피드백 프롬프트는 이 결과를 받아 자연어 가이드를 생성한다.

### speech-rate-rule

역할:
- 말 속도가 너무 빠르거나 느린지 판단한다.

판정 예:
- 분당 음절 또는 단어 수 기준으로 `FAST`, `NORMAL`, `SLOW` 분류

### silence-duration-rule

역할:
- AI 질문 이후 사용자가 답변을 시작하기까지의 침묵 시간을 판단한다.

판정 예:
- 일정 시간 이상 침묵하면 `response_delay=LONG`
- 소개팅/면접/발표 같은 `situation_type`마다 해석은 다를 수 있지만, 원시 판정은 code/rule에서 한다.

### response-length-rule

역할:
- 사용자의 답변이 너무 짧거나 긴지 판단한다.

판정 예:
- 발표 예상 질문 답변이 지나치게 짧으면 근거 부족으로 표시
- 소개팅 첫 대답이 지나치게 길면 대화 균형 피드백에 활용

## 보류할 DB 테이블

아래 항목은 지금 당장 DB 테이블로 분리하지 않는다.

### simulation_scene

보류 이유:
- MVP에서는 체험별 장면이 아직 자주 바뀔 가능성이 높다.
- prompt 파일과 `situation_type` 조건으로 충분히 실험할 수 있다.

추후 DB화 조건:
- 체험별 장면 수가 늘어나고, 관리자 화면에서 장면을 켜고 끄거나 순서를 바꿔야 할 때.

### feedback_rule

보류 이유:
- 정량 rule은 초기에는 코드로 두는 편이 테스트하기 쉽다.
- DB 조건식으로 만들면 구현 복잡도가 올라간다.

추후 DB화 조건:
- threshold를 운영 중 자주 바꿔야 하거나, 체험별 rule 조합을 관리자 설정으로 바꿔야 할 때.

### vton_guide

보류 이유:
- 사용자가 고르는 옷 사진과 VTON용 옷 정보는 별도 에셋 관리에서 다룬다.
- DB에는 context slot과 option만 먼저 안정화한다.

추후 DB화 조건:
- 같은 옷 이미지라도 `situation_type`/context에 따라 VTON prompt를 다르게 조합해야 할 때.

### result_template

보류 이유:
- MVP 결과지는 변화 카드 형태로 고정해도 충분하다.
- 결과 문구 생성은 `result-card.md` prompt에서 처리한다.

추후 DB화 조건:
- 체험별 결과지 섹션을 운영자가 직접 추가/삭제/정렬해야 할 때.

## MVP 기준 결론

현재 MVP에서 필요한 DB 테이블은 다음 정도로 제한한다.

```text
context_slot
context_option
context_slot_option
```

Prompt 파일은 다음을 둔다.

```text
context-extraction.md
follow-up-question.md
simulation-dialogue.md
feedback-generation.md
result-card.md
```

Code/rule은 다음을 우선 구현한다.

```text
gesture-navigation-rule
voice-volume-rule
speech-rate-rule
silence-duration-rule
response-length-rule
```
