ALTER TABLE simulation_turn
    ADD COLUMN generation_mode VARCHAR(30) NOT NULL DEFAULT 'NORMAL' AFTER turn_no,
    ADD COLUMN scene_cue TEXT NULL AFTER opponent_line_status,
    ADD COLUMN action_prompt TEXT NULL AFTER opponent_line,
    ADD COLUMN accepted_intent_hint TEXT NULL AFTER action_prompt;

UPDATE simulation_turn
SET scene_cue = '상대가 대화를 이어갑니다.',
    action_prompt = '상대의 말에 자연스럽게 답해보세요.',
    accepted_intent_hint = '상대 발화의 의도에 맞게 답한다.'
WHERE opponent_line IS NOT NULL;
