package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("프론트가 계산해 전달하는 turn별 응답 metrics. AI 피드백의 보조 입력으로만 사용한다")
public record TurnMetrics(Integer responseDelayMs, Double speechRate, Double volume) {}
