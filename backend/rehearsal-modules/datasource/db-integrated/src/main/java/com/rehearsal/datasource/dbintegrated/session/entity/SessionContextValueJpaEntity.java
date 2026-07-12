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
    name = "session_context_value",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_session_context_value_session_key",
            columnNames = {"session_id", "context_key"}))
public class SessionContextValueJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private RehearsalSessionJpaEntity session;

  @Column(name = "context_key", nullable = false, length = 100)
  private String contextKey;

  @Column(name = "context_value", nullable = false, columnDefinition = "TEXT")
  private String contextValue;

  public static SessionContextValueJpaEntity create(
      RehearsalSessionJpaEntity session, String contextKey, String contextValue) {
    SessionContextValueJpaEntity entity = new SessionContextValueJpaEntity();
    entity.session = session;
    entity.contextKey = contextKey;
    entity.contextValue = contextValue;
    return entity;
  }

  public void updateValue(String contextValue) {
    this.contextValue = contextValue;
  }
}
