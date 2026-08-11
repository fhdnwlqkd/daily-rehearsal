package com.rehearsal.api.config.ai;

import com.rehearsal.domain.rehearsal.model.OpponentLineCommand;
import com.rehearsal.domain.rehearsal.model.SimulationTurnPlan;
import com.rehearsal.domain.rehearsal.port.OpponentLineGeneratorClient;

public class FakeOpponentLineGeneratorClient implements OpponentLineGeneratorClient {

  @Override
  public SimulationTurnPlan generate(OpponentLineCommand command) {
    return new SimulationTurnPlan(
        "상대방이 대화를 이어가려고 합니다.",
        "그렇군요. 조금 더 자세히 말씀해 주시겠어요?",
        command.turnObjective(),
        command.turnObjective());
  }
}
