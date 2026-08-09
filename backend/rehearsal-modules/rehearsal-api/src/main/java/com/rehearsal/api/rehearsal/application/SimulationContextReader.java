package com.rehearsal.api.rehearsal.application;

import com.rehearsal.domain.rehearsal.model.ConversationHistory;
import com.rehearsal.domain.rehearsal.model.EvaluationStatus;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.model.TurnEvaluation;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.repository.SessionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SimulationContextReader {

  private final SessionRepository sessionRepository;

  public Map<String, Object> context(ClientSession session) {
    return sessionRepository
        .findContext(session.getSessionId())
        .orElseGet(() -> SessionContext.empty(session.getSituationType()))
        .valuesWithSituationType();
  }

  public List<ConversationHistory> history(String sessionId, int beforeTurnNo) {
    List<ConversationHistory> history = new ArrayList<>();
    for (SimulationTurn turn : sessionRepository.findTurns(sessionId)) {
      if (turn.getTurnNo() >= beforeTurnNo) {
        continue;
      }
      latestSuccessfulAttempt(turn)
          .ifPresent(
              attempt ->
                  history.add(
                      new ConversationHistory(
                          turn.getTurnNo(),
                          turn.getPlan().opponentLine(),
                          attempt.getUserTranscript())));
    }
    return List.copyOf(history);
  }

  public List<TurnEvaluation> evaluations(String sessionId) {
    List<TurnEvaluation> evaluations = new ArrayList<>();
    for (SimulationTurn turn : sessionRepository.findTurns(sessionId)) {
      latestSuccessfulAttempt(turn)
          .ifPresent(
              attempt ->
                  evaluations.add(
                      new TurnEvaluation(
                          turn.getTurnNo(),
                          attempt.getSuccess(),
                          attempt.getFeedback(),
                          attempt.getFallback())));
    }
    return List.copyOf(evaluations);
  }

  private java.util.Optional<SimulationTurnAttempt> latestSuccessfulAttempt(SimulationTurn turn) {
    return sessionRepository.findAttempts(turn.getId()).stream()
        .filter(attempt -> attempt.getEvaluationStatus() == EvaluationStatus.COMPLETED)
        .filter(attempt -> Boolean.TRUE.equals(attempt.getSuccess()))
        .max(Comparator.comparingInt(SimulationTurnAttempt::getAttemptNo));
  }
}
