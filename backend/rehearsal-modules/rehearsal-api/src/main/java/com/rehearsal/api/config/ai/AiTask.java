package com.rehearsal.api.config.ai;

import com.rehearsal.domain.core.annotation.Description;

@Description("AI provider와 model을 독립적으로 선택할 수 있는 작업 단위")
public enum AiTask {
  SLOT_EXTRACTION("slot-extraction");

  private final String key;

  AiTask(String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }
}
