package com.rehearsal.api.decart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.api.config.decart.DecartProperties;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.decart.model.DecartSpec;
import com.rehearsal.domain.decart.model.OutfitCandidate;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
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

  @Test
  void resolvesCandidatesFilteredBySituationTypeAndOutfitDirection() {
    DecartProperties properties = new DecartProperties();
    properties
        .getOutfits()
        .put("date_neat", outfit("데이트룩", SituationType.DATE, "neat_casual", false));
    properties
        .getOutfits()
        .put("business_formal", outfit("정장", SituationType.INTERVIEW, "formal_clean", true));
    OutfitSpecResolver resolver = new OutfitSpecResolver(properties);

    List<OutfitCandidate> candidates =
        resolver.resolveCandidates(SituationType.INTERVIEW, "formal_clean");

    assertThat(candidates).extracting(OutfitCandidate::outfitId).containsExactly("business_formal");
    assertThat(candidates.get(0).defaultOutfit()).isTrue();
  }

  @Test
  void fallsBackToSituationTypeMatchesWhenNoOutfitDirectionMatches() {
    DecartProperties properties = new DecartProperties();
    properties
        .getOutfits()
        .put("business_formal", outfit("정장", SituationType.INTERVIEW, "formal_clean", false));
    OutfitSpecResolver resolver = new OutfitSpecResolver(properties);

    List<OutfitCandidate> candidates =
        resolver.resolveCandidates(SituationType.INTERVIEW, "soft_friendly");

    assertThat(candidates).extracting(OutfitCandidate::outfitId).containsExactly("business_formal");
  }

  @Test
  void marksFirstCandidateAsDefaultWhenNoneExplicitlyMarked() {
    DecartProperties properties = new DecartProperties();
    properties
        .getOutfits()
        .put("date_neat", outfit("데이트룩", SituationType.DATE, "neat_casual", false));
    properties
        .getOutfits()
        .put("date_soft", outfit("소프트룩", SituationType.DATE, "soft_friendly", false));
    OutfitSpecResolver resolver = new OutfitSpecResolver(properties);

    List<OutfitCandidate> candidates = resolver.resolveCandidates(SituationType.DATE, null);

    assertThat(candidates).hasSize(2);
    assertThat(candidates.get(0).defaultOutfit()).isTrue();
    assertThat(candidates.get(1).defaultOutfit()).isFalse();
  }

  @Test
  void throwsNotFoundWhenNoOutfitMatchesSituationType() {
    DecartProperties properties = new DecartProperties();
    properties
        .getOutfits()
        .put("date_neat", outfit("데이트룩", SituationType.DATE, "neat_casual", false));
    OutfitSpecResolver resolver = new OutfitSpecResolver(properties);

    assertThatThrownBy(() -> resolver.resolveCandidates(SituationType.INTERVIEW, null))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }

  private DecartProperties.OutfitSpec outfit(
      String label, SituationType situationType, String outfitDirection, boolean defaultOutfit) {
    DecartProperties.OutfitSpec outfitSpec = new DecartProperties.OutfitSpec();
    outfitSpec.setLabel(label);
    outfitSpec.setThumbnailUrl("https://asset-store/thumbnails/" + label + ".png");
    outfitSpec.setSituationTypes(List.of(situationType));
    outfitSpec.setOutfitDirection(outfitDirection);
    outfitSpec.setDefaultOutfit(defaultOutfit);
    return outfitSpec;
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
