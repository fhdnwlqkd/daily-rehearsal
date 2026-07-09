package com.rehearsal.api.support;

import com.rehearsal.api.session.application.ContextExtractionWorker;
import com.rehearsal.domain.extraction.model.ContextExtractionJob;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class RecordingContextExtractionWorker extends ContextExtractionWorker {

  private final AtomicInteger briefingInvocationCount = new AtomicInteger();
  private final AtomicInteger followUpInvocationCount = new AtomicInteger();
  private boolean rejectNextDispatch = false;

  public RecordingContextExtractionWorker() {
    super(null, null, null, null);
  }

  public void rejectNextDispatch() {
    this.rejectNextDispatch = true;
  }

  @Override
  public void extractBriefingAsync(ContextExtractionJob job, String transcript) {
    briefingInvocationCount.incrementAndGet();
    rejectIfRequested();
  }

  @Override
  public void extractFollowUpAsync(ContextExtractionJob job, String transcript) {
    followUpInvocationCount.incrementAndGet();
    rejectIfRequested();
  }

  public int briefingInvocationCount() {
    return briefingInvocationCount.get();
  }

  public int followUpInvocationCount() {
    return followUpInvocationCount.get();
  }

  private void rejectIfRequested() {
    if (rejectNextDispatch) {
      rejectNextDispatch = false;
      throw new RejectedExecutionException("thread pool exhausted");
    }
  }
}
