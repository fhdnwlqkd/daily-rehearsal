package com.rehearsal.datasource.dbintegrated.session.repository;

import com.rehearsal.datasource.dbintegrated.session.entity.RehearsalSessionJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RehearsalSessionJpaRepository
    extends JpaRepository<RehearsalSessionJpaEntity, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select session from RehearsalSessionJpaEntity session where session.sessionId = :sessionId")
  Optional<RehearsalSessionJpaEntity> findByIdForUpdate(@Param("sessionId") String sessionId);
}
