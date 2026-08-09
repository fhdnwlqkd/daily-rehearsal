package com.rehearsal.domain.situation.model;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SituationType {
  DATE("date", "소개팅", "내일의 소개팅을 짧게 말해주세요", "내일 소개팅이 있는데 첫 인사가 어색할까 봐 걱정돼요."),
  BUSINESS_MEETING(
      "business_meeting", "비즈니스 미팅", "내일의 비즈니스 미팅을 짧게 말해주세요", "내일 고객 미팅에서 핵심 내용을 차분하게 전달하고 싶어요.");

  private final String key;
  private final String displayName;
  private final String briefingTitle;
  private final String exampleAnswer;

  public static SituationType fromKey(String key) {
    return findByKey(key).orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
  }

  public static Optional<SituationType> findByKey(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    return Arrays.stream(values()).filter(type -> type.key.equals(key.strip())).findFirst();
  }
}
