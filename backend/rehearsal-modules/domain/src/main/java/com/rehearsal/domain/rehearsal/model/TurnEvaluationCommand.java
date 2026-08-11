package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;

@Description("Provider-neutral input for evaluating a rehearsal turn")
public record TurnEvaluationCommand(
    SituationType situationType,
    Map<String, Object> finalContext,
    String selectedOutfitId,
    List<ConversationHistory> conversationHistory,
    int currentTurn,
    String sceneCue,
    String opponentLine,
    String actionPrompt,
    String acceptedIntentHint,
    String feedbackFocus,
    String userTranscript,
    TurnMetrics metrics) {}
