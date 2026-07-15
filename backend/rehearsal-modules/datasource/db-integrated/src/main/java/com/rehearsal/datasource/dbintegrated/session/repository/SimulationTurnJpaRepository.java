package com.rehearsal.datasource.dbintegrated.session.repository;

import com.rehearsal.datasource.dbintegrated.session.entity.SimulationTurnJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationTurnJpaRepository extends JpaRepository<SimulationTurnJpaEntity, Long> {

  Optional<SimulationTurnJpaEntity> findBySessionSessionIdAndTurnNo(String sessionId, int turnNo);

  List<SimulationTurnJpaEntity> findAllBySessionSessionIdOrderByTurnNoAsc(String sessionId);
}
