package com.newroniq.civicarch.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component("models")
final class ModelHealth implements HealthIndicator {

  private final URI uri;

  private final String chat;
  private final String embedding;

  ModelHealth(
      @Value("${spring.ai.ollama.base-url}") String url,
      @Value("${spring.ai.ollama.chat.options.model}") String chat,
      @Value("${spring.ai.ollama.embedding.options.model}") String embedding) {
    this.uri = URI.create(url + "/api/tags");
    this.chat = chat;
    this.embedding = embedding;
  }

  public Health health() {
    try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()) {
      var response =
          client.send(
              HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).GET().build(),
              HttpResponse.BodyHandlers.ofString());
      boolean hasChat = false;
      boolean hasEmbedding = false;
      for (var model : JsonMapper.builder().build().readTree(response.body()).path("models")) {
        String name = model.path("name").asText();
        hasChat |= name.equals(chat);
        hasEmbedding |= name.equals(embedding);
      }
      return response.statusCode() == 200 && hasChat && hasEmbedding
          ? Health.up().build()
          : Health.down().build();
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      return Health.down().build();
    } catch (java.io.IOException | RuntimeException _) {
      return Health.down().build();
    }
  }
}
