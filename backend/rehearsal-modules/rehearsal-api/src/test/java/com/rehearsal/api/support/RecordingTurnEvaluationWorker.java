package com.rehearsal.api.support;

import com.rehearsal.api.rehearsal.application.TurnEvaluationWorker;
import com.rehearsal.domain.rehearsal.model.TurnMetrics;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link TurnEvaluationWorker}를 상속해 실제 evaluateAsync 로직(세션 재조회, Gemini 호출)을 건너뛰고 호출 횟수만 기록하는 테스트
 * 더블. dispatch 실패(스레드풀 rejection) 시나리오를 재현하기 위해 예외를 던지도록 설정할 수도 있다.
 */
public class RecordingTurnEvaluationWorker extends TurnEvaluationWorker {

  private final AtomicInteger invocationCount = new AtomicInteger();
  private boolean rejectNextDispatch = false;

  public RecordingTurnEvaluationWorker() {
    super(null, null, null, null);
  }

  public void rejectNextDispatch() {
    this.rejectNextDispatch = true;
  }

  @Override
  public void evaluateAsync(
      String sessionId, int turnNo, String userTranscript, TurnMetrics metrics) {
    invocationCount.incrementAndGet();
    if (rejectNextDispatch) {
      rejectNextDispatch = false;
      throw new RejectedExecutionException("thread pool exhausted");
    }
  }

  public int invocationCount() {
    return invocationCount.get();
  }
}
