package com.rehearsal.datasource.client.openai.prompt;

import com.rehearsal.domain.core.annotation.Description;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Description("OpenAI client에서 사용하는 프롬프트 계약의 종류와 이름을 관리하는 enum")
@Getter
@RequiredArgsConstructor
public enum OpenAiPromptType {
  @Description("사용자 transcript에서 Daily Rehearsal context slot 원시 값을 추출하는 프롬프트")
  CONTEXT_SLOT_EXTRACTION(
      "context_slot_extraction", "Extract Daily Rehearsal context slots from a user transcript.");

  private final String promptName;
  private final String description;
}
