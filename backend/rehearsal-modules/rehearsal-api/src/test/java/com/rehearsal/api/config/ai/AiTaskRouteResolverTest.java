package com.rehearsal.api.config.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.api.config.ai.AiClientProperties.Provider;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AiTaskRouteResolverTest {

  @Test
  void resolvesDefaultProviderUsingProviderDefaultModel() {
    AiClientProperties properties = new AiClientProperties();
    properties.getDefaults().setProvider(Provider.OPENAI);
    properties.getOpenai().setModel("global-openai-model");
    properties.setTasks(Map.of());

    AiTaskRoute route = new AiTaskRouteResolver(properties).resolve(AiTask.SLOT_EXTRACTION);

    assertThat(route.task()).isEqualTo(AiTask.SLOT_EXTRACTION);
    assertThat(route.provider()).isEqualTo(Provider.OPENAI);
    assertThat(route.model()).isEqualTo("global-openai-model");
  }

  @Test
  void resolvesTaskProviderUsingTaskModel() {
    AiClientProperties properties = new AiClientProperties();
    AiClientProperties.TaskRoute slotExtractionRoute = new AiClientProperties.TaskRoute();
    slotExtractionRoute.setProvider(Provider.GEMINI);
    slotExtractionRoute.setModel("slot-specific-gemini-model");
    properties.setTasks(Map.of(AiTask.SLOT_EXTRACTION.key(), slotExtractionRoute));

    AiTaskRoute route = new AiTaskRouteResolver(properties).resolve(AiTask.SLOT_EXTRACTION);

    assertThat(route.provider()).isEqualTo(Provider.GEMINI);
    assertThat(route.model()).isEqualTo("slot-specific-gemini-model");
  }

  @Test
  void resolvesTaskProviderUsingProviderDefaultModelWhenTaskModelIsBlank() {
    AiClientProperties properties = new AiClientProperties();
    properties.getGemini().setModel("global-gemini-model");
    AiClientProperties.TaskRoute slotExtractionRoute = new AiClientProperties.TaskRoute();
    slotExtractionRoute.setProvider(Provider.GEMINI);
    properties.setTasks(Map.of(AiTask.SLOT_EXTRACTION.key(), slotExtractionRoute));

    AiTaskRoute route = new AiTaskRouteResolver(properties).resolve(AiTask.SLOT_EXTRACTION);

    assertThat(route.provider()).isEqualTo(Provider.GEMINI);
    assertThat(route.model()).isEqualTo("global-gemini-model");
  }

  @Test
  void resolvesNoneWhenNoDefaultOrTaskProviderExists() {
    AiClientProperties properties = new AiClientProperties();
    properties.setTasks(Map.of());

    AiTaskRoute route = new AiTaskRouteResolver(properties).resolve(AiTask.SLOT_EXTRACTION);

    assertThat(route.provider()).isEqualTo(Provider.NONE);
    assertThat(route.model()).isEmpty();
  }

  @Test
  void resolvesFakeProviderWithoutModel() {
    AiClientProperties properties = new AiClientProperties();
    AiClientProperties.TaskRoute slotExtractionRoute = new AiClientProperties.TaskRoute();
    slotExtractionRoute.setProvider(Provider.FAKE);
    properties.setTasks(Map.of(AiTask.SLOT_EXTRACTION.key(), slotExtractionRoute));

    AiTaskRoute route = new AiTaskRouteResolver(properties).resolve(AiTask.SLOT_EXTRACTION);

    assertThat(route.provider()).isEqualTo(Provider.FAKE);
    assertThat(route.model()).isEmpty();
  }

  @Test
  void taskProviderOverridesDefaultProvider() {
    AiClientProperties properties = new AiClientProperties();
    properties.getDefaults().setProvider(Provider.OPENAI);
    properties.getGemini().setModel("global-gemini-model");
    AiClientProperties.TaskRoute slotExtractionRoute = new AiClientProperties.TaskRoute();
    slotExtractionRoute.setProvider(Provider.GEMINI);
    properties.setTasks(Map.of(AiTask.SLOT_EXTRACTION.key(), slotExtractionRoute));

    AiTaskRoute route = new AiTaskRouteResolver(properties).resolve(AiTask.SLOT_EXTRACTION);

    assertThat(route.provider()).isEqualTo(Provider.GEMINI);
    assertThat(route.model()).isEqualTo("global-gemini-model");
  }
}
