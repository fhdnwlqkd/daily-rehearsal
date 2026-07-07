package com.rehearsal.domain.rehearsal.port;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationCommand;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationRawResult;

@Description("turn의 성공/실패와 피드백을 판정하는 외부 AI client port")
public interface TurnEvaluationClient {

  TurnEvaluationRawResult evaluate(TurnEvaluationCommand command);
}
