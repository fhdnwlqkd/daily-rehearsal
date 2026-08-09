package com.rehearsal.datasource.dbintegrated.session.mapper;

import com.rehearsal.datasource.dbintegrated.session.entity.RehearsalResultJpaEntity;
import com.rehearsal.datasource.dbintegrated.session.entity.RehearsalSessionJpaEntity;
import com.rehearsal.domain.rehearsal.model.RehearsalResult;
import org.springframework.stereotype.Component;

@Component
public class RehearsalResultJpaMapper {

  public RehearsalResultJpaEntity toNewEntity(
      RehearsalSessionJpaEntity session, RehearsalResult result) {
    return RehearsalResultJpaEntity.create(
        session, result.videoUrl(), result.ticketSummary(), result.downloadUrl());
  }

  public void updateEntity(RehearsalResultJpaEntity entity, RehearsalResult result) {
    entity.update(result.videoUrl(), result.ticketSummary(), result.downloadUrl());
  }

  public RehearsalResult toDomain(RehearsalResultJpaEntity entity) {
    return new RehearsalResult(
        entity.getId(),
        entity.getSession().getSessionId(),
        entity.getVideoUrl(),
        entity.getTicketSummary(),
        entity.getDownloadUrl());
  }
}
