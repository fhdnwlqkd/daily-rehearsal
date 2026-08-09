package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("LLM 또는 fake evaluator가 반환한 원시 turn 판정 결과")
public record TurnEvaluationRawResult(boolean success, String feedback) {}
