package com.newroniq.civicarch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newroniq.civicarch.application.RuntimePolicy;
import com.newroniq.civicarch.domain.Models;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.embedding.EmbeddingModel;

class ModelsTest {
  @Test
  void healthPreservesInterruptFlag() {
    Thread.currentThread().interrupt();
    try {
      assertThat(
              new ModelHealth("http://127.0.0.1:1", "chat", "embed").health().getStatus().getCode())
          .isEqualTo("DOWN");
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void validatesVectorsAndImmutableIdentity() {
    var chat = mock(ChatModel.class);
    var embedding = mock(EmbeddingModel.class);
    var catalog = mock(ModelCatalog.class);
    when(catalog.digest("model")).thenReturn("a".repeat(64));
    var adapter = new OllamaModels(chat, embedding, "model", catalog, "configured-chat");
    assertThat(adapter.embeddingIdentity()).endsWith("/768");
    float[] good = new float[768];
    good[0] = 1;
    when(embedding.embed(anyString())).thenReturn(good);
    assertThat(adapter.embed("text")).isEqualTo(good);
    float[] invalid = new float[768];
    invalid[0] = Float.NaN;
    for (float[] vector : List.of(new float[2], new float[768], invalid)) {
      when(embedding.embed(anyString())).thenReturn(vector);
      assertThatThrownBy(() -> adapter.embed("text")).isInstanceOf(IllegalStateException.class);
    }
    when(chat.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("[1]")))));
    assertThat(adapter.select("question", "evidence")).isEqualTo("[1]");
    var prompt =
        org.mockito.ArgumentCaptor.forClass(org.springframework.ai.chat.prompt.Prompt.class);
    verify(chat).call(prompt.capture());
    assertThat(prompt.getValue().getOptions().getModel()).isEqualTo("configured-chat");
    when(catalog.digest("model")).thenReturn("b".repeat(64));
    assertThatThrownBy(adapter::embeddingIdentity).hasMessageContaining("changed");
  }

  @Test
  void deadlineCancellationAndSaturationStayBounded() throws Exception {
    var delegate = mock(Models.class);
    var metrics = new SimpleMeterRegistry();
    var defaults = RuntimePolicy.defaults();
    var policy = new RuntimePolicy(.65, 10, 20000, 1024, 1, 40, 280, 40, 32768, 768);
    var stopped = new CountDownLatch(1);
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    when(delegate.select(anyString(), anyString()))
        .thenAnswer(
            i -> {
              entered.countDown();
              try {
                release.await();
              } catch (InterruptedException _) {
                stopped.countDown();
                release.await();
              }
              return "[]";
            });
    var bounded = new BoundedModels(delegate, policy, metrics);
    try {
      assertThatThrownBy(() -> bounded.select("q", "e")).hasMessageContaining("deadline");
      assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
      assertThat(stopped.await(1, TimeUnit.SECONDS)).isTrue();
      assertThatThrownBy(() -> bounded.select("q", "e"))
          .isInstanceOf(com.newroniq.civicarch.application.QuestionService.Busy.class);
      release.countDown();
    } finally {
      release.countDown();
      bounded.close();
    }
    var normal = new BoundedModels(delegate, defaults, metrics);
    try {
      when(delegate.embed("text")).thenReturn(new float[] {1});
      when(delegate.embeddingIdentity()).thenReturn("id");
      assertThat(normal.embed("text")).containsExactly(1);
      assertThat(normal.embeddingIdentity()).isEqualTo("id");
      when(delegate.embed("bad")).thenThrow(new IllegalStateException());
      assertThatThrownBy(() -> normal.embed("bad")).hasMessageContaining("unavailable");
    } finally {
      normal.close();
    }
  }

  @Test
  void modelHealthAndCatalogReportMissingMalformedAndAvailableModels() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var body = new java.util.concurrent.atomic.AtomicReference<>("{}");
    server.createContext(
        "/api/tags",
        exchange -> {
          byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, bytes.length);
          try (var output = exchange.getResponseBody()) {
            output.write(bytes);
          }
        });
    server.start();
    String url = "http://127.0.0.1:" + server.getAddress().getPort();
    var health = new ModelHealth(url, "chat", "embed");
    var catalog = new ModelCatalog(url);
    try {
      assertThat(health.health().getStatus().getCode()).isEqualTo("DOWN");
      assertThatThrownBy(() -> catalog.digest("embed")).isInstanceOf(IllegalStateException.class);
      body.set(
          "{\"models\":[{\"name\":\"chat\"},{\"name\":\"embed\",\"digest\":\""
              + "a".repeat(64)
              + "\"}]}");
      assertThat(health.health().getStatus().getCode()).isEqualTo("UP");
      assertThat(catalog.digest("embed")).hasSize(64);
      body.set("{\"models\":[{\"name\":\"embed\",\"digest\":\"bad\"}]}");
      assertThatThrownBy(() -> catalog.digest("embed")).hasMessageContaining("digest");
      body.set("bad json");
      assertThat(health.health().getStatus().getCode()).isEqualTo("DOWN");
    } finally {
      server.stop(0);
    }
    assertThat(health.health().getStatus().getCode()).isEqualTo("DOWN");
  }
}
