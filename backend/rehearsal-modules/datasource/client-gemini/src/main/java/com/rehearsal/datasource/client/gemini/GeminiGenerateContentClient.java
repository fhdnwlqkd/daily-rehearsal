package com.rehearsal.datasource.client.gemini;

import com.google.genai.types.GenerateContentConfig;
import com.rehearsal.domain.core.annotation.Description;

@Description("Gemini generateContent 호출을 감싸 테스트와 실제 SDK 호출을 분리하는 client 인터페이스")
public interface GeminiGenerateContentClient {

  String generateContent(String model, String userMessage, GenerateContentConfig config);
}
