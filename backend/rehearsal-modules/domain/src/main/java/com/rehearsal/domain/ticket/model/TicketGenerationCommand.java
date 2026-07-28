package com.rehearsal.domain.ticket.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.TurnEvaluation;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;

@Description("provider와 무관하게 ticket copy generator client에 전달하는 입력 계약")
public record TicketGenerationCommand(
    @Description("리허설 상황 타입") SituationType situationType,
    @Description("세션에 누적된 최종 context") Map<String, Object> finalContext,
    @Description("사용자가 선택한 outfit id") String selectedOutfitId,
    @Description("시뮬레이션 전체 대화 history") List<ConversationHistory> conversationHistory,
    @Description("턴별 평가/피드백 결과") List<TurnEvaluation> turnEvaluations) {}
