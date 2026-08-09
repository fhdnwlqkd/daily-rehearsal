package com.rehearsal.api.config.ai;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.OpponentLineCommand;
import com.rehearsal.domain.rehearsal.port.OpponentLineGeneratorClient;

@Description("외부 AI 호출 없이 local/test 환경에서 deterministic 다음 상대 발화를 만드는 fake generator")
public class FakeOpponentLineGeneratorClient implements OpponentLineGeneratorClient {

  @Override
  public String generate(OpponentLineCommand command) {
    return "그렇군요, turn " + command.currentTurn() + "에서는 어떻게 하실 건가요?";
  }
}
