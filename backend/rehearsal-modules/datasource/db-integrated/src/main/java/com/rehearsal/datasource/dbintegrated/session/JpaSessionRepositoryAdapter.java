package com.rehearsal.datasource.dbintegrated.session;

import com.rehearsal.datasource.dbintegrated.session.entity.RehearsalResultJpaEntity;
import com.rehearsal.datasource.dbintegrated.session.entity.RehearsalSessionJpaEntity;
import com.rehearsal.datasource.dbintegrated.session.entity.SessionContextValueJpaEntity;
import com.rehearsal.datasource.dbintegrated.session.entity.SimulationTurnAttemptJpaEntity;
import com.rehearsal.datasource.dbintegrated.session.entity.SimulationTurnJpaEntity;
import com.rehearsal.datasource.dbintegrated.session.mapper.ClientSessionJpaMapper;
import com.rehearsal.datasource.dbintegrated.session.mapper.RehearsalResultJpaMapper;
import com.rehearsal.datasource.dbintegrated.session.mapper.SessionContextJpaMapper;
import com.rehearsal.datasource.dbintegrated.session.mapper.SimulationJpaMapper;
import com.rehearsal.datasource.dbintegrated.session.repository.RehearsalResultJpaRepository;
import com.rehearsal.datasource.dbintegrated.session.repository.RehearsalSessionJpaRepository;
import com.rehearsal.datasource.dbintegrated.session.repository.SessionContextValueJpaRepository;
import com.rehearsal.datasource.dbintegrated.session.repository.SimulationTurnAttemptJpaRepository;
import com.rehearsal.datasource.dbintegrated.session.repository.SimulationTurnJpaRepository;
import com.rehearsal.domain.rehearsal.model.RehearsalResult;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.repository.SessionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaSessionRepositoryAdapter implements SessionRepository {

  private final RehearsalSessionJpaRepository sessionJpaRepository;
  private final SessionContextValueJpaRepository contextValueJpaRepository;
  private final SimulationTurnJpaRepository turnJpaRepository;
  private final SimulationTurnAttemptJpaRepository attemptJpaRepository;
  private final RehearsalResultJpaRepository resultJpaRepository;
  private final ClientSessionJpaMapper clientSessionMapper;
  private final SessionContextJpaMapper sessionContextMapper;
  private final SimulationJpaMapper simulationMapper;
  private final RehearsalResultJpaMapper rehearsalResultMapper;

  @Override
  @Transactional
  public ClientSession saveSession(ClientSession session) {
    RehearsalSessionJpaEntity entity =
        sessionJpaRepository
            .findById(session.getSessionId())
            .map(
                existing -> {
                  clientSessionMapper.updateEntity(existing, session);
                  return existing;
                })
            .orElseGet(() -> clientSessionMapper.toNewEntity(session));
    return clientSessionMapper.toDomain(sessionJpaRepository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ClientSession> findSession(String sessionId) {
    return sessionJpaRepository.findById(sessionId).map(clientSessionMapper::toDomain);
  }

  @Override
  @Transactional
  public SessionContext saveContext(String sessionId, SessionContext context) {
    RehearsalSessionJpaEntity session = requiredSession(sessionId);
    List<SessionContextValueJpaEntity> entities = new ArrayList<>();
    context
        .values()
        .forEach(
            (key, value) -> {
              SessionContextValueJpaEntity entity =
                  contextValueJpaRepository
                      .findBySessionSessionIdAndContextKey(sessionId, key)
                      .orElseGet(
                          () ->
                              SessionContextValueJpaEntity.create(
                                  session, key, sessionContextMapper.writeValue(value)));
              entity.updateValue(sessionContextMapper.writeValue(value));
              entities.add(entity);
            });
    contextValueJpaRepository.saveAll(entities);
    return context;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SessionContext> findContext(String sessionId) {
    return sessionJpaRepository
        .findById(sessionId)
        .map(
            session ->
                sessionContextMapper.toDomain(
                    session.getSituationType(),
                    contextValueJpaRepository.findAllBySessionSessionIdOrderByIdAsc(sessionId)));
  }

  @Override
  @Transactional
  public SimulationTurn saveTurn(SimulationTurn turn) {
    SimulationTurnJpaEntity entity =
        turnJpaRepository
            .findBySessionSessionIdAndTurnNo(turn.getSessionId(), turn.getTurnNo())
            .map(
                existing -> {
                  simulationMapper.updateTurnEntity(existing, turn);
                  return existing;
                })
            .orElseGet(
                () -> simulationMapper.toNewTurnEntity(requiredSession(turn.getSessionId()), turn));
    return simulationMapper.toDomain(turnJpaRepository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SimulationTurn> findTurn(String sessionId, int turnNo) {
    return turnJpaRepository
        .findBySessionSessionIdAndTurnNo(sessionId, turnNo)
        .map(simulationMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SimulationTurn> findTurns(String sessionId) {
    return turnJpaRepository.findAllBySessionSessionIdOrderByTurnNoAsc(sessionId).stream()
        .map(simulationMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional
  public SimulationTurnAttempt saveAttempt(SimulationTurnAttempt attempt) {
    SimulationTurnJpaEntity turn = requiredTurn(attempt.getSimulationTurnId());
    SimulationTurnAttemptJpaEntity entity =
        attemptJpaRepository
            .findBySimulationTurnIdAndAttemptNo(turn.getId(), attempt.getAttemptNo())
            .map(
                existing -> {
                  simulationMapper.updateAttemptEntity(existing, attempt);
                  return existing;
                })
            .orElseGet(() -> simulationMapper.toNewAttemptEntity(turn, attempt));
    return simulationMapper.toDomain(attemptJpaRepository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SimulationTurnAttempt> findAttempt(Long turnId, int attemptNo) {
    return attemptJpaRepository
        .findBySimulationTurnIdAndAttemptNo(turnId, attemptNo)
        .map(simulationMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SimulationTurnAttempt> findLatestAttempt(String sessionId, int turnNo) {
    return attemptJpaRepository
        .findTopBySimulationTurnSessionSessionIdAndSimulationTurnTurnNoOrderByAttemptNoDesc(
            sessionId, turnNo)
        .map(simulationMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SimulationTurnAttempt> findAttempts(Long turnId) {
    return attemptJpaRepository.findAllBySimulationTurnIdOrderByAttemptNoAsc(turnId).stream()
        .map(simulationMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional
  public RehearsalResult saveResult(RehearsalResult result) {
    RehearsalResultJpaEntity entity =
        resultJpaRepository
            .findBySessionSessionId(result.sessionId())
            .map(
                existing -> {
                  rehearsalResultMapper.updateEntity(existing, result);
                  return existing;
                })
            .orElseGet(
                () ->
                    rehearsalResultMapper.toNewEntity(requiredSession(result.sessionId()), result));
    return rehearsalResultMapper.toDomain(resultJpaRepository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<RehearsalResult> findResult(String sessionId) {
    return resultJpaRepository
        .findBySessionSessionId(sessionId)
        .map(rehearsalResultMapper::toDomain);
  }

  private RehearsalSessionJpaEntity requiredSession(String sessionId) {
    return sessionJpaRepository
        .findById(sessionId)
        .orElseThrow(() -> new IllegalStateException("Session entity not found: " + sessionId));
  }

  private SimulationTurnJpaEntity requiredTurn(Long turnId) {
    return turnJpaRepository
        .findById(turnId)
        .orElseThrow(
            () -> new IllegalStateException("Simulation turn entity not found: " + turnId));
  }
}
