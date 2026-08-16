package com.rehearsal.api.config.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TicketPropertiesTest {

  @Test
  void trimsWhitespaceAndTrailingSlashFromDownloadPageBaseUrl() {
    TicketProperties properties = new TicketProperties();

    properties.setDownloadPageBaseUrl("  https://example.com/  ");

    assertThat(properties.getDownloadPageBaseUrl()).isEqualTo("https://example.com");
  }

  @Test
  void usesSafeDefaultWhenDownloadPageBaseUrlIsBlank() {
    TicketProperties properties = new TicketProperties();

    properties.setDownloadPageBaseUrl("   ");

    assertThat(properties.getDownloadPageBaseUrl()).isEqualTo("http://localhost:3000");
  }

  @Test
  void rejectsDownloadPageBaseUrlWithoutHttpScheme() {
    TicketProperties properties = new TicketProperties();

    assertThatThrownBy(() -> properties.setDownloadPageBaseUrl("example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("absolute http(s) URL");
  }

  @Test
  void rejectsDownloadPageBaseUrlWithQuery() {
    TicketProperties properties = new TicketProperties();

    assertThatThrownBy(() -> properties.setDownloadPageBaseUrl("https://example.com?source=ticket"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("query or fragment");
  }
}
