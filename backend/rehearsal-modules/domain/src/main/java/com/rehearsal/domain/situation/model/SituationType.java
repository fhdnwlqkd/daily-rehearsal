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
      "내일의 소개팅 장면을 떠올려주세요. 누구와 어디서 만나는지, 어떤 인상을 남기고 싶은지, 편하게 나눌 대화 소재, 이번 만남에서 기대하는 점 가운데 지금 떠오르는 한두 가지만 이야기해주세요.",
      "친구 소개로 카페에서 처음 만나요. 전시 이야기를 나누며 편안하게 대화하고, 다음에도 자연스럽게 만날 수 있으면 좋겠어요."),
  INTERVIEW(
      "interview",
      "면접",
      "내일의 면접 장면을 떠올려주세요. 지원 분야와 면접 방식, 보여주고 싶은 모습, 특히 연습하고 싶은 질문, 활용하고 싶은 강점이나 경험 가운데 지금 떠오르는 한두 가지만 이야기해주세요.",
      "서비스직 일대일 면접이에요. 침착하고 책임감 있게 보이고 싶고, 갈등 상황에 어떻게 대처할지 묻는 질문이 걱정돼요."),
  FIRST_DAY(
      "first_day",
      "첫 출근",
      "내일의 첫 출근 장면을 떠올려주세요. 함께할 팀과 맡게 될 역할, 기억되고 싶은 모습, 걱정되는 순간, 잘해보고 싶은 일이나 궁금한 점 가운데 지금 떠오르는 한두 가지만 이야기해주세요.",
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
