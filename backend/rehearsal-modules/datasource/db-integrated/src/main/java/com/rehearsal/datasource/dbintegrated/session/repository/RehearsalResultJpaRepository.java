package com.rehearsal.datasource.dbintegrated.session.repository;

import com.rehearsal.datasource.dbintegrated.session.entity.RehearsalResultJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RehearsalResultJpaRepository
    extends JpaRepository<RehearsalResultJpaEntity, Long> {

  Optional<RehearsalResultJpaEntity> findBySessionSessionId(String sessionId);
}
