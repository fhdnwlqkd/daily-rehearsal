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
      "모두 답하지 않아도 괜찮아요. 소개팅 장면을 떠올리며 다음 중 말하기 편한 것 한두 가지만 골라 이야기해주세요. 누구와 어디서 만나는지, 어떤 인상을 남기고 싶은지, 편한 대화 소재나 원하는 마무리 중 무엇이든 좋아요. 더 필요한 내용은 제가 한 번만 짧게 여쭤볼게요.",
      "친구 소개로 카페에서 처음 만나요. 따뜻하고 편안하게 이야기하다 서로 웃으며 마치고 싶고, 전시 이야기는 편해요."),
  INTERVIEW(
      "interview",
      "면접",
      "모두 답하지 않아도 괜찮아요. 면접 장면을 떠올리며 다음 중 말하기 편한 것 한두 가지만 골라 이야기해주세요. 지원 분야나 면접 방식, 보여주고 싶은 모습, 준비하고 싶은 질문, 활용할 강점이나 경험 중 무엇이든 좋아요. 더 필요한 내용은 제가 한 번만 짧게 여쭤볼게요.",
      "서비스직 일대일 면접이에요. 침착하고 책임감 있게 보이고 싶고, 갈등 상황에 어떻게 대처할지 묻는 질문이 걱정돼요."),
  FIRST_DAY(
      "first_day",
      "첫 출근",
      "모두 답하지 않아도 괜찮아요. 첫 출근 날을 떠올리며 다음 중 말하기 편한 것 한두 가지만 골라 이야기해주세요. 팀과 역할, 기억되고 싶은 모습, 걱정되는 순간, 잘해보고 싶은 일이나 궁금한 점 중 무엇이든 좋아요. 더 필요한 내용은 제가 한 번만 짧게 여쭤볼게요.",
      "마케팅팀 신입으로 첫 출근해요. 밝고 협업적인 사람으로 기억되고 싶지만 팀 앞에서 자기소개하는 순간이 걱정돼요.");

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
