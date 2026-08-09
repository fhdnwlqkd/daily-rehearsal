package com.rehearsal.datasource.dbintegrated.session.mapper;

import com.rehearsal.datasource.dbintegrated.session.entity.RehearsalSessionJpaEntity;
import com.rehearsal.datasource.dbintegrated.session.entity.SimulationTurnAttemptJpaEntity;
import com.rehearsal.datasource.dbintegrated.session.entity.SimulationTurnJpaEntity;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.model.SimulationTurnPlan;
import org.springframework.stereotype.Component;

@Component
public class SimulationJpaMapper {

  public SimulationTurnJpaEntity toNewTurnEntity(
      RehearsalSessionJpaEntity session, SimulationTurn turn) {
    return SimulationTurnJpaEntity.create(
        session,
        turn.getTurnNo(),
        turn.getGenerationMode(),
        turn.getOpponentLineStatus(),
        turn.getPlan());
  }

  public void updateTurnEntity(SimulationTurnJpaEntity entity, SimulationTurn turn) {
    entity.updateTurn(
        turn.getGenerationMode(),
        turn.getOpponentLineStatus(),
        turn.getPlan(),
        turn.getFailureReason());
  }

  public SimulationTurn toDomain(SimulationTurnJpaEntity entity) {
    return SimulationTurn.restore(
        entity.getId(),
        entity.getSession().getSessionId(),
        entity.getTurnNo(),
        entity.getGenerationMode(),
        entity.getOpponentLineStatus(),
        plan(entity),
        entity.getOpponentLineFailureReason());
  }

  private SimulationTurnPlan plan(SimulationTurnJpaEntity entity) {
    if (entity.getOpponentLine() == null) {
      return null;
    }
    return new SimulationTurnPlan(
        entity.getSceneCue(),
        entity.getOpponentLine(),
        entity.getActionPrompt(),
        entity.getAcceptedIntentHint());
  }

  public SimulationTurnAttemptJpaEntity toNewAttemptEntity(
      SimulationTurnJpaEntity turn, SimulationTurnAttempt attempt) {
    return SimulationTurnAttemptJpaEntity.create(
        turn, attempt.getAttemptNo(), attempt.getUserTranscript(), attempt.getEvaluationStatus());
  }

  public void updateAttemptEntity(
      SimulationTurnAttemptJpaEntity entity, SimulationTurnAttempt attempt) {
    entity.updateEvaluation(
        attempt.getEvaluationStatus(),
        attempt.getSuccess(),
        attempt.getFeedback(),
        attempt.getFallback(),
        attempt.getFailureReason());
  }

  public SimulationTurnAttempt toDomain(SimulationTurnAttemptJpaEntity entity) {
    return SimulationTurnAttempt.restore(
        entity.getId(),
        entity.getSimulationTurn().getId(),
        entity.getAttemptNo(),
        entity.getUserTranscript(),
        entity.getEvaluationStatus(),
        entity.getSuccess(),
        entity.getFeedback(),
        entity.getFallback(),
        entity.getEvaluationFailureReason());
  }
}
