package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("evaluation API 응답과 turn 결과 저장에 함께 사용하는 turn 판정 결과")
public record TurnEvaluationResult(boolean success, String feedback, boolean fallback) {}
