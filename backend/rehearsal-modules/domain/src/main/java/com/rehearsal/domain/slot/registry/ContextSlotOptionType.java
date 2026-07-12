package com.rehearsal.domain.slot.registry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContextSlotOptionType {
  CALM_CONFIDENT("calm_confident", "차분하고 자신감 있게"),
  WARM_NATURAL("warm_natural", "따뜻하고 자연스럽게"),
  SHARP_PREPARED("sharp_prepared", "예리하고 철저하게"),
  NEAT_CASUAL("neat_casual", "단정한 캐주얼"),
  FORMAL_CLEAN("formal_clean", "깔끔한 정장"),
  SOFT_FRIENDLY("soft_friendly", "부드럽고 친근하게");

  private final String key;
  private final String label;
}
