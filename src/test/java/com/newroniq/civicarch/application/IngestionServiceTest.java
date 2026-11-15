package com.newroniq.civicarch.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newroniq.civicarch.domain.ContentSource;
import com.newroniq.civicarch.domain.Models;
import com.newroniq.civicarch.domain.PublishedContent;
import com.newroniq.civicarch.domain.RevisionRepository;
import com.newroniq.civicarch.domain.SourceRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IngestionServiceTest {
  @Test
  void interruptionStopsRunAndPreservesInterruptFlag() throws Exception {
    var first = new SourceRegistry.Source("first", "/guidance/first");
    var second = new SourceRegistry.Source("second", "/guidance/second");
    SourceRegistry registry = () -> List.of(first, second);
    var content = mock(ContentSource.class);
    var store = mock(RevisionRepository.class);
    var models = mock(Models.class);
    var run = UUID.randomUUID();
    when(store.startRun()).thenReturn(run);
    when(content.fetch(first)).thenThrow(new InterruptedException("cancelled"));
    var service =
        new IngestionService(
            registry,
            content,
            store,
            models,
            RuntimePolicy.defaults(),
            new com.newroniq.civicarch.infrastructure.MetricsTelemetry(
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
    try {
      assertThatThrownBy(service::ingest)
          .hasMessage("Ingestion interrupted")
          .hasCauseInstanceOf(InterruptedException.class);
      org.assertj.core.api.Assertions.assertThat(Thread.currentThread().isInterrupted()).isTrue();
      verify(content, never()).fetch(second);
      verify(store).finishRun(run, 2, 0, 0, 0, 1);
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void changedUnchangedUnavailableAndFailure() throws Exception {
    for (String state :
        List.of("changed", "unchanged", "WITHDRAWN", "REDIRECT", "UNAVAILABLE", "failure")) {
      var source = new SourceRegistry.Source("test", "/guidance/test");
      SourceRegistry registry = () -> List.of(source);
      var content = mock(ContentSource.class);
      var store = mock(RevisionRepository.class);
      var models = mock(Models.class);
      UUID run = UUID.randomUUID();
      when(store.startRun()).thenReturn(run);
      when(models.embeddingIdentity()).thenReturn("digest");
      when(models.embed(anyString())).thenReturn(new float[] {1});
      var published =
          new PublishedContent(
              "id",
              "title",
              "guide",
              "en",
              null,
              "raw",
              List.of("changed", "unchanged", "failure").contains(state) ? "ACTIVE" : state,
              List.of(new PublishedContent.Section("heading", "source text")));
      when(content.fetch(source)).thenReturn(published);
      when(store.unchanged(anyString(), anyString())).thenReturn(state.equals("unchanged"));
      var service =
          new IngestionService(
              registry,
              content,
              store,
              models,
              RuntimePolicy.defaults(),
              new com.newroniq.civicarch.infrastructure.MetricsTelemetry(
                  new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
      if (state.equals("failure")) {
        when(content.fetch(source)).thenThrow(new IllegalStateException());
        assertThatThrownBy(service::ingest).hasMessageContaining("incomplete");
        verify(store).state(source, "ERROR", "IllegalStateException");
      } else {
        new IngestionJob(service).run();
      }
      if (state.equals("unchanged")) {
        verify(store).refresh("test", published);
        verify(models, never()).embed(anyString());
      }
      if (state.equals("changed")) {
        verify(store)
            .publish(
                eq(source),
                eq(published),
                eq("raw"),
                anyString(),
                anyList(),
                anyList(),
                eq("digest"));
      }
    }
  }
}
