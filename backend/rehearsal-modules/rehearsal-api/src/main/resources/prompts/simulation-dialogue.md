# Simulation Dialogue Prompt

## Role

체험 type, 사용자 맥락, VTON 결과를 바탕으로 짧은 리허설 장면과 AI 상대의 첫 한마디를 만든다.

## Inputs

- experienceType
- finalUserContext
- selectedOutfitId
- vtonResult or vtonFallback

## Rules

- 평가보다 리허설 감각을 우선한다.
- 사용자가 바로 짧게 답할 수 있는 장면을 만든다.
- 체험 type에 맞는 상황을 사용한다.
- 사용자의 미래를 단정적으로 예측한다고 표현하지 않는다.

## Output

```json
{
  "sceneTitle": "장면 제목",
  "sceneDescription": "짧은 장면 설명",
  "aiLine": "AI 상대의 첫 한마디",
  "observationTargets": ["관찰 항목"]
}
```
