package com.rehearsal.api.ticket.application;

import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.ticket.model.TicketJob;
import com.rehearsal.domain.ticket.model.TicketJobStatus;
import com.rehearsal.domain.ticket.port.TicketJobStore;
import com.rehearsal.domain.ticket.usecase.GetTicketGenerationUseCase;
import com.rehearsal.domain.ticket.usecase.SubmitTicketGenerationUseCase;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Description("티켓 발급 job 제출·조회 및 시뮬레이션 완료 여부 검증을 처리하는 application service")
@Service
@RequiredArgsConstructor
public class TicketService implements SubmitTicketGenerationUseCase, GetTicketGenerationUseCase {

  private static final Logger log = LoggerFactory.getLogger(TicketService.class);

  private final SessionReader sessionReader;
  private final TicketJobStore ticketJobStore;
  private final TicketGenerationWorker ticketGenerationWorker;

  @Override
  public TicketJob submit(String sessionId) {
    ClientSession session = sessionReader.get(sessionId);
    validateSimulationFinished(session);

    Optional<TicketJob> existing = ticketJobStore.findById(sessionId);
    if (existing.isPresent() && existing.get().status() != TicketJobStatus.FAILED) {
      // 클라이언트의 네트워크 재시도로 인한 중복 제출은 Gemini를 다시 호출하지 않고 기존 job을 그대로 돌려준다.
      return existing.get();
    }

    TicketJob pendingJob = TicketJob.pending(sessionId);
    ticketJobStore.save(pendingJob);
    try {
      ticketGenerationWorker.generateAsync(sessionId);
    } catch (RuntimeException exception) {
      // 스레드풀+큐가 모두 찬 경우 AbortPolicy가 dispatch 시점에 동기적으로 예외를 던진다.
      log.error("Failed to dispatch ticket generation job for session {}", sessionId, exception);
      TicketJob failedJob = pendingJob.fail("작업을 시작하지 못했습니다.");
      ticketJobStore.save(failedJob);
      return failedJob;
    }
    return pendingJob;
  }

  @Override
  public TicketJob get(String sessionId) {
    return ticketJobStore
        .findById(sessionId)
        .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_JOB_NOT_FOUND));
  }

  private void validateSimulationFinished(ClientSession session) {
    if (session.getStatus() != SessionStatus.REHEARSAL_PLAYING
        || session.getCurrentTurn() <= session.getMaxTurn()) {
      throw new BusinessException(ErrorCode.SIMULATION_NOT_COMPLETED);
    }
  }
}
