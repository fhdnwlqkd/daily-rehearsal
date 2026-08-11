package com.rehearsal.datasource.dbintegrated.session.entity;

import com.rehearsal.domain.rehearsal.model.OpponentLineStatus;
import com.rehearsal.domain.rehearsal.model.SimulationTurnPlan;
import com.rehearsal.domain.rehearsal.model.TurnGenerationMode;
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
    name = "simulation_turn",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_simulation_turn_session_turn",
            columnNames = {"session_id", "turn_no"}))
public class SimulationTurnJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private RehearsalSessionJpaEntity session;

  @Column(name = "turn_no", nullable = false)
  private int turnNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "generation_mode", nullable = false, length = 30)
  private TurnGenerationMode generationMode;

  @Enumerated(EnumType.STRING)
  @Column(name = "opponent_line_status", nullable = false, length = 30)
  private OpponentLineStatus opponentLineStatus;

  @Column(name = "scene_cue", columnDefinition = "TEXT")
  private String sceneCue;

  @Column(name = "opponent_line", columnDefinition = "TEXT")
  private String opponentLine;

  @Column(name = "action_prompt", columnDefinition = "TEXT")
  private String actionPrompt;

  @Column(name = "accepted_intent_hint", columnDefinition = "TEXT")
  private String acceptedIntentHint;

  @Column(name = "opponent_line_failure_reason", columnDefinition = "TEXT")
  private String opponentLineFailureReason;

  public static SimulationTurnJpaEntity create(
      RehearsalSessionJpaEntity session,
      int turnNo,
      TurnGenerationMode generationMode,
      OpponentLineStatus opponentLineStatus,
      SimulationTurnPlan plan) {
    SimulationTurnJpaEntity entity = new SimulationTurnJpaEntity();
    entity.session = session;
    entity.turnNo = turnNo;
    entity.generationMode = generationMode;
    entity.opponentLineStatus = opponentLineStatus;
    entity.updatePlan(plan);
    return entity;
  }

  public void updateTurn(
      TurnGenerationMode generationMode,
      OpponentLineStatus opponentLineStatus,
      SimulationTurnPlan plan,
      String failureReason) {
    this.generationMode = generationMode;
    this.opponentLineStatus = opponentLineStatus;
    updatePlan(plan);
    this.opponentLineFailureReason = failureReason;
  }

  private void updatePlan(SimulationTurnPlan plan) {
    this.sceneCue = plan == null ? null : plan.sceneCue();
    this.opponentLine = plan == null ? null : plan.opponentLine();
    this.actionPrompt = plan == null ? null : plan.actionPrompt();
    this.acceptedIntentHint = plan == null ? null : plan.acceptedIntentHint();
  }
}
