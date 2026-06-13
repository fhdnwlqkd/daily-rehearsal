package com.rehearsal.datasource.client.openai;

import com.openai.models.responses.ResponseCreateParams;
import com.rehearsal.domain.core.annotation.Description;

@Description("OpenAI Responses API 호출을 추상화하는 client adapter 계약")
public interface OpenAiResponsesClient {

  String createResponse(ResponseCreateParams params);
}
