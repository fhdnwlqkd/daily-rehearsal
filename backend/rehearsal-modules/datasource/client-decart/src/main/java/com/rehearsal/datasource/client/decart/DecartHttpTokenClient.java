package com.rehearsal.datasource.client.decart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.decart.port.DecartTokenPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.RequiredArgsConstructor;

@Description("Decart REST API를 호출해 단기 client token을 발급받는 adapter")
@RequiredArgsConstructor
public class DecartHttpTokenClient implements DecartTokenPort {

  private final String apiKey;
  private final String tokenEndpoint;
  private final ObjectMapper objectMapper;

  @Override
  public String issueClientToken() {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("Decart API key is not configured");
    }
    try {
      HttpClient httpClient = HttpClient.newHttpClient();
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(tokenEndpoint))
              .header("Authorization", "Bearer " + apiKey)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString("{}"))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new IllegalStateException(
            "Decart token API returned unexpected status: " + response.statusCode());
      }

      DecartTokenCreateResponse tokenResponse =
          objectMapper.readValue(response.body(), DecartTokenCreateResponse.class);
      return tokenResponse.apiKey();
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to issue Decart client token", e);
    }
  }
}
