package com.rehearsal.datasource.dbintegrated.session.repository;

import com.rehearsal.datasource.dbintegrated.session.entity.SessionContextValueJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionContextValueJpaRepository
    extends JpaRepository<SessionContextValueJpaEntity, Long> {

  List<SessionContextValueJpaEntity> findAllBySessionSessionIdOrderByIdAsc(String sessionId);

  Optional<SessionContextValueJpaEntity> findBySessionSessionIdAndContextKey(
      String sessionId, String contextKey);
}
