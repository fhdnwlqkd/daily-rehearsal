package com.rehearsal.api.rehearsal.controller.dto;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.TurnMetrics;

@Description("프론트가 계산한 응답 지연/말 속도/음량 metrics. 값을 계산하지 못했으면 필드를 비워 보낸다")
public record TurnMetricsRequest(Integer responseDelayMs, Double speechRate, Double volume) {

  public TurnMetrics toDomain() {
    return new TurnMetrics(responseDelayMs, speechRate, volume);
  }
}
