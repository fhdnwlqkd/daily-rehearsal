package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("next-line API 응답과 세션 currentOpponentLine 갱신에 함께 사용하는 발화 생성 결과")
public record OpponentLineResult(SimulationTurnPlan plan, boolean fallback) {}
