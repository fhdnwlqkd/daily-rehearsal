package com.rehearsal.api.config.ticket;

import com.rehearsal.domain.core.annotation.Description;
import java.net.URI;
import java.util.Locale;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Description("Configuration for mobile ticket download URLs")
@Getter
@ConfigurationProperties(prefix = "rehearsal.ticket")
public class TicketProperties {

  private static final String DEFAULT_DOWNLOAD_PAGE_BASE_URL = "http://localhost:3000";

  private String downloadPageBaseUrl = DEFAULT_DOWNLOAD_PAGE_BASE_URL;

  public void setDownloadPageBaseUrl(String downloadPageBaseUrl) {
    if (downloadPageBaseUrl == null || downloadPageBaseUrl.isBlank()) {
      this.downloadPageBaseUrl = DEFAULT_DOWNLOAD_PAGE_BASE_URL;
      return;
    }

    String normalized = downloadPageBaseUrl.strip();
    URI uri;
    try {
      uri = URI.create(normalized);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "Ticket download page base URL must be a valid URL", exception);
    }

    String scheme = uri.getScheme();
    boolean supportedScheme =
        scheme != null
            && (scheme.toLowerCase(Locale.ROOT).equals("http")
                || scheme.toLowerCase(Locale.ROOT).equals("https"));
    if (!supportedScheme || uri.getHost() == null) {
      throw new IllegalArgumentException(
          "Ticket download page base URL must be an absolute http(s) URL");
    }
    if (uri.getQuery() != null || uri.getFragment() != null) {
      throw new IllegalArgumentException(
          "Ticket download page base URL must not contain a query or fragment");
    }

    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    this.downloadPageBaseUrl = normalized;
  }
}
