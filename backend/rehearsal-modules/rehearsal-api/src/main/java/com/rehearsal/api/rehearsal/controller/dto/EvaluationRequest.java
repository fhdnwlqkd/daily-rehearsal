package com.rehearsal.api.rehearsal.controller.dto;

import com.rehearsal.domain.core.annotation.Description;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Description("turn evaluation 요청. 사용자 응답 STT transcript와 선택적 metrics를 담는다")
public record EvaluationRequest(
    @Description("사용자 응답 STT transcript") @NotBlank String transcript,
    @Description("프론트가 계산한 응답 metrics. 계산하지 못했으면 null") @Valid TurnMetricsRequest metrics) {}
