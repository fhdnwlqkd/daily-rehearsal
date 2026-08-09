package com.rehearsal.api.config.ai;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationCommand;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationRawResult;
import com.rehearsal.domain.rehearsal.port.TurnEvaluationClient;

@Description("외부 AI 호출 없이 local/test 환경에서 deterministic turn 판정 결과를 만드는 fake evaluator")
public class FakeTurnEvaluationClient implements TurnEvaluationClient {

  @Override
  public TurnEvaluationRawResult evaluate(TurnEvaluationCommand command) {
    String transcript = command.userTranscript() == null ? "" : command.userTranscript().strip();
    if (transcript.isBlank()) {
      return new TurnEvaluationRawResult(false, "답변이 비어 있어요. 다시 말씀해주세요.");
    }
    return new TurnEvaluationRawResult(true, "자연스러운 답변입니다.");
  }
}
