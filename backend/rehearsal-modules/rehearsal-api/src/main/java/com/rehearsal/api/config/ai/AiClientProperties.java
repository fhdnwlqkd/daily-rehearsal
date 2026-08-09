package com.rehearsal.api.config.ai;

import com.rehearsal.datasource.client.gemini.GeminiGenerateContentConfigBuilder;
import com.rehearsal.datasource.client.gemini.GeminiSlotExtractorClient;
import com.rehearsal.domain.core.annotation.Description;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Description("yml에서 AI provider 설정을 바인딩하는 설정 모델")
@Getter
@Setter
@ConfigurationProperties(prefix = "rehearsal.ai")
public class AiClientProperties {

  private Defaults defaults = new Defaults();
  private Gemini gemini = new Gemini();

  public enum Provider {
    FAKE,
    GEMINI
  }

  @Getter
  @Setter
  public static class Defaults {

    private Provider provider = Provider.GEMINI;
  }

  @Getter
  @Setter
  public static class Gemini {

    private boolean enabled = true;
    private String apiKey = "";
    private String model = GeminiSlotExtractorClient.DEFAULT_MODEL;
    private float temperature = GeminiGenerateContentConfigBuilder.DEFAULT_TEMPERATURE;
    private int thinkingBudget = GeminiGenerateContentConfigBuilder.DEFAULT_THINKING_BUDGET;
  }
}
