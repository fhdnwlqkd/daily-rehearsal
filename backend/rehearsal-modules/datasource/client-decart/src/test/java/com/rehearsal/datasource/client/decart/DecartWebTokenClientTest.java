package com.rehearsal.datasource.client.decart;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DecartWebTokenClientTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void createsClientTokenWithFiveMinuteTtl() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    HttpServer server = tokenServer(requestBody);

    try {
      server.start();
      String endpoint = "http://localhost:" + server.getAddress().getPort() + "/token";

      DecartTokenCreateResponse response =
          DecartWebTokenClient.of("test-api-key", endpoint).createToken();

      JsonNode body = objectMapper.readTree(requestBody.get());
      assertThat(body.path("expiresIn").asInt()).isEqualTo(300);
      assertThat(response.apiKey()).isEqualTo("test-client-token");
    } finally {
      server.stop(0);
    }
  }

  private HttpServer tokenServer(AtomicReference<String> requestBody) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/token",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] response = "{\"apiKey\":\"test-client-token\"}".getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    return server;
  }
}
