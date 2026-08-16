package com.rehearsal.domain.slot.registry;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContextSlotOptionType {
  // desired_persona
  CALM_CONFIDENT("calm_confident", "차분하고 자신감 있게"),
  WARM_NATURAL("warm_natural", "따뜻하고 자연스럽게"),
  SHARP_PREPARED("sharp_prepared", "또렷하고 준비된 모습으로"),
  CURIOUS_ENGAGED("curious_engaged", "호기심 있고 적극적으로"),
  HONEST_GROUNDED("honest_grounded", "솔직하고 진정성 있게"),
  ENERGETIC_POSITIVE("energetic_positive", "밝고 긍정적으로"),
  THOUGHTFUL_CONSIDERATE("thoughtful_considerate", "사려 깊고 배려 있게"),
  PROFESSIONAL_RELIABLE("professional_reliable", "전문적이고 믿음직하게"),
  COLLABORATIVE_OPEN("collaborative_open", "협업적이고 열린 태도로"),

  // response_style
  CONCISE_DIRECT("concise_direct", "짧고 핵심부터"),
  RELAXED_CONVERSATIONAL("relaxed_conversational", "편안한 대화체로"),
  STRUCTURED_EVIDENCE("structured_evidence", "근거와 순서를 갖춰"),
  LISTEN_AND_RESPOND("listen_and_respond", "상대 말을 듣고 반응하며"),
  QUESTION_AND_EXPAND("question_and_expand", "질문을 주고받으며"),
  EMPATHETIC_RESPONSIVE("empathetic_responsive", "공감하며 반응하게"),
  ASSERTIVE_CLEAR("assertive_clear", "분명하고 주도적으로"),
  HUMBLE_HONEST("humble_honest", "겸손하고 솔직하게"),

  // familiarity_level
  FIRST_TIME("first_time", "처음 경험하는 상황"),
  LIMITED_EXPERIENCE("limited_experience", "경험이 거의 없는 상황"),
  SOME_EXPERIENCE("some_experience", "몇 번 경험한 상황"),
  VERY_FAMILIAR("very_familiar", "익숙하게 경험한 상황"),

  // outfit_direction — outfit candidate 설정과 연결된 안정적인 key다.
  NEAT_CASUAL("neat_casual", "단정한 캐주얼"),
  FORMAL_CLEAN("formal_clean", "깔끔한 정장"),
  SOFT_FRIENDLY("soft_friendly", "부드럽고 친근하게");

  private final String key;
  private final String label;

  public static Optional<ContextSlotOptionType> findByKey(String key) {
    if (key == null || key.isBlank()) {
      return Optional.empty();
    }
    return Arrays.stream(values()).filter(option -> option.key.equals(key.strip())).findFirst();
  }
}
