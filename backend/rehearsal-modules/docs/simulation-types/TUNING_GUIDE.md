# 시뮬레이션 타입 튜닝 가이드

이 문서는 `소개팅`, `면접`, `첫 출근` 시뮬레이션을 담당하는 팀원이 타입별 문구와 규칙을 안전하게 수정하기 위한 가이드다.

## 수정 대상 요약

| 목적 | 파일 | 수정 시점 |
|---|---|---|
| 타입 이름과 브리핑 안내 | `domain/.../situation/model/SituationType.java` | 프론트에 보이는 타입명, 브리핑 질문 또는 예시 답변을 바꿀 때 |
| 브리핑에서 수집할 정보 | `domain/.../slot/registry/ContextSlotSchemaType.java` | 타입별 required/optional slot, 재질문, 기본값을 바꿀 때 |
| 턴 진행과 AI 생성 방향 | `domain/.../rehearsal/registry/type/*RehearsalConfig.java` | 첫 턴, 턴별 목표, 피드백 기준, 복구 방향을 바꿀 때 |
| 티켓 장애 대응 문구 | `domain/.../ticket/registry/TicketCopyRegistry.java` | AI 티켓 생성 실패 시 사용할 fallback 문구를 바꿀 때 |
| 기획 의도와 튜닝 기록 | `docs/simulation-types/{type}.md` | 타입별 시나리오 의도와 검증 사례를 정리할 때 |

타입별 파일은 다음과 같이 대응한다.

| 타입 | 기획 문서 | 턴 설정 |
|---|---|---|
| 소개팅 (`date`) | `date.md` | `DateRehearsalConfig.java` |
| 면접 (`interview`) | `interview.md` | `InterviewRehearsalConfig.java` |
| 첫 출근 (`first_day`) | `first-day.md` | `FirstDayRehearsalConfig.java` |

## 권장 수정 순서

1. 담당 타입의 Markdown 문서에 사용자 상황, 턴별 목표, 정상 답변 의도, 피드백 방향을 먼저 적는다.
2. 브리핑 화면의 문구가 달라지면 `SituationType.java`를 수정한다.
3. 시뮬레이션 개인화에 추가 정보가 꼭 필요할 때만 `ContextSlotSchemaType.java`를 수정한다.
4. 담당 타입의 `*RehearsalConfig.java`에 첫 턴과 AI 생성 지침을 반영한다.
5. AI 티켓 생성 실패 시 문구도 타입에 맞아야 한다면 `TicketCopyRegistry.java`를 수정한다.
6. 자동 테스트와 전체 빌드를 실행한다.

## SituationType.java

`SituationType`은 API와 세션이 공유하는 상황 타입의 기준이다.

- `key`: 외부 API와 DB에서 쓰는 안정적인 식별자다. 현재 `date`, `interview`, `first_day`를 사용한다.
- `displayName`: 상황 선택 화면에 표시할 이름이다.
- `briefingTitle`: 타입 선택 후 사용자에게 보여줄 브리핑 질문이다.
- `exampleAnswer`: 사용자가 어떤 내용을 말하면 되는지 보여주는 예시다.

`key`는 API와 저장 데이터의 계약이므로 문구를 다듬듯 가볍게 변경하면 안 된다. 타입을 추가할 때는 enum 상수만 추가하지 말고 slot schema, rehearsal config, ticket fallback과 테스트도 함께 추가한다.

## ContextSlotSchemaType.java

이 파일은 턴마다 사용자의 답변을 평가하는 규칙이 아니다. 시뮬레이션 시작 전에 받은 briefing transcript에서 어떤 정보를 수집할지 정의한다.

현재 공통 핵심 정보는 다음과 같다.

- `desired_persona`: 사용자가 남기고 싶은 인상
- `critical_moment`: 가장 걱정하거나 연습하고 싶은 순간
- `outfit_direction`: 원하는 스타일 방향

각 slot에는 다음 설정이 포함될 수 있다.

- required/optional 여부
- 추출 힌트
- 누락 시 표시할 고정 재질문
- AI 실패 또는 최대 재질문 횟수 초과 시 사용할 default
- 선택형 slot의 options

흐름은 `briefing transcript -> slot normalize -> required 누락 판단 -> follow-up 또는 default -> SessionContext 저장`이다. 특정 타입의 턴 생성에 반드시 필요한 정보가 현재 slot으로 표현되지 않을 때만 새 slot을 추가한다. 단순히 프롬프트 문장을 바꾸기 위해 slot을 늘리지는 않는다.

## 타입별 RehearsalConfig

타입별 설정 클래스는 실제 3턴 시뮬레이션의 내용과 AI 생성 방향을 결정한다.

- `maxTurn`: 전체 턴 수다. 현재 3으로 유지한다.
- `firstTurn`: 첫 장면, 상대 발화, 사용자 행동 요구, 정상 응답 의도를 정적으로 정의한다.
- `turnObjectives`: 각 턴에서 훈련할 목표다. 목록 크기는 `maxTurn`과 같아야 한다.
- `feedbackFocus`: 평가 피드백에서 우선 확인할 관점이다.
- `recoveryDirection`: 이전 답변이 유효하지 않았을 때 다음 턴을 자연스럽게 이어가기 위한 AI 지침이다.
- `technicalFallback`: Gemini 호출 또는 응답 파싱이 실패했을 때 사용할 고정 턴이다.

첫 턴은 설정의 `firstTurn`을 사용한다. 이후 턴은 briefing context와 이전 사용자 발화를 바탕으로 Gemini가 생성하며, 이전 턴이 정상 진행되지 않았다면 `recoveryDirection`을 함께 사용한다. `acceptedIntentHint`는 정상 답변의 문장 자체가 아니라 의도를 설명하는 내부 평가 힌트이며 프론트에 노출하지 않는다.

팀원이 주로 튜닝할 부분은 다음과 같다.

- 사용자에게 실제로 말해볼 만한 첫 장면과 행동 요구인지
- 1~3턴의 목표가 중복되지 않고 난도가 자연스럽게 이어지는지
- 낮은 품질의 답변과 무관한 답변을 구분할 수 있는 평가 힌트인지
- 실패 후 다음 턴이 briefing context를 유지하면서도 자연스럽게 복구되는지
- 기술적 fallback만으로도 화면이 멈추지 않는지

## TicketCopyRegistry.java

`TicketCopyRegistry`의 문구는 정상적인 티켓 내용의 주 공급원이 아니다. Gemini 기반 티켓 생성이 실패했을 때도 결과 화면을 완성하기 위한 타입별 `ChangeCard` fallback이다.

- `todayAction`: 오늘 연습에서 바꾼 행동
- `tomorrowAttitude`: 실전에서 유지할 태도
- `ifThenPlan`: 어려운 상황이 생겼을 때 실행할 행동 계획

정상 흐름의 변화 카드 내용은 briefing context와 턴 평가 결과를 이용해 생성한다. 따라서 상세한 개인화 문구를 이 registry에 하드코딩하지 않는다.

## 타입별 Markdown 문서

각 타입 문서는 코드에 들어갈 문장을 임의로 모아두는 파일이 아니라, 팀원이 같은 기준으로 튜닝하고 검증하기 위한 기획 계약이다. 최소한 다음 내용을 유지한다.

- 이 타입에서 사용자가 연습하려는 상황
- briefing에서 필요한 required/optional context
- 1~3턴의 훈련 목표
- 정상 답변으로 인정할 의도
- 재시도와 강제 진행 시 피드백 방향
- 다음 턴 복구 방향
- 기술적 fallback
- 대표적인 수동 검증 입력과 기대 결과

기획 문서와 코드가 달라지면 같은 PR에서 함께 수정한다.

## 공용 코드와 충돌 방지

타입 담당자는 우선 자신의 Markdown 문서와 `*RehearsalConfig.java`를 수정한다. 다음 파일은 여러 타입이 공유하므로 필요한 구역만 작게 수정하고 작업 전에 최신 브랜치를 반영한다.

- `SituationType.java`
- `ContextSlotSchemaType.java`
- `TicketCopyRegistry.java`

공통 API DTO, `SimulationService`, worker, JPA entity, 상태 enum을 바꿔야 한다면 타입별 튜닝 범위를 넘어선다. 세 타입의 계약에 영향을 주므로 별도 리팩터 이슈로 분리해 먼저 합의한다.

## 검증

`backend/rehearsal-modules`에서 다음 순서로 확인한다.

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :domain:test :rehearsal-api:test --console=plain
.\gradlew.bat build --console=plain
```

PR 전에는 담당 타입의 다음 사례를 수동으로도 확인한다.

1. 정상 답변이 `ACCEPTED`로 평가되고 다음 턴이 이전 발화를 반영하는지
2. 무관한 첫 답변이 `RETRY_REQUIRED`가 되는지
3. 두 번째 무관한 답변이 `FORCED_ADVANCE`가 되고 다음 턴이 복구되는지
4. 세 번째 턴 이후 시뮬레이션이 완료되는지
5. AI 실패 시 타입별 technical fallback과 ticket fallback이 사용되는지
