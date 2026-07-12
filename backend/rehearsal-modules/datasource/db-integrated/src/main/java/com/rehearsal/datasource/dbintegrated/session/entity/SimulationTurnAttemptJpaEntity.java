package com.rehearsal.datasource.dbintegrated.session.entity;

import com.rehearsal.domain.rehearsal.model.EvaluationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "simulation_turn_attempt",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_simulation_turn_attempt_turn_attempt",
            columnNames = {"simulation_turn_id", "attempt_no"}))
public class SimulationTurnAttemptJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "simulation_turn_id", nullable = false)
  private SimulationTurnJpaEntity simulationTurn;

  @Column(name = "attempt_no", nullable = false)
  private int attemptNo;

  @Column(name = "user_transcript", nullable = false, columnDefinition = "TEXT")
  private String userTranscript;

  @Enumerated(EnumType.STRING)
  @Column(name = "evaluation_status", nullable = false, length = 30)
  private EvaluationStatus evaluationStatus;

  @Column(name = "success")
  private Boolean success;

  @Column(name = "feedback", columnDefinition = "TEXT")
  private String feedback;

  @Column(name = "fallback")
  private Boolean fallback;

  @Column(name = "evaluation_failure_reason", columnDefinition = "TEXT")
  private String evaluationFailureReason;

  public static SimulationTurnAttemptJpaEntity create(
      SimulationTurnJpaEntity simulationTurn,
      int attemptNo,
      String userTranscript,
      EvaluationStatus evaluationStatus) {
    SimulationTurnAttemptJpaEntity entity = new SimulationTurnAttemptJpaEntity();
    entity.simulationTurn = simulationTurn;
    entity.attemptNo = attemptNo;
    entity.userTranscript = userTranscript;
    entity.evaluationStatus = evaluationStatus;
    return entity;
  }

  public void updateEvaluation(
      EvaluationStatus evaluationStatus,
      Boolean success,
      String feedback,
      Boolean fallback,
      String failureReason) {
    this.evaluationStatus = evaluationStatus;
    this.success = success;
    this.feedback = feedback;
    this.fallback = fallback;
    this.evaluationFailureReason = failureReason;
  }
}
