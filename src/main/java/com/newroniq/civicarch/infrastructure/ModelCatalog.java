package com.newroniq.civicarch.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ModelCatalog {
  private final RestClient client;

  public ModelCatalog(@Value("${spring.ai.ollama.base-url}") String url) {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(2000);
    factory.setReadTimeout(3000);
    client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
  }

  public String digest(String name) {
    String json = client.get().uri("/api/tags").retrieve().body(String.class);
    for (var model : JsonMapper.builder().build().readTree(json).path("models")) {
      if (model.path("name").asText().equals(name)) {
        String digest = model.path("digest").asText();
        if (!digest.matches("[a-f0-9]{64}")) {
          throw new IllegalStateException("Invalid model digest");
        }
        return digest;
      }
    }
    throw new IllegalStateException("Configured model unavailable");
  }
}
