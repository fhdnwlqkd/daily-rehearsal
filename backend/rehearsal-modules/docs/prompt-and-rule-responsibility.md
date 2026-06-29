# Prompt, Rule, and Fallback Responsibility

이 문서는 Daily Rehearsal에서 정적 규칙, AI prompt, 프론트 계산, fallback 책임을 구분한다.

## 1. 기본 원칙

- 질문 문구는 AI가 만들지 않는다.
- slot 질문과 default는 enum/정적 매핑에 둔다.
- AI는 사용자 답변을 해석하고 정규화한다.
- AI 실패는 전시 중단 사유가 아니다.
- 모든 AI 호출에는 fallback이 있어야 한다.
- LLM은 stateless로 보고 매 요청에 필요한 context와 history를 모두 전달한다.

## 2. 정적 설정이 담당하는 것

### 상황 타입

- 타입 key
- 사용자 표시 label
- gesture order
- 타입별 브리핑 문구
- 타입별 예시 답변

### Slot 정의

- slot key
- 값 타입
- required 여부
- 고정 질문 문구
- default 값
- 선택지

### Outfit 정의

- outfit id
- 표시 이름
- thumbnail
- Decart prompt/reference parameter
- 노출 가능한 situation type

### Simulation 설정

- 타입별 `maxTurn`
- 첫 상대 발화 fallback
- 다음 상대 발화 fallback
- 실패 시 사용자에게 보여줄 고정 피드백

## 3. AI가 담당하는 것

### Context Normalize

입력:

- `situationType`
- 현재 context
- target slot definitions
- 사용자 transcript

출력:

```json
{
  "slots": {
    "desired_persona": "warm_natural",
    "critical_moment": "첫 인사를 나누는 순간"
  }
}
```

규칙:

- target slot key만 반환한다.
- 판단할 수 없으면 null을 반환한다.
- 질문 문구를 만들지 않는다.
- ready 여부를 판단하지 않는다.

fallback:

- AI 실패 또는 timeout이면 target slot에 default를 적용한다.

### Simulation Evaluation

입력:

- situation type
- final context
- selected outfit
- 전체 conversation history
- 현재 turn
- 사용자 transcript
- 프론트 metrics

출력:

```json
{
  "success": true,
  "feedback": "첫 문장이 짧고 자연스럽습니다.",
  "retryReason": null
}
```

규칙:

- 구조화 JSON으로 동기 응답한다.
- 성공/실패와 피드백만 반환한다.
- 다음 상대 발화는 만들지 않는다.

fallback:

- AI 실패 또는 timeout이면 `success=false`, `feedback="다시 시도해보세요."`로 응답한다.

### Next Opponent Line

입력:

- situation type
- final context
- selected outfit
- 전체 conversation history
- 현재 turn

출력:

- 순수 텍스트 상대 발화
- SSE token stream

규칙:

- JSON을 반환하지 않는다.
- 피드백을 섞지 않는다.
- 토큰 스트리밍 연출을 위해 SSE로 반환한다.

fallback:

- AI streaming 실패 또는 timeout이면 타입별 고정 발화를 반환한다.

## 4. 프론트가 담당하는 것

- MediaPipe 제스처 인식
- Web Speech API STT
- TTS 또는 상대 발화 표시
- Decart WebRTC 직접 연결
- MediaRecorder 녹화
- 기본적인 metrics 계산
  - 응답 지연
  - 말 속도
  - 음량
- 티켓/QR 표시
- 다운로드 페이지 표시

## 5. 백엔드가 담당하는 것

- 세션 생성과 상태 전이
- context 누적
- missing slot 판단
- max_attempt 판단
- default 적용
- Decart URI 빌드
- selectedOutfit 저장
- simulation turn 상태 추적
- 대화 로그 저장
- AI 호출 orchestration
- fallback 적용
- 영상 URL 연결
- 티켓 생성
- QR payload 생성

## 6. Rule로 고정할 것

### 재질문 횟수

`max_attempt`는 재질문 라운드 전체 횟수다. slot별 횟수가 아니다.

### 턴 종료

시뮬레이션은 성공한 turn 수가 `N`에 도달하면 종료한다. 실패 turn은 같은 turn을 재시도하며 실패 횟수 제한은 두지 않는다.

### Metrics

프론트가 계산 가능한 값은 프론트에서 계산한다.

- responseDelayMs
- speechRate
- volume

이 값들은 AI 피드백 입력으로만 사용한다. AI가 계산 결과를 뒤집지 않도록 prompt에서 제한한다.

## 7. Fallback Table

| 실패 지점 | fallback |
| --- | --- |
| context normalize 실패 | target slot default 적용 |
| 빈 slot이 max_attempt 이후에도 남음 | default 적용 |
| Decart URI 생성 실패 | 기본 outfit parameter 사용 |
| Decart WebRTC 실패 | 이미지 카드 fallback |
| evaluation AI 실패 | 실패 처리 + “다시 시도해보세요.” |
| next-line SSE 실패 | 타입별 고정 상대 발화 |
| video upload 실패 | 영상 없이 티켓 발급 또는 재업로드 안내 |
| ticket 생성 실패 | 정적 템플릿 티켓 발급 |
