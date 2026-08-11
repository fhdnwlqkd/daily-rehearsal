package com.rehearsal.domain.session.repository;

import com.rehearsal.domain.rehearsal.model.RehearsalResult;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionContext;
import java.util.List;
import java.util.Optional;

public interface SessionRepository {

  ClientSession saveSession(ClientSession session);

  Optional<ClientSession> findSession(String sessionId);

  Optional<ClientSession> findSessionForUpdate(String sessionId);

  SessionContext saveContext(String sessionId, SessionContext context);

  Optional<SessionContext> findContext(String sessionId);

  SimulationTurn saveTurn(SimulationTurn turn);

  Optional<SimulationTurn> findTurn(String sessionId, int turnNo);

  List<SimulationTurn> findTurns(String sessionId);

  SimulationTurnAttempt saveAttempt(SimulationTurnAttempt attempt);

  Optional<SimulationTurnAttempt> findAttempt(Long turnId, int attemptNo);

  Optional<SimulationTurnAttempt> findLatestAttempt(String sessionId, int turnNo);

  List<SimulationTurnAttempt> findAttempts(Long turnId);

  RehearsalResult saveResult(RehearsalResult result);

  Optional<RehearsalResult> findResult(String sessionId);
}
