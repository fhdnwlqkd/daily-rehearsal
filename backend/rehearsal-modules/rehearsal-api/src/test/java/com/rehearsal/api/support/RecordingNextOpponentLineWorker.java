package com.rehearsal.api.support;

import com.rehearsal.api.rehearsal.application.NextOpponentLineWorker;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link NextOpponentLineWorker}를 상속해 실제 generateAsync 로직(세션 재조회, Gemini 호출)을 건너뛰고 호출 횟수만 기록하는 테스트
 * 더블. dispatch 실패(스레드풀 rejection) 시나리오를 재현하기 위해 예외를 던지도록 설정할 수도 있다.
 */
public class RecordingNextOpponentLineWorker extends NextOpponentLineWorker {

  private final AtomicInteger invocationCount = new AtomicInteger();
  private boolean rejectNextDispatch = false;

  public RecordingNextOpponentLineWorker() {
    super(null, null, null, null);
  }

  public void rejectNextDispatch() {
    this.rejectNextDispatch = true;
  }

  @Override
  public void generateAsync(String sessionId, int turnNo) {
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
