package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;

@Description("Provider-neutral input for generating a simulation turn plan")
public record OpponentLineCommand(
    SituationType situationType,
    Map<String, Object> finalContext,
    String selectedOutfitId,
    List<ConversationHistory> conversationHistory,
    int currentTurn,
    TurnGenerationMode generationMode,
    String turnObjective,
    String feedbackFocus,
    String recoveryDirection,
    SimulationTurnPlan previousTurnPlan) {}
