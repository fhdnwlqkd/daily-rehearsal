package com.rehearsal.datasource.dbintegrated.session.entity;

import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.situation.model.SituationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "rehearsal_session")
public class RehearsalSessionJpaEntity extends BaseJpaEntity {

  @Id
  @Column(name = "session_id", nullable = false, length = 36)
  private String sessionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "situation_type", nullable = false, length = 50)
  private SituationType situationType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private SessionStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "context_status", nullable = false, length = 50)
  private ContextStatus contextStatus;

  @Column(name = "follow_up_attempt", nullable = false)
  private int followUpAttempt;

  @Column(name = "selected_outfit_id", length = 100)
  private String selectedOutfitId;

  @Column(name = "current_turn", nullable = false)
  private int currentTurn;

  @Column(name = "max_turn", nullable = false)
  private int maxTurn;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  public static RehearsalSessionJpaEntity create(
      String sessionId,
      SituationType situationType,
      SessionStatus status,
      ContextStatus contextStatus,
      int followUpAttempt,
      String selectedOutfitId,
      int currentTurn,
      int maxTurn) {
    RehearsalSessionJpaEntity entity = new RehearsalSessionJpaEntity();
    entity.sessionId = sessionId;
    entity.update(
        situationType,
        status,
        contextStatus,
        followUpAttempt,
        selectedOutfitId,
        currentTurn,
        maxTurn);
    return entity;
  }

  public void update(
      SituationType situationType,
      SessionStatus status,
      ContextStatus contextStatus,
      int followUpAttempt,
      String selectedOutfitId,
      int currentTurn,
      int maxTurn) {
    this.situationType = situationType;
    this.status = status;
    this.contextStatus = contextStatus;
    this.followUpAttempt = followUpAttempt;
    this.selectedOutfitId = selectedOutfitId;
    this.currentTurn = currentTurn;
    this.maxTurn = maxTurn;
  }

  public void complete(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }
}
