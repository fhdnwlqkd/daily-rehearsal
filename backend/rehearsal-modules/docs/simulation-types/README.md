# 시뮬레이션 타입 튜닝 가이드

## 목적

Daily Rehearsal의 시뮬레이션 엔진은 소개팅, 면접, 첫 출근에 동일한 진행 규칙을 적용한다.
타입 담당자는 공통 엔진을 수정하지 않고 담당 타입의 briefing, 첫 턴, 턴 목표와 피드백 기준을 조정한다.

## 공통 진행 규칙

- 모든 타입은 최대 3턴으로 진행한다.
- 첫 턴의 장면, 상대 발화, 행동 요구는 타입별 정적 설정에서 가져온다.
- 2턴과 3턴은 briefing context와 이전에 통과한 사용자 발화를 바탕으로 LLM이 생성한다.
- 사용자 발화는 요청한 행동의 의도에 맞으면 표현이 다소 서툴러도 `ACCEPTED`로 처리한다.
- 요청과 무관하거나 진행 방법을 묻는 발화는 첫 번째 시도에서 `RETRY_REQUIRED`로 처리한다.
- 같은 턴의 두 번째 시도도 의도에 맞지 않으면 `FORCED_ADVANCE`로 처리하고 다음 턴으로 이동한다.
- `RETRY_REQUIRED`와 `FORCED_ADVANCE` 발화는 다음 턴의 대화 history에 포함하지 않는다.
- 강제 진행 후 다음 턴은 briefing context와 현재 턴 계획을 사용해 `RECOVERY` 모드로 생성한다.
- Gemini 호출 자체가 실패하면 타입별 기술적 fallback으로 흐름을 계속한다.

## 한 턴의 화면 순서

1. 장면 안내(`sceneCue`)
2. 상대 발화(`opponentLine`)
3. 사용자 행동 요구(`actionPrompt`)
4. 사용자 발화
5. 평가 및 코칭 피드백
6. 재시도 또는 다음 턴

## 타입 담당자가 조정할 항목

각 타입 담당자는 담당 타입의 문서와 타입 정의 파일만 수정한다.

- situation type의 key, 한글 label, briefing 질문과 예시 답변
- briefing에서 수집할 required/optional context slot
- 첫 장면, 첫 상대 발화, 첫 행동 요구, 최소 통과 의도
- 2턴과 3턴에서 달성할 대화 목표
- 재시도 안내와 강제 진행 후 recovery 방향
- 좋은 답변으로 인정할 최소 의도와 피드백 어조
- Gemini 장애 시 사용할 기술적 fallback 턴

정상적인 2턴과 3턴의 실제 대사는 고정하지 않는다. 타입 담당자는 LLM이 따라야 할 목표와 평가 기준을 제공한다.

## 수정하지 않을 공통 영역

타입 튜닝 PR에서는 다음 공통 영역을 수정하지 않는다.

- `SimulationService`, `NextOpponentLineWorker`, `TurnEvaluationWorker`
- `SimulationTurn`, `SimulationTurnAttempt` 및 공통 상태 enum
- JPA entity, mapper, Flyway migration
- 공통 controller, DTO, 프론트 상태머신
- Gemini 공통 JSON parsing 및 polling 처리

공통 계약 변경이 필요하면 타입 튜닝과 분리된 이슈로 제안한다.

## 타입별 문서

- [공용 context slot](context-slots.md)
- [소개팅](date.md)
- [면접](interview.md)
- [첫 출근](first-day.md)
