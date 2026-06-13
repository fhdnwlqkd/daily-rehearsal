package com.rehearsal.api.config.ai;

import com.rehearsal.api.config.ai.AiClientProperties.Provider;
import com.rehearsal.domain.core.annotation.Description;

@Description("특정 AI 작업을 수행할 provider와 model 선택 결과")
public record AiTaskRoute(AiTask task, Provider provider, String model) {}
