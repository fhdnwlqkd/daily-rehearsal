# Client Session State - RDB Architecture

## 목적

`ClientSession`은 한 사용자의 Daily Rehearsal 1회 실행 상태를 나타낸다.

이 문서는 MySQL RDB를 세션의 단일 진실 공급원(Single Source of Truth)으로 사용하는
현재 구조를 정의한다. 별도 Redis session/cache/job은 사용하지 않는다.

`ClientSession`은 `rehearsal_session`에 대응하는 세션 root 도메인 모델이다. Context, simulation
turn/attempt, 최종 결과는 별도 도메인 모델과 테이블로 관리한다.

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
| Persistence entities | `datasource/db-integrated/.../entity` | 5개 RDB 테이블과 각각 매핑되는 JPA 엔티티 |

`SessionRepository`는 하나의 domain port로 유지한다. JPA adapter 내부에서는 테이블별 Spring Data
JPA repository를 사용해 session, context, turn, attempt, result를 저장하고 조회한다.

JPA 관계는 자식 entity가 부모 FK 연관관계를 소유한다. 자식에서 부모로 이동할 때는 lazy reference를
사용하고, 부모 entity에는 `@OneToMany` collection을 두지 않는다. Polling은 필요한 child row를
repository query로 직접 조회한다.

정적 설정은 세션에 저장하지 않는다. 정적 설정은 코드 기반 registry 또는 resolver에서 조회한다.

| Static component | Role |
| --- | --- |
| `SituationType` (Enum) | 선택 가능한 상황 타입과 선택 화면 메타데이터 제공 |
| `ContextSlotSchemaType` (Enum) | 상황별 slot 정의, 질문, default, option 제공 |
| `OutfitSpecResolver` | outfit id 검증 및 Decart VTON spec 제공 |
| `RehearsalConfigRegistry` | 상황별 시뮬레이션 설정 제공. 예: 최대 턴 수, 첫 상대 발화 |

## Target Domain Boundaries

| Domain model | Persistence table | Role |
| --- | --- | --- |
| `ClientSession` | `rehearsal_session` | 세션 root와 전체/context 진행 상태 |
| `SessionContext` | `session_context_value` | 세션에서 수집한 context 값 |
| `SimulationTurn` | `simulation_turn` | 상대 발화 생성 상태와 결과 |
| `SimulationTurnAttempt` | `simulation_turn_attempt` | 사용자 답변과 평가 상태/결과 |
| `RehearsalResult` | `rehearsal_result` | 영상, 티켓, 다운로드 결과 |

## ClientSession Target Fields

| Field | Type | Role |
| --- | --- | --- |
| `sessionId` | `String` | 프론트가 세션 단위 API를 호출할 때 사용하는 공개 식별자 |
| `situationType` | `SituationType` | 사용자가 선택한 상황 타입. slot, outfit, rehearsal config 조회 기준 |
| `status` | `SessionStatus` | Daily Rehearsal 전체 플로우 상태 |
| `contextStatus` | `ContextStatus` | context 수집 과정의 세부 상태 |
| `followUpAttempt` | `int` | context 수집 중 사용한 재질문 라운드 횟수 |
| `selectedOutfitId` | `String` | 사용자가 확정한 outfit id |
| `currentTurn` | `int` | 현재 리허설 시뮬레이션 턴 |
| `maxTurn` | `int` | 상황별 rehearsal config에서 결정된 최대 턴 수 |

## Context Fields

수집된 context 값은 `session_context_value`에 누적한다. Partial/Final 상태를 별도 context 복사본으로
저장하지 않고 `ClientSession.contextStatus`로 구분한다. Missing slot과 follow-up 질문은 현재 context
값과 정적 `ContextSlotSchemaType`에서 계산한다.

상황 타입의 기준값은 `ClientSession.situationType`이다. context payload에 `situation_type`이 필요할 때는
클라이언트 입력이나 AI 응답을 신뢰하지 않고 `session.getSituationType().getKey()`에서 파생해서 넣는다.

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
| `confirmOutfit(selectedOutfitId)` | `TRANSFORMATION_READY`이고 context 수집이 완료된 상태 | outfit id를 저장하고 `REHEARSAL_READY`로 이동 |
| `startSimulation(maxTurn)` | `REHEARSAL_READY` | `REHEARSAL_PLAYING`으로 이동하고 `currentTurn = 1`, `maxTurn` 저장 |
| `advanceTurn()` | `REHEARSAL_PLAYING`이고 턴 제한 이내 | 성공한 평가가 저장된 뒤 다음 턴으로 이동 |

context 완료 관련 전이도 같은 패턴을 따라야 한다.

| Transition | Expected source | Expected effect |
| --- | --- | --- |
| 최초 briefing 추출 시작 | `BRIEFING` / `NOT_STARTED` | `CONTEXT_EXTRACTING` / `EXTRACTING`으로 이동 |
| Follow-up merge 시작 | `FOLLOW_UP_REQUIRED` / `FOLLOW_UP_REQUIRED` | `CONTEXT_EXTRACTING` / `MERGING`으로 이동하고 `followUpAttempt` 증가 |
| Follow-up 필요 | `CONTEXT_EXTRACTING` / `EXTRACTING` 또는 `MERGING` | context 값을 저장하고 follow-up required 상태로 이동 |
| Context 완료 | `CONTEXT_EXTRACTING` / `EXTRACTING` 또는 `MERGING` | context 값을 저장하고 transformation ready 상태로 이동 |

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
