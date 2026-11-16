package com.newroniq.civicarch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newroniq.civicarch.infrastructure.EvidenceStore;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class PostgresIT {

  @Test
  void migrationPublicationAndRetrieval() throws Exception {
    try (var pg =
        new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg17")
                .asCompatibleSubstituteFor("postgres"))) {
      pg.start();
      var ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
      Flyway.configure().dataSource(ds).load().migrate();
      var jdbc = new JdbcTemplate(ds);
      var store = new EvidenceStore(jdbc, new DataSourceTransactionManager(ds));
      var source =
          new com.newroniq.civicarch.domain.SourceRegistry.Source("test", "/guidance/test");
      var payload =
          new com.newroniq.civicarch.domain.PublishedContent(
              "8a079766-b1a7-4701-998e-1eaeae8fb2a5",
              "Synthetic guidance",
              "guide",
              "en",
              null,
              "raw",
              "ACTIVE",
              List.of());
      float[] vector = new float[768];
      vector[0] = 1;
      var chunks =
          List.of(
              new com.newroniq.civicarch.domain.PublishedContent.Section(
                  "Heading", "Synthetic guidance text"));
      store.publish(source, payload, "raw", "v1", chunks, List.of(vector), "test-model");
      assertThat(store.unchanged("test", "v1")).isTrue();
      store.publish(source, payload, "raw", "v1", chunks, List.of(vector), "test-model");
      assertThat(jdbc.queryForObject("SELECT count(*) FROM document_chunk", Integer.class))
          .isEqualTo(1);
      store.publish(source, payload, "raw2", "v2", chunks, List.of(vector), "test-model");
      assertThat(store.retrieve(vector, "test-model", List.of("test"), 5, 0.5)).hasSize(1);
      store.state(source, "ERROR", "Synthetic timeout");
      assertThat(store.retrieve(vector, "test-model", List.of("test"), 5, 0.5)).hasSize(1);
      var before = store.retrieve(vector, "test-model", List.of("test"), 5, 0.5).getFirst();
      var invalidVectors = List.of(new float[2]);
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  store.publish(
                      source, payload, "bad", "partial", chunks, invalidVectors, "test-model"))
          .isInstanceOf(Exception.class);
      assertThat(store.retrieve(vector, "test-model", List.of("test"), 5, 0.5).getFirst().chunkId())
          .isEqualTo(before.chunkId());
      assertThat(store.retrieve(vector, "test-model", List.of(), 5, 0.5)).isEmpty();
      assertThat(store.retrieve(vector, "test-model", List.of("disabled"), 5, 0.5)).isEmpty();
      var sourceKeys = List.of("test");
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () -> store.retrieve(vector, "wrong-model", sourceKeys, 5, 0.5))
          .hasMessageContaining("reindex");
      var multipleKeys = List.of("disabled", "test", "test') OR TRUE --");
      assertThat(store.retrieve(vector, "test-model", multipleKeys, 5, 0.5)).hasSize(1);
      assertThat(store.retrieve(vector, "test-model", List.of("test') OR TRUE --"), 5, 0.5))
          .isEmpty();
      var updated =
          new com.newroniq.civicarch.domain.PublishedContent(
              payload.contentId(),
              "Updated title",
              "guide",
              "en",
              "2026-11-01T00:00:00Z",
              "newRaw",
              "ACTIVE",
              List.of());
      store.refresh("test", updated);
      assertThat(store.retrieve(vector, "test-model", List.of("test"), 5, 0.5).getFirst().title())
          .isEqualTo("Updated title");
      var run = store.startRun();
      org.assertj.core.api.Assertions.assertThatThrownBy(store::startRun)
          .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
      store.finishRun(run, 1, 1, 0, 0, 0);
      var next = store.startRun();
      store.finishRun(next, 1, 0, 0, 0, 1);
      var models = mock(com.newroniq.civicarch.domain.Models.class);
      when(models.embeddingIdentity()).thenReturn("test-model");
      when(models.embed(org.mockito.ArgumentMatchers.anyString())).thenReturn(vector);
      com.newroniq.civicarch.domain.SourceRegistry registry = () -> List.of(source);
      com.newroniq.civicarch.domain.ContentSource fetch =
          ignored ->
              new com.newroniq.civicarch.domain.PublishedContent(
                  payload.contentId(),
                  payload.title(),
                  "guide",
                  "en",
                  null,
                  "raw",
                  "ACTIVE",
                  chunks);
      var ingestion =
          new com.newroniq.civicarch.application.IngestionService(
              registry,
              fetch,
              store,
              models,
              com.newroniq.civicarch.application.RuntimePolicy.defaults(),
              new com.newroniq.civicarch.infrastructure.MetricsTelemetry(
                  new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
      ingestion.ingest();
      ingestion.ingest();
      verify(models, times(1)).embed(org.mockito.ArgumentMatchers.anyString());
      store.state(source, "WITHDRAWN", null);
      assertThat(store.retrieve(vector, "test-model", List.of("test"), 5, 0.5)).isEmpty();
    }
  }
}
