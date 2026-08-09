package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;

@Description("provider와 무관하게 turn evaluation client에 전달하는 입력 계약")
public record TurnEvaluationCommand(
    @Description("리허설 상황 타입") SituationType situationType,
    @Description("세션에 누적된 최종 context") Map<String, Object> finalContext,
    @Description("사용자가 선택한 outfit id") String selectedOutfitId,
    @Description("현재 turn 이전까지의 전체 대화 history") List<ConversationHistory> conversationHistory,
    @Description("현재 판정 대상 turn 번호") int currentTurn,
    @Description("이번 turn에서 사용자가 응답한 상대 발화") String opponentLine,
    @Description("사용자 응답 transcript") String userTranscript,
    @Description("프론트가 계산한 응답 metrics") TurnMetrics metrics) {}
