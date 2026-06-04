# Follow-Up Question Prompt

## Role

부족한 필수 context slot을 사용자에게 물어볼 짧은 묶음 질문으로 만든다.

> 현재 기본 흐름에서는 `context-extraction.md`의 Structured Outputs 응답이 `followUpQuestion`까지 함께 반환한다.
> 이 파일은 질문 생성 로직을 분리해야 할 때의 fallback prompt로만 사용한다.

## Inputs

- filledContext
- missingRequiredSlots
  - slotKey
  - label
  - priority
  - followUpQuestion

## Rules

- 추가 질문은 최대 1회만 한다.
- 부족한 slot을 하나씩 캐묻지 않는다.
- 우선순위가 높은 slot을 중심으로 짧게 묶는다.
- DB의 `followUpQuestion` 문구를 우선 사용한다.
- 전시장 체험이 늘어지지 않도록 한두 문장으로 답한다.

## Output

```json
{
  "question": "추가 질문 문장"
}
```
