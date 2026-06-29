package com.rehearsal.api.decart.application;

import com.rehearsal.api.config.decart.DecartProperties;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.decart.model.DecartSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Description("outfitId를 Decart VTON 스펙(prompt, referenceImageUrl)으로 변환하는 resolver")
@Component
@RequiredArgsConstructor
public class OutfitSpecResolver {

  private final DecartProperties decartProperties;

  public DecartSpec resolve(String outfitId) {
    DecartProperties.OutfitSpec outfitSpec = decartProperties.getOutfits().get(outfitId);
    if (outfitSpec == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    return new DecartSpec(
        decartProperties.getModel(),
        outfitSpec.getPrompt(),
        outfitSpec.getReferenceImageUrl(),
        outfitSpec.isEnhance());
  }
}
