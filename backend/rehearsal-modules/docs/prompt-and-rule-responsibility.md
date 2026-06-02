# Prompt and Rule Responsibility Draft

이 문서는 Daily Rehearsal MVP에서 어떤 설정을 DB에 저장하고, 어떤 로직을 prompt 파일로 관리하며, 어떤 판단을 code/rule로 고정할지 구분한다.

## 기본 원칙

- DB는 체험 운영자가 바꿀 수 있어야 하는 구조화 설정을 저장한다.
- Prompt 파일은 LLM의 역할, 출력 형식, 말투, 판단 기준을 버전 관리한다.
- Code/rule은 정량 지표처럼 LLM에게 맡기면 흔들리는 판정을 담당한다.
- 옷 리스트와 옷 이미지는 별도 에셋/이미지 관리 영역에서 다룬다. MVP DB schema에는 옷 선택지 테이블을 두지 않는다.

## DB에 둘 것

### experience_type

세션 내내 유지되는 체험 type이다.

역할:
- 사용자가 고를 수 있는 체험 종류를 제공한다.
- 세션 생성 시 `experienceType`으로 고정된다.
- 이후 context slot, prompt 입력, feedback 기준을 선택하는 기준값이 된다.

예:
- `PRESENTATION`
- `DATE`
- `INTERVIEW`
- `DAILY_RESET`

### context_slot

체험 type별로 사용자의 음성 브리핑에서 채워야 하는 맥락이다.

역할:
- LLM이 어떤 값을 추출해야 하는지 알려준다.
- 어떤 맥락이 필수인지 판단한다.
- 부족한 맥락이 있을 때 추가 질문을 만들 기준을 제공한다.
- 비어 있는 값에 기본값을 적용할 수 있게 한다.

예:
- 발표 주제
- 발표 공간
- 가장 걱정되는 순간
- 되고 싶은 태도
- 상대 분위기

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

입력:
- `experienceType`
- STT transcript
- DB에서 가져온 `context_slot` 목록
- 각 slot의 required 여부, type, option, extraction hint

역할:
- transcript에서 slot별 값을 추출한다.
- 판단할 수 없는 값은 억지로 채우지 않고 `null`로 둔다.
- slot key를 DB 정의와 동일하게 반환한다.
- MVP에서는 confidence 점수를 반환하지 않는다.

출력 예:

```json
{
  "slots": {
    "presentation_topic": {
      "value": "팀 프로젝트 발표"
    },
    "audience_mood": {
      "value": null
    }
  }
}
```

### follow-up-question.md

부족한 맥락을 사용자에게 한 번 더 묻는 프롬프트이다.

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
- `experienceType`
- 최종 사용자 context
- 사용자가 선택한 옷 또는 VTON 결과 메타 정보
- 체험 목표

역할:
- 발표, 소개팅, 면접 등 type별로 어울리는 상호작용 장면을 만든다.
- 사용자가 짧게 답할 수 있는 한 문장 또는 짧은 상황을 생성한다.
- 평가보다 리허설 감각을 우선한다.

예:
- 발표: 예상 질문 한 문장
- 소개팅: 첫 인사 또는 어색한 침묵 상황
- 면접: 자기소개 이후 꼬리 질문

### feedback-generation.md

리허설 응답에 대한 정성 피드백을 생성하는 프롬프트이다.

입력:
- `experienceType`
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
- VTON 결과 또는 fallback 정보
- 피드백 결과
- 체험 type

역할:
- 긴 리포트가 아니라 짧은 변화 카드로 결과를 정리한다.
- 오늘 바꿀 행동, 내일 유지할 태도, If-Then 카드를 만든다.
- 체험 type별 표현 차이는 프롬프트 조건으로 처리한다.

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

### pose-stability-rule

역할:
- VTON 베이스 스냅샷에 적합한 자세인지 판단한다.
- 전신 포함 여부, 중앙 정렬, 어깨 기울기, 안정화 시간을 계산한다.

판정 예:
- 전신이 프레임 밖이면 캡처 불가
- 어깨 기울기가 기준보다 크면 자세 조정 안내
- 일정 시간 이상 안정되면 base snapshot 캡처 가능

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
- 소개팅/면접/발표 type마다 해석은 다를 수 있지만, 원시 판정은 code/rule에서 한다.

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
- prompt 파일과 `experienceType` 조건으로 충분히 실험할 수 있다.

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
- DB에는 체험 type과 context만 먼저 안정화한다.

추후 DB화 조건:
- 같은 옷 이미지라도 체험 type/context에 따라 VTON prompt를 다르게 조합해야 할 때.

### result_template

보류 이유:
- MVP 결과지는 변화 카드 형태로 고정해도 충분하다.
- 결과 문구 생성은 `result-card.md` prompt에서 처리한다.

추후 DB화 조건:
- 체험별 결과지 섹션을 운영자가 직접 추가/삭제/정렬해야 할 때.

## MVP 기준 결론

현재 MVP에서 필요한 DB 테이블은 다음 정도로 제한한다.

```text
experience_type
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
pose-stability-rule
voice-volume-rule
speech-rate-rule
silence-duration-rule
response-length-rule
```
