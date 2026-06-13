package com.rehearsal.datasource.client.openai.prompt;

import com.rehearsal.domain.core.annotation.Description;

@Description("OpenAI 요청에 함께 전달할 developer message와 user message 묶음")
public record OpenAiPromptMessages(
    @Description("프롬프트 계약 종류") OpenAiPromptType promptType,
    @Description("모델 역할과 고정 규칙을 담는 developer message") String developerMessage,
    @Description("transcript와 runtime slot schema를 담는 user message") String userMessage) {}
