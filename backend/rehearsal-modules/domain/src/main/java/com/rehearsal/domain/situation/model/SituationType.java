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
  DATE(
      "date",
      "소개팅",
      "내일의 소개팅에서 어떤 인상을 남기고 싶은지 말해주세요.",
      "처음 만났을 때 어색하지 않게 인사하고 따뜻하고 자연스러운 인상을 주고 싶어요."),
  INTERVIEW(
      "interview",
      "면접",
      "내일의 면접과 가장 걱정되는 질문을 말해주세요.",
      "백엔드 개발자 면접에서 프로젝트 기여도를 구체적으로 설명하는 것이 걱정돼요."),
  FIRST_DAY(
      "first_day",
      "첫 출근",
      "첫 출근에서 어떤 모습으로 기억되고 싶은지 말해주세요.",
      "새 팀원들에게 먼저 인사하고 모르는 것은 편하게 질문하는 사람으로 보이고 싶어요.");

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
