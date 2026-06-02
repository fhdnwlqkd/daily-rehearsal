# Daily Rehearsal Prompts

이 폴더는 LLM에게 맡길 자연어 판단과 생성 규칙을 관리한다.

DB는 체험 type과 context slot 같은 구조화 설정을 저장하고, 이 prompt 파일들은 LLM의 역할, 출력 형식, 말투, 금지 사항을 정의한다.

MVP prompt 파일:

- `context-extraction.md`: 사용자 브리핑에서 context slot 값을 추출한다.
- `follow-up-question.md`: 부족한 필수 slot을 1회 묶음 질문으로 만든다.
- `simulation-dialogue.md`: 체험 type과 사용자 맥락에 맞는 리허설 장면을 만든다.
- `feedback-generation.md`: 리허설 응답과 정량 rule 결과를 바탕으로 피드백을 만든다.
- `result-card.md`: 최종 변화 카드 내용을 만든다.
