package com.newroniq.civicarch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newroniq.civicarch.application.QuestionService;
import com.newroniq.civicarch.domain.Evidence;
import com.newroniq.civicarch.domain.Models;
import com.newroniq.civicarch.infrastructure.EvidenceStore;
import com.newroniq.civicarch.infrastructure.Sources;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestionServiceTest {

  @Test
  void emptyRetrievalNeverInvokesChat() throws Exception {
    var models = mock(Models.class);
    var store = mock(EvidenceStore.class);
    var sources = new Sources("config/sources.yml");
    when(models.embed(anyString())).thenReturn(new float[] {1});
    when(models.embeddingIdentity()).thenReturn("test");
    var service =
        new QuestionService(
            models,
            store,
            sources,
            com.newroniq.civicarch.application.RuntimePolicy.defaults(),
            new com.newroniq.civicarch.infrastructure.MetricsTelemetry(
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
    assertThat(service.answer("No evidence", 5, "test").outcome())
        .isEqualTo("INSUFFICIENT_EVIDENCE");
    verify(models, never()).select(anyString(), anyString());
  }

  @Test
  void selectedEvidenceIsRenderedExactlyAndInvalidOutputAbstains() throws Exception {
    var models = mock(Models.class);
    var store = mock(EvidenceStore.class);
    var sources = new Sources("config/sources.yml");
    when(models.embed(anyString())).thenReturn(new float[] {1});
    when(models.embeddingIdentity()).thenReturn("test");
    var evidence =
        new Evidence(
            UUID.randomUUID(),
            "test",
            "Title",
            "https://www.gov.uk/guidance/test",
            "id",
            null,
            Instant.now(),
            "Heading",
            "Only this exact source text.",
            "ACTIVE",
            0.9);
    when(store.retrieve(any(), anyString(), anyList(), anyInt(), anyDouble()))
        .thenReturn(List.of(evidence));
    var service =
        new QuestionService(
            models,
            store,
            sources,
            com.newroniq.civicarch.application.RuntimePolicy.defaults(),
            new com.newroniq.civicarch.infrastructure.MetricsTelemetry(
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
    for (String invalid :
        List.of("[999]", "[1,1]", "[1.5]", "{\"url\":\"https://evil.test\"}", "made up answer")) {
      when(models.select(anyString(), anyString())).thenReturn(invalid);
      assertThat(service.answer("Question", 5, "test").citations()).isEmpty();
    }
    when(models.select(anyString(), anyString())).thenReturn("[1]");
    var result = service.answer("Question", 5, "test");
    assertThat(result.outcome()).isEqualTo("ANSWERED");
    assertThat(result.citations().getFirst().chunkId()).isEqualTo(evidence.chunkId());
    assertThat(result.answer()).contains(evidence.excerpt());
  }
}
