package com.rehearsal.datasource.client.decart;

import com.rehearsal.domain.core.annotation.Description;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Description("WebClient를 사용해 Decart token 발급 API를 실제로 호출하는 client 구현체")
@RequiredArgsConstructor
public class DecartWebTokenClient implements DecartTokenClient {

  private final WebClient webClient;

  private static final String API_KEY_HEADER = "x-api-key";
  private static final int CLIENT_TOKEN_TTL_SECONDS = 300;

  public static DecartWebTokenClient of(String apiKey, String tokenEndpoint) {
    WebClient webClient =
        WebClient.builder()
            .baseUrl(tokenEndpoint)
            .defaultHeader(API_KEY_HEADER, apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    return new DecartWebTokenClient(webClient);
  }

  @Override
  public DecartTokenCreateResponse createToken() {
    return webClient
        .post()
        .bodyValue(Map.of("expiresIn", CLIENT_TOKEN_TTL_SECONDS))
        .retrieve()
        .onStatus(
            status -> !status.is2xxSuccessful(),
            res ->
                res.bodyToMono(String.class)
                    .map(
                        body ->
                            new IllegalStateException(
                                "Decart token API returned unexpected status: "
                                    + res.statusCode())))
        .bodyToMono(DecartTokenCreateResponse.class)
        .block();
  }
}
