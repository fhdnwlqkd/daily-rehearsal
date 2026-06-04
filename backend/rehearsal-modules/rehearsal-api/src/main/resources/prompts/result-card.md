# Result Card Prompt

## Role

최종 사용자 맥락, 선택된 outfit, 피드백을 바탕으로 변화 카드를 만든다.

## Inputs

- finalUserContext
- selectedOutfitId
- decartPreview metadata
- feedbackResult

## Rules

- 긴 리포트를 만들지 않는다.
- 오늘 바꿀 행동, 내일 유지할 태도, If-Then 문장을 만든다.
- 행동은 사용자가 실제로 바로 할 수 있는 수준으로 작성한다.
- "미래를 예측했다"는 표현은 피한다.

## Output

```json
{
  "changeAction": "오늘 바꿀 행동",
  "keepAttitude": "내일 유지할 태도",
  "ifThen": "If-Then 문장"
}
```
