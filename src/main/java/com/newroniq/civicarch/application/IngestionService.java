package com.newroniq.civicarch.application;

import com.newroniq.civicarch.domain.ChunkPolicy;
import com.newroniq.civicarch.domain.ContentSource;
import com.newroniq.civicarch.domain.Models;
import com.newroniq.civicarch.domain.RevisionRepository;
import com.newroniq.civicarch.domain.SourceRegistry;
import java.util.ArrayList;
import org.springframework.stereotype.Service;

@Service
public class IngestionService {
  private final com.newroniq.civicarch.domain.Telemetry telemetry;

  private final RuntimePolicy policy;

  private final SourceRegistry sources;

  private final ContentSource client;

  private final RevisionRepository store;

  private final Models models;

  public IngestionService(
      SourceRegistry sources,
      ContentSource client,
      RevisionRepository store,
      Models models,
      RuntimePolicy policy,
      com.newroniq.civicarch.domain.Telemetry telemetry) {
    this.policy = policy;
    this.telemetry = telemetry;
    this.sources = sources;
    this.client = client;
    this.store = store;
    this.models = models;
  }

  public void ingest() {
    long started = System.nanoTime();
    var outcome = com.newroniq.civicarch.domain.Telemetry.Outcome.FAILURE;
    try {
      ingestInternal();
      outcome = com.newroniq.civicarch.domain.Telemetry.Outcome.SUCCESS;
    } finally {
      telemetry.recordOperation(
          com.newroniq.civicarch.domain.Telemetry.Operation.INGESTION,
          outcome,
          System.nanoTime() - started);
    }
  }

  private enum DocumentOutcome {
    CHANGED,
    UNCHANGED,
    SKIPPED
  }

  private void ingestInternal() {
    var run = store.startRun();
    int changed = 0;
    int unchanged = 0;
    int skipped = 0;
    int failed = 0;
    try {
      for (var source : sources.entries()) {
        try {
          switch (ingestDocument(source)) {
            case CHANGED -> changed++;
            case UNCHANGED -> unchanged++;
            case SKIPPED -> skipped++;
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          failed++;
          store.state(source, "ERROR", "InterruptedException");
          throw new IllegalStateException("Ingestion interrupted", e);
        } catch (com.newroniq.civicarch.domain.ContentAcquisitionException | RuntimeException e) {
          store.state(source, "ERROR", e.getClass().getSimpleName());
          failed++;
        }
      }
    } finally {
      store.finishRun(run, sources.entries().size(), changed, unchanged, skipped, failed);
    }
    if (failed > 0) {
      throw new IllegalStateException("Ingestion incomplete; inspect ingestion_run");
    }
  }

  private DocumentOutcome ingestDocument(SourceRegistry.Source source)
      throws com.newroniq.civicarch.domain.ContentAcquisitionException, InterruptedException {
    var content = client.fetch(source);
    if (!content.status().equals("ACTIVE")) {
      store.state(source, content.status(), null);
      return DocumentOutcome.SKIPPED;
    }
    String model = models.embeddingIdentity();
    String hash =
        ChunkPolicy.hash(
            "extract1/chunk"
                + policy.chunkWords()
                + "-"
                + policy.overlapWords()
                + "/"
                + model
                + "/"
                + content.sections());
    if (store.unchanged(source.sourceKey(), hash)) {
      store.refresh(source.sourceKey(), content);
      return DocumentOutcome.UNCHANGED;
    }
    var chunks = ChunkPolicy.chunks(content.sections(), policy.chunkWords(), policy.overlapWords());
    var vectors = new ArrayList<float[]>();
    for (var chunk : chunks) {
      vectors.add(models.embed(chunk.text()));
    }
    store.publish(source, content, content.payloadHash(), hash, chunks, vectors, model);
    return DocumentOutcome.CHANGED;
  }
}
