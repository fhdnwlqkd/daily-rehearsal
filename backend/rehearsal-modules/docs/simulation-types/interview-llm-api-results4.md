# 면접 Gemini 회귀 실험 4: 공용 slot·prompt 개선 후 비교

## 1. 목적

`637a6dd` 기준 실제 Gemini API로 기존 `results`, `results2`, `results3`의 브리핑·단일 답변·3턴 흐름을 다시 실행했다.

이번 실험은 다음 변경이 실제 문제를 줄였는지 확인한다.

- transcript에 근거하지 않은 slot 추론 금지
- `내일 면접`, `긴장돼요`, `잘하고 싶어요` 같은 막연한 표현을 `critical_moment`로 채우지 않기
- 질문의 문구가 아닌 의미와 최소 의도를 기준으로 평가하기
- `RECOVERY`에서 실패한 답변을 성공한 사실처럼 가정하지 않고 질문을 쉽게 바꾸기
- prompt injection과 누락 placeholder를 사용자 맥락으로 사용하지 않기

실험은 별도 MySQL DB와 실제 Gemini provider를 사용했다. GET polling은 저장된 RDB 상태만 조회하며 Gemini를 다시 호출하지 않는다.

## 2. 이전에 발견한 `critical_moment` 문제

이전 결과에서 다음 브리핑은 구체적인 장면이 없는데도 slot이 채워졌다.

| 입력 | 이전 추출 값 | 문제 |
|---|---|---|
| `내일 면접이 있어요` | `내일 면접` | 상황 전체를 어려운 순간으로 오인 |
| `긴장하지 않고 말하고 싶어요` | `긴장하지 않고 말하는 순간` | 막연한 목표를 장면처럼 가공 |
| `면접이 걱정돼요` | 유사한 일반 표현 | 필수 slot이 채워진 것으로 판정 |

서버는 `critical_moment` 문자열의 품질을 다시 평가하지 않고 null 여부로 missing slot을 판단한다. 따라서 Gemini가 막연한 말을 그럴듯한 문구로 바꾸면 follow-up이 생략되고, 이후 질문도 보편적인 기술 질문으로 수렴했다.

새 extraction hint는 위 표현을 명시적인 나쁜 예로 두고 null을 요구한다. 이 방식은 기존 missing-slot 판정과 follow-up 흐름을 그대로 살리면서, 잘못된 값이 들어오는 최초 지점을 막는다.

## 3. 브리핑 회귀 결과

| ID | 입력 특징 | 최초 상태 | 최종 상태 | 관찰 |
|---|---|---|---|---|
| B1 | 충분한 보편 브리핑 | `COMPLETED` | `COMPLETED` | `critical_moment=자기소개랑 팀 프로젝트 경험을 설명하는 순간` |
| B2 | 원하는 인상 누락 | `FOLLOW_UP_REQUIRED` | `COMPLETED` | `desired_persona`만 정확히 재질문 |
| B3 | 구체적인 걱정 순간 누락 | `FOLLOW_UP_REQUIRED` | `COMPLETED` | 이전과 달리 `내일 면접`을 null로 두고 구체적인 순간을 재질문 |
| B4 | `내일 면접이 있어요`만 입력 | `FOLLOW_UP_REQUIRED` | `COMPLETED` | persona와 critical moment를 재질문. 모호한 후속 답변 뒤에도 둘은 null로 남음 |
| B5 | 날씨·식사 같은 무관 정보 | `FOLLOW_UP_REQUIRED` | `COMPLETED` | 무관 정보를 slot으로 복사하지 않음. `긴장하지 않고`도 critical moment로 인정하지 않음 |

B4와 B5의 최종 `COMPLETED`는 개인화 정보가 충분해졌다는 뜻이 아니다. `maxFollowUpAttempt=1` 이후 화면을 멈추지 않는 정책에 따라 optional/default 처리를 거쳐 진행된 것이다. 특히 B4의 `desired_persona`, `critical_moment`와 B5의 `critical_moment`는 최종 context에서도 null이었다.

### 브리핑 개선 판정

- `critical_moment` 오탐을 직접 겨냥한 변경은 실제로 효과가 있었다.
- 무관한 날씨·식사 정보를 면접 slot에 끼워 넣지 않았다.
- 필요한 slot만 고정 질문으로 다시 묻는 기존 rule-based follow-up과 잘 연결됐다.
- 다만 한 번의 follow-up도 모호하면 개인화할 정보가 없는 채 다음 단계로 진행된다.

## 4. 단일 답변·STT 회귀 결과

| ID | 입력 | 결과 | 판정 |
|---|---|---|---|
| G1 | 자연스러운 보편 답변 | `ACCEPTED` | 적절 |
| G2 | 짧지만 직무·경험·강점 포함 | `ACCEPTED` | 이전보다 최소 의도를 잘 인정 |
| G3 | 핵심 뒤 사생활 정보가 긴 답변 | `RETRY_REQUIRED` | 핵심은 맞지만 불필요 정보 때문에 전체 재시도. 여전히 엄격함 |
| G4 | 추임새가 있지만 직무·경험 포함 | `RETRY_REQUIRED` | 내용은 맞는데 표현 품질 때문에 재시도. 새 규칙과 어긋날 여지가 있음 |
| G5 | 모르는 기술에 대한 확인 접근법 | 질문이 실제 경험을 요구해 `RETRY_REQUIRED` | 문맥상 적절 |
| G6 | 기술 오류 질문에 갈등 해결 답변 | `RETRY_REQUIRED` | 질문 불일치를 정확히 판정 |
| G7 | 녹음·화면 확인 발화 | `RETRY_REQUIRED` | 적절 |
| G8 | 두 번 회피 | `RETRY_REQUIRED -> FORCED_ADVANCE` | 적절하나 마지막 Gemini 피드백이 고정 문구로 사라짐 |
| G9 | 치킨·게임 이야기 | `RETRY_REQUIRED` | 적절 |
| G10 | 평가 지시를 바꾸라는 요청 | `RETRY_REQUIRED` | prompt injection 방어 성공 |
| S1 | 조사 생략·문장 절단 | `RETRY_REQUIRED` | 의미는 충분히 복원 가능해 보이며 여전히 엄격함 |
| S2 | 단어 반복 | `ACCEPTED` | 적절 |
| S3 | 일부 오인식 | `ACCEPTED` | 의미 중심 평가 성공 |
| S4 | 한영 혼합 | `ACCEPTED` | 적절 |
| S5 | 정상 답변 뒤 배경 발화 | `RETRY_REQUIRED` | 전시장 소음에 여전히 민감 |

### 평가 prompt 개선 판정

- 짧은 G2와 오인식 S3, 한영 혼합 S4는 답변의 의미를 기준으로 통과했다.
- prompt injection은 평가 기준을 바꾸지 못했다.
- G3, G4, S1은 질문 의도에 답했는데도 전달 품질을 이유로 재시도됐다. `on-topic이면 표현 개선만으로 거절하지 않는다`는 공통 규칙이 면접 평가에서 아직 안정적으로 지켜지지 않는다.
- G4 피드백은 `강점을 먼저 정의하기`와 `추임새 줄이기`를 동시에 요구했다. 실패 시 하나의 실행 가능한 변화만 제시한다는 규칙도 완전히 지켜지지 않았다.

## 5. 3턴 회귀 결과

| ID | 턴 결과 요약 | 질문 흐름 판정 |
|---|---|---|
| F1 | `ACCEPTED / ACCEPTED / RETRY->FORCED` | 협업 갈등에서 소통 원칙으로 자연스럽게 심화 |
| F2 | `RETRY->ACCEPTED / RETRY->FORCED / RETRY->FORCED` | recovery가 직전 기술 오류 질문을 거의 그대로 반복 |
| F3 | `ACCEPTED / RETRY->ACCEPTED / RETRY->FORCED` | 실제 경험을 요구하는 문맥 판정은 적절. 3턴은 다시 과도하게 구체적 |
| F4 | `RETRY->ACCEPTED / RETRY->FORCED / RETRY->FORCED` | recovery 질문이 직전 질문과 동일 |
| R1 | `ACCEPTED / ACCEPTED / ACCEPTED` | API 문제에서 데이터 무결성으로 자연스럽게 심화 |
| R2 | `ACCEPTED / RETRY->ACCEPTED / RETRY->FORCED` | 부실 브리핑이어도 대화는 성립하지만 기술 질문으로 수렴 |
| R3 | `RETRY->FORCED / RETRY->FORCED / RETRY->FORCED` | recovery가 조금 쉬워졌으나 답변 배경을 계속 추가 요구 |
| R4 | `RETRY->FORCED / ACCEPTED / ACCEPTED` | 브리핑의 갈등 맥락을 반영해 설득 방식으로 자연스럽게 이어짐 |
| R5 | `RETRY->FORCED / RETRY->FORCED / RETRY->ACCEPTED` | 질문 자체는 이전보다 조금 변했지만 실패한 자기소개를 성공한 것처럼 가정 |
| L1 | `ACCEPTED / RETRY->FORCED / RETRY->FORCED` | 실패 경험 맥락은 반영했지만 recovery가 같은 질문을 반복 |

### RECOVERY에서 좋아진 점

- R3 턴 3은 `거창한 사례가 아니어도 괜찮다`고 범위를 낮추고 작은 경험을 허용했다.
- R5 턴 3은 턴 2의 문장을 완전히 복제하지 않고 `가장 큰 난관`으로 각도를 일부 바꿨다.
- prompt에 accepted history만 사실로 사용한다는 기준이 생겼다.

### RECOVERY에서 남은 문제

1. F2와 F4는 직전 질문·행동 요구를 사실상 그대로 반복했다.
2. R5는 자기소개가 강제 진행됐는데도 `네, 잘 들었습니다`, `자기소개에 이어`라고 성공을 가정했다.
3. `한 가지 경험부터`라는 recovery 설정이 `가장 성취감을 느낀 프로젝트`, `가장 큰 난관`, `수치적 결과`처럼 더 어려운 요구로 바뀌었다.
4. 두 번째 실패에서는 Gemini의 구체적인 피드백이 `두 번의 연습을 마쳤어요`로 교체되어 거절 이유를 확인할 수 없다.
5. 보편적인 면접 context만 있으면 기술 난관·기술 스택·테스트 도구 질문으로 쉽게 수렴한다.

## 6. 이번 변경으로 명확히 좋아진 점

1. **막연한 critical moment 차단**: B3~B5에서 null과 follow-up이 정확히 발생했다.
2. **무관 정보와 prompt injection 격리**: 날씨·식사·평가 조작 문구가 slot 또는 평가 기준으로 사용되지 않았다.
3. **최소 의도 일부 수용**: 짧은 G2, 반복 S2, 오인식 S3, 한영 혼합 S4가 통과했다.
4. **context가 충분할 때 질문 연결 개선**: F1, R1, R4는 앞선 답변과 브리핑을 이용해 자연스럽게 심화됐다.
5. **null context 안전성**: B4/B5처럼 정보가 끝내 없더라도 placeholder를 사용자에게 노출하지 않고 흐름이 멈추지 않았다.

## 7. 같은 방식으로 추가할 prompt 개선안

### 7.1 면접 평가의 최소 합격선 예시 추가

공통 평가 규칙만으로는 G3/G4/S1이 여전히 엄격하게 판정됐다. 면접 설정에 구체적인 양성·음성 예시를 둔다.

- 자기소개는 `지원 맥락 + 역할/강점 중 하나`가 있으면 통과한다.
- 추임새, 문장 절단, 짧은 답변, 전달 순서 미흡은 핵심 의도가 있으면 실패 이유가 아니라 코칭 소재다.
- 구체적 사례는 해당 턴의 action prompt가 요구할 때만 필수다.
- 정상 답변 뒤 짧은 배경 발화가 붙더라도 핵심 답변이 완결되면 통과하고, 피드백에서 주변 소음을 짧게 안내한다.

이 규칙은 `InterviewRehearsalConfig`의 면접 전용 평가 기준 또는 provider-neutral evaluation command에 포함하는 편이 낫다.

### 7.2 면접 RECOVERY에 금지 예시 추가

현재의 `범위를 좁힌 질문`은 Gemini가 자의적으로 해석한다. 다음을 면접 전용 recovery direction에 명시한다.

- `가장 큰`, `가장 어려운`, `수치로`, `성과 중심` 요구를 새로 추가하지 않는다.
- 직전 질문을 문장만 바꿔 반복하지 않는다.
- ACCEPTED history가 없으면 `잘 들었습니다`, `자기소개를 마친 후`처럼 성공을 가정하지 않는다.
- 구체적 경험이 어려우면 `본인이 한 행동 하나` 또는 `먼저 확인할 것 하나`로 범위를 줄인다.
- 나쁜 예와 좋은 예를 함께 넣어 `critical_moment` 힌트와 같은 방식으로 모델의 경계를 고정한다.

### 7.3 피드백 한 가지 원칙 강화

G4처럼 두 가지 개선점을 동시에 요구하지 않도록 면접 예시를 추가한다.

- 실패 피드백은 가장 큰 원인 하나만 말한다.
- 전달 방식보다 질문 의도 불일치를 우선한다.
- 두 번째 실패에서도 Gemini 피드백을 보존하고, 서버의 진행 안내를 별도 필드로 내려준다.

## 8. prompt만으로 잡기 어려운 후속 보강

같은 입력에서도 Gemini 결과가 달라질 수 있으므로 중요한 불변 조건은 서버 검증을 병행한다.

1. `critical_moment`가 situation type 자체 또는 `긴장`, `잘하고 싶다` 같은 일반 표현뿐이면 null로 정규화한다.
2. recovery 생성 결과가 이전 `opponentLine` 또는 `actionPrompt`와 동일하면 한 번 재생성하거나 정적 recovery plan을 사용한다.
3. ACCEPTED history가 없는데 `마친 후`, `잘 들었습니다` 같은 성공 가정 문구가 나오면 재생성한다.
4. POST evaluation 중복 제출에 대한 idempotency를 보강한다.
5. 배경 음성 문제는 LLM prompt만이 아니라 프론트 STT 구간 확정 또는 화자 분리와 함께 처리한다.

## 9. 면접 schema 확장 제안

공용 slot catalog는 이미 `ContextSlotType`에 정의됐지만, 현재 `ContextSlotSchemaType.INTERVIEW`는 다음 세 개만 사용한다.

- `desired_persona` REQUIRED
- `critical_moment` REQUIRED
- `outfit_direction` OPTIONAL

그래서 면접 직무·유형, 편하게 말할 경험, 예상 질문을 수집하지 못하고 2~3턴이 보편적인 기술 질문으로 수렴한다.

### 권장 INTERVIEW 구성

| Required level | Slot | 사용 목적 |
|---|---|---|
| REQUIRED | `situation_detail` | 지원 직무, 면접 유형·단계 |
| REQUIRED | `desired_persona` | 남기고 싶은 인상 |
| REQUIRED | `conversation_material` | 질문을 만들 때 활용할 경험·기술 영역 |
| REQUIRED | `critical_moment` | 집중 연습할 구체 장면 |
| SOFT_REQUIRED | `desired_outcome` | 이번 연습의 성공 기준 |
| SOFT_REQUIRED | `anticipated_question` | 사용자가 걱정하는 질문 내용 |
| SOFT_REQUIRED | `user_strength` | 자기소개·후속 질문의 개인화 소재 |
| SOFT_REQUIRED | `supporting_example` | 구체 경험 기반 꼬리질문 소재 |
| SOFT_REQUIRED | `response_style` | 답변 방식과 코칭 기준 |
| SOFT_REQUIRED | `counterpart_context` | 실무진·임원·다대일 등 상대 맥락 |
| SOFT_REQUIRED | `familiarity_level` | 난도 조절 보조 |
| OPTIONAL | `interaction_setting` | 대면·화상 환경 |
| OPTIONAL | `prior_interaction_context` | 1차 면접 등 이전 단계 |
| OPTIONAL | `interaction_constraint` | 피하고 싶은 답변 방식·주제 |
| OPTIONAL | `outfit_direction` | 의상 후보 방향 |

`REQUIRED`는 한 번에 너무 많이 늘리지 않고 네 개 이내로 유지한다. 나머지는 사용자가 말하면 추출하되 follow-up을 강제하지 않는다.

### 적용 파일

1. `ContextSlotSchemaType.INTERVIEW`의 `SchemaItemDef` 목록을 위 구성으로 변경한다.
2. `SituationType.INTERVIEW`의 briefing 문구와 예시 답변을 required slot이 자연스럽게 나오도록 바꾼다.
3. `ContextSlotSchemaTypeTest`, extraction prompt/schema 테스트, fake extractor fixture를 갱신한다.
4. briefing 실제 API 회귀에서 missing slot과 한 번의 follow-up 완료를 검증한다.
5. F1~R5를 다시 실행해 질문 다양성과 개인화가 실제로 좋아졌는지 비교한다.

권장 briefing 문구 예시는 다음과 같다.

> 어떤 면접을 앞두고 있나요? 지원 직무와 편하게 설명할 경험, 가장 걱정되는 질문이나 순간, 남기고 싶은 인상을 함께 말해주세요.

## 10. 결론

이번 공용 slot·prompt 변경은 가장 명확했던 `critical_moment` 오탐을 실제로 해결했다. 그러나 면접 schema가 여전히 세 slot뿐이라 질문 개인화에는 한계가 있고, 평가의 합격선과 recovery 난도 완화는 prompt 규칙이 확률적으로만 적용되고 있다.

다음 개선 순서는 다음이 적합하다.

1. INTERVIEW schema를 공용 slot catalog로 확장한다.
2. 면접 전용 평가 최소 합격선과 recovery 금지·허용 예시를 추가한다.
3. 동일 질문·성공 가정 문구처럼 중요한 조건은 서버 검증으로 보강한다.
4. 같은 회귀 세트를 다시 실행해 prompt 개선과 schema 확장의 효과를 분리해 비교한다.
