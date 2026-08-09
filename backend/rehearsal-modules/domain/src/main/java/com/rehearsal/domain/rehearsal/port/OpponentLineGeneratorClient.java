package com.rehearsal.domain.rehearsal.port;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.OpponentLineCommand;

@Description("다음 상대 발화 순수 텍스트를 생성하는 외부 AI client port")
public interface OpponentLineGeneratorClient {

  String generate(OpponentLineCommand command);
}
