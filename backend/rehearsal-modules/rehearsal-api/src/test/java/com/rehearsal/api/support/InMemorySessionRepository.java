package com.rehearsal.api.support;

import com.rehearsal.domain.rehearsal.model.RehearsalResult;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.repository.SessionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemorySessionRepository implements SessionRepository {

  private final Map<String, ClientSession> sessions = new HashMap<>();
  private final Map<String, SessionContext> contexts = new HashMap<>();
  private final Map<String, SimulationTurn> turns = new HashMap<>();
  private final Map<String, SimulationTurnAttempt> attempts = new HashMap<>();
  private final Map<String, RehearsalResult> results = new HashMap<>();

  public InMemorySessionRepository(ClientSession... sessions) {
    for (ClientSession session : sessions) {
      saveSession(session);
    }
  }

  @Override
  public ClientSession saveSession(ClientSession session) {
    sessions.put(session.getSessionId(), session);
    return session;
  }

  @Override
  public Optional<ClientSession> findSession(String sessionId) {
    return Optional.ofNullable(sessions.get(sessionId));
  }

  @Override
  public SessionContext saveContext(String sessionId, SessionContext context) {
    contexts.put(sessionId, context);
    return context;
  }

  @Override
  public Optional<SessionContext> findContext(String sessionId) {
    return Optional.ofNullable(contexts.get(sessionId));
  }

  @Override
  public SimulationTurn saveTurn(SimulationTurn turn) {
    turns.put(turnKey(turn.getSessionId(), turn.getTurnNo()), turn);
    return turn;
  }

  @Override
  public Optional<SimulationTurn> findTurn(String sessionId, int turnNo) {
    return Optional.ofNullable(turns.get(turnKey(sessionId, turnNo)));
  }

  @Override
  public SimulationTurnAttempt saveAttempt(SimulationTurnAttempt attempt) {
    attempts.put(attemptKey(attempt.getSimulationTurnId(), attempt.getAttemptNo()), attempt);
    return attempt;
  }

  @Override
  public Optional<SimulationTurnAttempt> findAttempt(Long turnId, int attemptNo) {
    return Optional.ofNullable(attempts.get(attemptKey(turnId, attemptNo)));
  }

  @Override
  public Optional<SimulationTurnAttempt> findLatestAttempt(String sessionId, int turnNo) {
    return findTurn(sessionId, turnNo)
        .flatMap(
            turn ->
                findAttempts(turn.getId()).stream()
                    .max(Comparator.comparingInt(SimulationTurnAttempt::getAttemptNo)));
  }

  @Override
  public List<SimulationTurnAttempt> findAttempts(Long turnId) {
    List<SimulationTurnAttempt> found = new ArrayList<>();
    for (SimulationTurnAttempt attempt : attempts.values()) {
      if (turnId.equals(attempt.getSimulationTurnId())) {
        found.add(attempt);
      }
    }
    return found;
  }

  @Override
  public RehearsalResult saveResult(RehearsalResult result) {
    results.put(result.sessionId(), result);
    return result;
  }

  @Override
  public Optional<RehearsalResult> findResult(String sessionId) {
    return Optional.ofNullable(results.get(sessionId));
  }

  private String turnKey(String sessionId, int turnNo) {
    return sessionId + ":" + turnNo;
  }

  private String attemptKey(Long turnId, int attemptNo) {
    return turnId + ":" + attemptNo;
  }
}
