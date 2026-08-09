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

  private long turnSequence = 1L;
  private long attemptSequence = 1L;

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
  public Optional<ClientSession> findSessionForUpdate(String sessionId) {
    return findSession(sessionId);
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
    SimulationTurn saved =
        turn.getId() == null
            ? SimulationTurn.restore(
                turnSequence++,
                turn.getSessionId(),
                turn.getTurnNo(),
                turn.getOpponentLineStatus(),
                turn.getOpponentLine(),
                turn.getFailureReason())
            : turn;
    turns.put(turnKey(saved.getSessionId(), saved.getTurnNo()), saved);
    return saved;
  }

  @Override
  public Optional<SimulationTurn> findTurn(String sessionId, int turnNo) {
    return Optional.ofNullable(turns.get(turnKey(sessionId, turnNo)));
  }

  @Override
  public List<SimulationTurn> findTurns(String sessionId) {
    return turns.values().stream()
        .filter(turn -> turn.getSessionId().equals(sessionId))
        .sorted(Comparator.comparingInt(SimulationTurn::getTurnNo))
        .toList();
  }

  @Override
  public SimulationTurnAttempt saveAttempt(SimulationTurnAttempt attempt) {
    SimulationTurnAttempt saved =
        attempt.getId() == null
            ? SimulationTurnAttempt.restore(
                attemptSequence++,
                attempt.getSimulationTurnId(),
                attempt.getAttemptNo(),
                attempt.getUserTranscript(),
                attempt.getEvaluationStatus(),
                attempt.getSuccess(),
                attempt.getFeedback(),
                attempt.getFallback(),
                attempt.getFailureReason())
            : attempt;
    attempts.put(attemptKey(saved.getSimulationTurnId(), saved.getAttemptNo()), saved);
    return saved;
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
