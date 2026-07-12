# Client Session State

## 목적

`ClientSession`은 한 사용자의 Daily Rehearsal 1회 실행 상태를 나타낸다.

이 값은 RDB 테이블(`rehearsal_session` 등)을 통해 영속화되며, 단일 진실 공급원(Single Source of Truth)으로 관리된다. 기존 구현의 Redis는 완전히 제거되었다.
`ClientSession`은 이제 명확한 RDB 엔티티 구조와 1:1로 매핑되는 도메인 모델이다.

세션은 사용자가 전시 플로우를 진행하는 동안 계속 바뀌는 값을 저장한다.

1. 상황 타입 선택
2. 브리핑 및 context 수집
3. outfit 확정
4. 리허설 시뮬레이션
5. 턴별 대화 기록 및 평가 누적

## 저장 경계

세션 상태는 MySQL(RDB)에 단일 진실 공급원(SSOT)으로 저장한다.

| Layer | File | Role |
| --- | --- | --- |
| Domain model | `domain/session/model/ClientSession.java` | 세션 상태와 상태 전이 규칙 |
| Domain port | `domain/session/repository/SessionRepository.java` | application service가 사용하는 세션 저장소 계약 |
| Persistence adapter | `datasource/db-integrated/.../JpaSessionRepositoryAdapter.java` | `SessionRepository`의 JPA 구현체 |
| Persistence entity | `datasource/db-integrated/.../RehearsalSessionEntity.java` | RDB 테이블과 매핑되는 JPA 엔티티 |

정적 설정은 세션에 저장하지 않는다. 정적 설정은 코드 기반 registry 또는 resolver에서 조회한다.

| Static component | Role |
| --- | --- |
| `SituationType` (Enum) | 선택 가능한 상황 타입과 선택 화면 메타데이터 제공 |
| `ContextSlotSchemaType` (Enum) | 상황별 slot 정의, 질문, default, option 제공 |
| `OutfitSpecResolver` | outfit id 검증 및 Decart VTON spec 제공 |
| `RehearsalConfigRegistry` | 상황별 시뮬레이션 설정 제공. 예: 최대 턴 수, 첫 상대 발화 |

## Session Fields

| Field | Type | Role |
| --- | --- | --- |
| `sessionId` | `String` | 프론트가 세션 단위 API를 호출할 때 사용하는 공개 식별자 |
| `situationType` | `SituationType` | 사용자가 선택한 상황 타입. slot, outfit, rehearsal config 조회 기준 |
| `status` | `SessionStatus` | Daily Rehearsal 전체 플로우 상태 |
| `contextStatus` | `ContextStatus` | context 수집 과정의 세부 상태 |
| `followUpAttempt` | `int` | context 수집 중 사용한 재질문 라운드 횟수 |
| `partialContext` | `SessionContext` | required slot이 아직 완성되지 않은 중간 context |
| `finalContext` | `SessionContext` | outfit, rehearsal, feedback, result 플로우에서 사용할 최종 context |
| `missingSlotKeys` | `List<String>` | 추가 입력이 필요한 required slot key 목록 |
| `followUpQuestions` | `List<String>` | 사용자에게 보여줄 고정 재질문 목록 |
| `selectedOutfitId` | `String` | 사용자가 확정한 outfit id |
| `currentTurn` | `int` | 현재 리허설 시뮬레이션 턴 |
| `maxTurn` | `int` | 상황별 rehearsal config에서 결정된 최대 턴 수 |
| `conversationHistory` | `List<ConversationHistory>` | 턴별 상대 발화와 사용자 transcript 기록 |
| `turnEvaluations` | `List<TurnEvaluation>` | 턴별 성공 여부, 피드백, fallback 여부 기록 |

## Context Fields

`partialContext`와 `finalContext`는 서로 다른 완성도를 표현하므로 별도 필드로 둔다.

`partialContext`는 required slot이 아직 남아 있을 때 사용한다. 이미 추출된 slot 값을 보존해서
follow-up 단계에서 부족한 정보만 다시 물을 수 있게 한다.

`finalContext`는 required slot이 채워졌거나 default로 보정된 뒤 사용하는 완성된 context다. outfit,
rehearsal, feedback, result 플로우는 `finalContext`를 기준으로 진행한다.

상황 타입의 기준값은 `ClientSession.situationType`이다. context payload에 `situation_type`이 필요할 때는
클라이언트 입력이나 AI 응답을 신뢰하지 않고 `session.getSituationType().key()`에서 파생해서 넣는다.

## Status Model

`SessionStatus`는 사용자 경험 전체의 진행 단계를 나타낸다.

| Status | Meaning |
| --- | --- |
| `BRIEFING` | 최초 briefing transcript를 받을 준비가 된 상태 |
| `CONTEXT_EXTRACTING` | transcript를 context slot으로 정규화하는 상태 |
| `FOLLOW_UP_REQUIRED` | required context slot을 채우기 위해 추가 입력이 필요한 상태 |
| `TRANSFORMATION_READY` | context가 완성되어 outfit transformation으로 넘어갈 수 있는 상태 |
| `TRANSFORMATION_PREPARING` | transformation 준비 중인 상태 |
| `REHEARSAL_READY` | outfit이 확정되어 리허설을 시작할 수 있는 상태 |
| `REHEARSAL_PLAYING` | 리허설 시뮬레이션 진행 중인 상태 |
| `FEEDBACK_GENERATING` | 피드백 또는 결과 생성 중인 상태 |
| `RESULT_READY` | 최종 결과가 준비된 상태 |
| `COMPLETED` | 세션 플로우가 완료된 상태 |
| `FAILED` | 세션 플로우가 실패한 상태 |

`ContextStatus`는 context 수집 과정만 따로 나타낸다.

| Status | Meaning |
| --- | --- |
| `NOT_STARTED` | context 수집을 아직 시작하지 않은 상태 |
| `EXTRACTING` | 최초 briefing transcript를 정규화하는 상태 |
| `FOLLOW_UP_REQUIRED` | context 수집을 위해 추가 입력이 필요한 상태 |
| `MERGING` | follow-up 답변을 기존 context에 병합하는 상태 |
| `COMPLETED` | context 수집이 완료된 상태 |
| `FAILED` | context 수집이 실패한 상태 |

## Current Transitions

`ClientSession`은 자유로운 setter 대신 상태 전이 메서드를 제공한다. 각 전이 메서드는 현재 상태에서 다음
상태로 이동해도 되는지 검증한 뒤, 관련 필드를 함께 변경한다.

| Method | Valid state | Effect |
| --- | --- | --- |
| `startContextExtraction()` | `BRIEFING` | `CONTEXT_EXTRACTING`, `ContextStatus.EXTRACTING`으로 이동 |
| `startFollowUpMerge()` | `FOLLOW_UP_REQUIRED` / `ContextStatus.FOLLOW_UP_REQUIRED` | `CONTEXT_EXTRACTING`, `ContextStatus.MERGING`으로 이동하고 `followUpAttempt` 증가 |
| `selectOutfit(selectedOutfitId)` | `TRANSFORMATION_READY` | 플로우를 진행하지 않고 선택 outfit id만 저장 |
| `confirmOutfit(selectedOutfitId)` | `TRANSFORMATION_READY`이고 `finalContext`가 완료된 상태 | outfit id를 저장하고 `REHEARSAL_READY`로 이동 |
| `startSimulation(maxTurn)` | `REHEARSAL_READY` | `REHEARSAL_PLAYING`으로 이동하고 `currentTurn = 1`, `maxTurn` 저장 |
| `recordTurn(...)` | `REHEARSAL_PLAYING`이고 턴 제한 이내 | 대화 기록과 턴 평가를 누적하고, 성공 시 다음 턴으로 이동 |

context 완료 관련 전이도 같은 패턴을 따라야 한다.

| Transition | Expected source | Expected effect |
| --- | --- | --- |
| 최초 briefing 추출 시작 | `BRIEFING` / `NOT_STARTED` | `CONTEXT_EXTRACTING` / `EXTRACTING`으로 이동 |
| Follow-up merge 시작 | `FOLLOW_UP_REQUIRED` / `FOLLOW_UP_REQUIRED` | `CONTEXT_EXTRACTING` / `MERGING`으로 이동하고 `followUpAttempt` 증가 |
| Follow-up 필요 | `CONTEXT_EXTRACTING` / `EXTRACTING` 또는 `MERGING` | `partialContext`, `missingSlotKeys`, `followUpQuestions` 저장 후 follow-up required 상태로 이동 |
| Context 완료 | `CONTEXT_EXTRACTING` / `EXTRACTING` 또는 `MERGING` | `finalContext` 저장, missing/follow-up 값 정리 후 transformation ready 상태로 이동 |

## Flow Ownership

Application service는 유스케이스 흐름을 조립하고 `ClientSession`의 전이 메서드를 호출한다.

| Application component | Session role |
| --- | --- |
| `SessionService` | 세션 생성, briefing 제출, outfit 확정 흐름 처리 |
| `SessionReader` | 공통 세션 조회 및 `SESSION_NOT_FOUND` 처리 |
| `SimulationService` | 시뮬레이션 시작 및 턴 결과 누적 |
| `DecartSpecService` | Decart token/spec 발급 전 세션 상태 검증 |

`ClientSession`은 세션 상태의 일관성을 책임진다. Application service는 어떤 흐름에서 어떤 전이를 호출할지
결정하고, domain model은 해당 전이가 허용되는 상태인지 검증한다.
