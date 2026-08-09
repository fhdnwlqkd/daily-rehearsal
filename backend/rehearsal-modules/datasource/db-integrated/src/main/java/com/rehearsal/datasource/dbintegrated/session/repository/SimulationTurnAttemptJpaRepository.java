package com.rehearsal.datasource.dbintegrated.session.repository;

import com.rehearsal.datasource.dbintegrated.session.entity.SimulationTurnAttemptJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationTurnAttemptJpaRepository
    extends JpaRepository<SimulationTurnAttemptJpaEntity, Long> {

  Optional<SimulationTurnAttemptJpaEntity> findBySimulationTurnIdAndAttemptNo(
      Long simulationTurnId, int attemptNo);

  List<SimulationTurnAttemptJpaEntity> findAllBySimulationTurnIdOrderByAttemptNoAsc(
      Long simulationTurnId);

  Optional<SimulationTurnAttemptJpaEntity>
      findTopBySimulationTurnSessionSessionIdAndSimulationTurnTurnNoOrderByAttemptNoDesc(
          String sessionId, int turnNo);
}
