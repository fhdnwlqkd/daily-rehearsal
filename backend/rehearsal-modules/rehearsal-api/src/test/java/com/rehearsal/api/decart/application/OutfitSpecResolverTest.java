package com.rehearsal.api.decart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.api.config.decart.DecartProperties;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.decart.model.DecartSpec;
import org.junit.jupiter.api.Test;

class OutfitSpecResolverTest {

  private static final String OUTFIT_ID = "presentation_jacket_01";
  private static final String MODEL = "lucy-vton-latest";
  private static final String PROMPT = "Substitute the current top with a navy blue blazer";
  private static final String REFERENCE_IMAGE_URL = "https://asset-store/outfits/jacket_01.png";

  @Test
  void resolvesOutfitSpecByOutfitId() {
    OutfitSpecResolver resolver = resolverWithOutfit(OUTFIT_ID);

    DecartSpec spec = resolver.resolve(OUTFIT_ID);

    assertThat(spec.model()).isEqualTo(MODEL);
    assertThat(spec.prompt()).isEqualTo(PROMPT);
    assertThat(spec.referenceImageUrl()).isEqualTo(REFERENCE_IMAGE_URL);
    assertThat(spec.enhance()).isFalse();
  }

  @Test
  void throwsNotFoundForUnknownOutfitId() {
    OutfitSpecResolver resolver = resolverWithOutfit(OUTFIT_ID);

    assertThatThrownBy(() -> resolver.resolve("unknown_outfit"))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  private OutfitSpecResolver resolverWithOutfit(String outfitId) {
    DecartProperties properties = new DecartProperties();
    properties.setModel(MODEL);

    DecartProperties.OutfitSpec outfitSpec = new DecartProperties.OutfitSpec();
    outfitSpec.setPrompt(PROMPT);
    outfitSpec.setReferenceImageUrl(REFERENCE_IMAGE_URL);
    outfitSpec.setEnhance(false);
    properties.getOutfits().put(outfitId, outfitSpec);

    return new OutfitSpecResolver(properties);
  }
}
