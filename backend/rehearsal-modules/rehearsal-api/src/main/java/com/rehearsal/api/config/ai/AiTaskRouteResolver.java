package com.rehearsal.api.config.ai;

import com.rehearsal.api.config.ai.AiClientProperties.Provider;
import com.rehearsal.domain.core.annotation.Description;
import java.util.Objects;

@Description("전역 provider 설정과 task별 route 설정으로 실제 AI provider/model을 결정하는 서비스")
public class AiTaskRouteResolver {

  private final AiClientProperties properties;

  public AiTaskRouteResolver(AiClientProperties properties) {
    this.properties = Objects.requireNonNull(properties, "properties must not be null");
  }

  public AiTaskRoute resolve(AiTask task) {
    AiClientProperties.TaskRoute route = properties.getTasks().get(task.key());
    Provider provider = taskProvider(route);
    if (provider == null || provider == Provider.NONE) {
      return new AiTaskRoute(task, Provider.NONE, "");
    }

    String model = route == null ? "" : route.getModel();
    return new AiTaskRoute(task, provider, isBlank(model) ? defaultModel(provider) : model);
  }

  private Provider taskProvider(AiClientProperties.TaskRoute route) {
    if (route != null && route.getProvider() != null) {
      return route.getProvider();
    }
    return properties.getDefaults().getProvider();
  }

  private String defaultModel(Provider provider) {
    return switch (provider) {
      case OPENAI -> properties.getOpenai().getModel();
      case GEMINI -> properties.getGemini().getModel();
      case NONE, FAKE -> "";
    };
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
