package com.rehearsal.api.config.decart;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Description("yml에서 Decart API 설정과 outfit별 스펙을 바인딩하는 설정 모델")
@Getter
@Setter
@ConfigurationProperties(prefix = "rehearsal.decart")
public class DecartProperties {

  private String apiKey = "";
  private String model = "lucy-vton-latest";
  private String tokenEndpoint = "https://api.decart.ai/v1/tokens";
  private Map<String, OutfitSpec> outfits = new LinkedHashMap<>();

  @Getter
  @Setter
  public static class OutfitSpec {
    private String prompt = "";
    private String referenceImageUrl = "";
    private boolean enhance = false;
    private String label = "";
    private String thumbnailUrl = "";
    private List<SituationType> situationTypes = List.of();
    private String outfitDirection = "";
    private boolean defaultOutfit = false;
  }
}
