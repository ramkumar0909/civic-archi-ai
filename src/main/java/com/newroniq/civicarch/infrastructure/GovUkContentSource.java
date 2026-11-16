package com.newroniq.civicarch.infrastructure;

import com.newroniq.civicarch.domain.ContentSource;
import com.newroniq.civicarch.domain.PublishedContent;
import com.newroniq.civicarch.domain.SourceRegistry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class GovUkContentSource implements ContentSource {
  private static final String ACTIVE = "ACTIVE";

  private final GovUkClient client;

  public GovUkContentSource(GovUkClient client) {
    this.client = client;
  }

  public PublishedContent fetch(SourceRegistry.Source source)
      throws com.newroniq.civicarch.domain.ContentAcquisitionException, InterruptedException {
    try {
      return fetchContent(source);
    } catch (java.io.IOException | RuntimeException | GovUkClient.FetchFailure e) {
      throw new com.newroniq.civicarch.domain.ContentAcquisitionException(
          "Unable to acquire GOV.UK content", e);
    }
  }

  private PublishedContent fetchContent(SourceRegistry.Source source)
      throws java.io.IOException, InterruptedException, GovUkClient.FetchFailure {
    byte[] raw;
    try {
      raw = client.fetch(source.path());
    } catch (GovUkClient.FetchFailure failure) {
      int code = failure.status;
      if (code >= 300 && code < 400 || code == 404 || code == 410) {
        return new PublishedContent(
            null, null, null, null, null, null, code < 400 ? "REDIRECT" : "UNAVAILABLE", List.of());
      }
      throw failure;
    }
    var node = JsonMapper.builder().build().readTree(raw);
    if (!source.path().equals(node.path("base_path").asText())) {
      throw new IllegalStateException("Canonical path mismatch");
    }
    String type = node.path("document_type").asText();
    String status = classifyStatus(node, type);
    if (status.equals(ACTIVE)) {
      UUID.fromString(node.path("content_id").asText());
    }
    var sections = new ContentExtractor().extract(node.path("details"));
    if (status.equals(ACTIVE) && sections.isEmpty()) {
      status = "UNAVAILABLE";
    }
    return new PublishedContent(
        node.path("content_id").asText(null),
        node.path("title").asText(null),
        type,
        node.path("locale").asText(null),
        node.path("public_updated_at").asText(null),
        ContentExtractor.hash(new String(raw, StandardCharsets.UTF_8)),
        status,
        sections);
  }

  private String classifyStatus(tools.jackson.databind.JsonNode node, String type) {
    if (type.equals("redirect")) {
      return "REDIRECT";
    }
    if (node.path("withdrawn").asBoolean(false)
        || (node.path("withdrawn_notice").isObject() && !node.path("withdrawn_notice").isEmpty())) {
      return "WITHDRAWN";
    }
    return ACTIVE;
  }
}
