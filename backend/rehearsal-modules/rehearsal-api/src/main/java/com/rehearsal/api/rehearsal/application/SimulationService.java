package com.rehearsal.api.rehearsal.application;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigDefinition;
import com.rehearsal.domain.rehearsal.registry.RehearsalConfigRegistry;
import com.rehearsal.domain.rehearsal.usecase.StartSimulationUseCase;
import com.rehearsal.domain.session.cache.SessionCache;
import com.rehearsal.domain.session.model.ClientSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Description("고정 N턴 리허설 시뮬레이션 시작을 처리하는 application service")
@Service
@RequiredArgsConstructor
public class SimulationService implements StartSimulationUseCase {

  private final SessionCache sessionCache;

  @Override
  public SimulationStart startSimulation(String sessionId) {
    ClientSession session = getValidSession(sessionId);
    RehearsalConfigDefinition config = getConfig(session);

    session.startSimulation();
    sessionCache.save(session);

    return new SimulationStart(
        session.getSessionId(),
        session.getCurrentTurn(),
        config.maxTurn(),
        config.firstOpponentLine());
  }

  private ClientSession getValidSession(String sessionId) {
    return sessionCache
        .findById(sessionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
  }

  private RehearsalConfigDefinition getConfig(ClientSession session) {
    return RehearsalConfigRegistry.findByType(session.getSituationType())
        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
  }
}
