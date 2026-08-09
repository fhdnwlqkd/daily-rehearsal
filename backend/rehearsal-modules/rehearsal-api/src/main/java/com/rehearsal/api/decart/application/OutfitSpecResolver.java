package com.rehearsal.api.decart.application;

import com.rehearsal.api.config.decart.DecartProperties;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.decart.model.DecartSpec;
import com.rehearsal.domain.decart.model.OutfitCandidate;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Description(
    "outfitId를 Decart VTON 스펙(prompt, referenceImageUrl)으로 변환하고, "
        + "situationType/outfitDirection 기준으로 옷 후보 목록을 필터링하는 resolver")
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

  public List<OutfitCandidate> resolveCandidates(
      SituationType situationType, String outfitDirection) {
    List<Map.Entry<String, DecartProperties.OutfitSpec>> situationMatches =
        decartProperties.getOutfits().entrySet().stream()
            .filter(entry -> matchesSituationType(entry.getValue(), situationType))
            .toList();

    List<Map.Entry<String, DecartProperties.OutfitSpec>> directionMatches =
        situationMatches.stream()
            .filter(entry -> matchesOutfitDirection(entry.getValue(), outfitDirection))
            .toList();

    List<Map.Entry<String, DecartProperties.OutfitSpec>> candidates =
        directionMatches.isEmpty() ? situationMatches : directionMatches;

    if (candidates.isEmpty()) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    String defaultOutfitId =
        candidates.stream()
            .filter(entry -> entry.getValue().isDefaultOutfit())
            .findFirst()
            .map(Map.Entry::getKey)
            .orElseGet(() -> candidates.get(0).getKey());

    return candidates.stream()
        .map(
            entry ->
                new OutfitCandidate(
                    entry.getKey(),
                    entry.getValue().getLabel(),
                    entry.getValue().getThumbnailUrl(),
                    entry.getKey().equals(defaultOutfitId)))
        .toList();
  }

  public String resolveLabel(String outfitId) {
    DecartProperties.OutfitSpec outfitSpec = decartProperties.getOutfits().get(outfitId);
    if (outfitSpec == null || outfitSpec.getLabel().isBlank()) {
      return outfitId;
    }
    return outfitSpec.getLabel();
  }

  private boolean matchesSituationType(
      DecartProperties.OutfitSpec outfitSpec, SituationType situationType) {
    return outfitSpec.getSituationTypes().isEmpty()
        || outfitSpec.getSituationTypes().contains(situationType);
  }

  private boolean matchesOutfitDirection(
      DecartProperties.OutfitSpec outfitSpec, String outfitDirection) {
    return outfitDirection == null
        || outfitDirection.isBlank()
        || outfitSpec.getOutfitDirection().isBlank()
        || outfitSpec.getOutfitDirection().equalsIgnoreCase(outfitDirection);
  }
}
