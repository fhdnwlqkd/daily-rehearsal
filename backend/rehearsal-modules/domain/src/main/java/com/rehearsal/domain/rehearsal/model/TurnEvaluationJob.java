package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("turn evaluation 비동기 작업 상태. (sessionId, turnNo) 단위로 저장되는 job 레코드")
public record TurnEvaluationJob(
    String sessionId,
    int turnNo,
    TurnEvaluationJobStatus status,
    @Description("PENDING/FAILED일 때 null") TurnEvaluationResult result,
    @Description("서버 로그/디버깅 전용. 클라이언트 응답에는 절대 노출하지 않는다. PENDING/COMPLETED일 때 null")
        String failureReason) {

  public static TurnEvaluationJob pending(String sessionId, int turnNo) {
    return new TurnEvaluationJob(sessionId, turnNo, TurnEvaluationJobStatus.PENDING, null, null);
  }

  public TurnEvaluationJob complete(TurnEvaluationResult result) {
    return new TurnEvaluationJob(
        sessionId, turnNo, TurnEvaluationJobStatus.COMPLETED, result, null);
  }

  public TurnEvaluationJob fail(String failureReason) {
    return new TurnEvaluationJob(
        sessionId, turnNo, TurnEvaluationJobStatus.FAILED, null, failureReason);
  }
}
