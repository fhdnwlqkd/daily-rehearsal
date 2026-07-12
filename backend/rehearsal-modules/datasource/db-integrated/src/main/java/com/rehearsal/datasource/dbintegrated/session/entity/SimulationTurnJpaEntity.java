package com.rehearsal.datasource.dbintegrated.session.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

  @Column(name = "opponent_line_status", nullable = false, length = 30)
  private String opponentLineStatus;

  @Column(name = "opponent_line", columnDefinition = "TEXT")
  private String opponentLine;

  @Column(name = "opponent_line_failure_reason", columnDefinition = "TEXT")
  private String opponentLineFailureReason;

  public static SimulationTurnJpaEntity create(
      RehearsalSessionJpaEntity session,
      int turnNo,
      String opponentLineStatus,
      String opponentLine) {
    SimulationTurnJpaEntity entity = new SimulationTurnJpaEntity();
    entity.session = session;
    entity.turnNo = turnNo;
    entity.opponentLineStatus = opponentLineStatus;
    entity.opponentLine = opponentLine;
    return entity;
  }

  public void updateOpponentLine(
      String opponentLineStatus, String opponentLine, String failureReason) {
    this.opponentLineStatus = opponentLineStatus;
    this.opponentLine = opponentLine;
    this.opponentLineFailureReason = failureReason;
  }
}
