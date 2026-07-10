package com.rehearsal.domain.rehearsal.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("next opponent line 비동기 작업 상태. (sessionId, turnNo) 단위로 저장되는 job 레코드")
public record OpponentLineJob(
    String sessionId,
    int turnNo,
    OpponentLineJobStatus status,
    @Description("PENDING/FAILED일 때 null") OpponentLineResult result,
    @Description("서버 로그/디버깅 전용. 클라이언트 응답에는 절대 노출하지 않는다. PENDING/COMPLETED일 때 null")
        String failureReason) {

  public static OpponentLineJob pending(String sessionId, int turnNo) {
    return new OpponentLineJob(sessionId, turnNo, OpponentLineJobStatus.PENDING, null, null);
  }

  public OpponentLineJob complete(OpponentLineResult result) {
    return new OpponentLineJob(sessionId, turnNo, OpponentLineJobStatus.COMPLETED, result, null);
  }

  public OpponentLineJob fail(String failureReason) {
    return new OpponentLineJob(
        sessionId, turnNo, OpponentLineJobStatus.FAILED, null, failureReason);
  }
}
