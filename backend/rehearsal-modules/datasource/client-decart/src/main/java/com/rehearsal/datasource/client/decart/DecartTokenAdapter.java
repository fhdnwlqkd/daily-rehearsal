package com.rehearsal.datasource.client.decart;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.decart.port.DecartTokenPort;
import lombok.RequiredArgsConstructor;

@Description("DecartTokenPort를 구현하고 DecartTokenClient에 실제 호출을 위임하는 adapter")
@RequiredArgsConstructor
public class DecartTokenAdapter implements DecartTokenPort {

  private final String apiKey;
  private final DecartTokenClient decartTokenClient;

  @Override
  public String issueClientToken() {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("Decart API key is not configured");
    }

    DecartTokenCreateResponse response = decartTokenClient.createToken();

    if (response == null || response.apiKey() == null) {
      throw new IllegalStateException("Decart token API returned empty response");
    }

    return response.apiKey();
  }
}
