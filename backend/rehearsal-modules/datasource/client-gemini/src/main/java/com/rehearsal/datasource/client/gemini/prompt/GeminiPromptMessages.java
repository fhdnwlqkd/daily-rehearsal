package com.rehearsal.datasource.client.gemini.prompt;

import com.rehearsal.domain.core.annotation.Description;

@Description("Gemini 요청에 함께 전달할 system instruction과 user message 묶음")
public record GeminiPromptMessages(
    @Description("모델 역할과 고정 규칙을 담는 system instruction") String systemInstruction,
    @Description("transcript와 runtime slot schema를 담는 user message") String userMessage) {}
