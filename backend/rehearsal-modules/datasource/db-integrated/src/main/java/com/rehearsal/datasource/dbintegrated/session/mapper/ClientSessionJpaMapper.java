package com.rehearsal.datasource.dbintegrated.session.mapper;

import com.rehearsal.datasource.dbintegrated.session.entity.RehearsalSessionJpaEntity;
import com.rehearsal.domain.session.model.ClientSession;
import org.springframework.stereotype.Component;

@Component
public class ClientSessionJpaMapper {

  public RehearsalSessionJpaEntity toNewEntity(ClientSession session) {
    return RehearsalSessionJpaEntity.create(
        session.getSessionId(),
        session.getSituationType(),
        session.getStatus(),
        session.getContextStatus(),
        session.getFollowUpAttempt(),
        session.getSelectedOutfitId(),
        session.getCurrentTurn(),
        session.getMaxTurn());
  }

  public void updateEntity(RehearsalSessionJpaEntity entity, ClientSession session) {
    entity.update(
        session.getSituationType(),
        session.getStatus(),
        session.getContextStatus(),
        session.getFollowUpAttempt(),
        session.getSelectedOutfitId(),
        session.getCurrentTurn(),
        session.getMaxTurn());
  }

  public ClientSession toDomain(RehearsalSessionJpaEntity entity) {
    return ClientSession.builder()
        .sessionId(entity.getSessionId())
        .situationType(entity.getSituationType())
        .status(entity.getStatus())
        .contextStatus(entity.getContextStatus())
        .followUpAttempt(entity.getFollowUpAttempt())
        .selectedOutfitId(entity.getSelectedOutfitId())
        .currentTurn(entity.getCurrentTurn())
        .maxTurn(entity.getMaxTurn())
        .build();
  }
}
