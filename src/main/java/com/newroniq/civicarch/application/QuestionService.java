package com.newroniq.civicarch.application;

import com.newroniq.civicarch.domain.CitationPolicy;
import com.newroniq.civicarch.domain.Evidence;
import com.newroniq.civicarch.domain.Models;
import com.newroniq.civicarch.domain.Retriever;
import com.newroniq.civicarch.domain.SourceRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class QuestionService {
  private final com.newroniq.civicarch.domain.Telemetry telemetry;

  public static final String DISCLAIMER =
      "CivicArch AI is an independent open-source project. It is not affiliated with, endorsed by, or an official service of the UK government. Its answers are informational and do not constitute a compliance determination.";

  private final Models models;

  private final Retriever store;

  private final SourceRegistry sources;

  private final RuntimePolicy policy;

  private final Semaphore slots;

  public QuestionService(
      Models models,
      Retriever store,
      SourceRegistry sources,
      RuntimePolicy policy,
      com.newroniq.civicarch.domain.Telemetry telemetry) {
    this.models = models;
    this.store = store;
    this.sources = sources;
    this.policy = policy;
    this.telemetry = telemetry;
    this.slots = new Semaphore(policy.concurrentInference());
  }

  public record Citation(
      String citationId,
      String sourceKey,
      String title,
      String url,
      String contentId,
      java.time.Instant publishedUpdatedAt,
      java.time.Instant retrievedAt,
      String section,
      java.util.UUID chunkId,
      String excerpt,
      String freshness) {

    static Citation from(int i, Evidence e) {
      return new Citation(
          "S" + i,
          e.sourceKey(),
          e.title(),
          e.url(),
          e.contentId(),
          e.publishedUpdatedAt(),
          e.retrievedAt(),
          e.section(),
          e.chunkId(),
          e.excerpt(),
          e.freshness());
    }
  }

  public record Result(
      String question,
      String answer,
      String outcome,
      List<Citation> citations,
      Map<String, Object> retrieval,
      String requestId,
      String disclaimer) {}

  public Result answer(String question, int max, String requestId) {
    long started = System.nanoTime();
    var outcome = com.newroniq.civicarch.domain.Telemetry.Outcome.FAILURE;
    try {
      var result = answerInternal(question, max, requestId);
      outcome =
          result.outcome().equals("ANSWERED")
              ? com.newroniq.civicarch.domain.Telemetry.Outcome.SUCCESS
              : com.newroniq.civicarch.domain.Telemetry.Outcome.ABSTAIN;
      return result;
    } finally {
      telemetry.recordOperation(
          com.newroniq.civicarch.domain.Telemetry.Operation.QUESTION,
          outcome,
          System.nanoTime() - started);
    }
  }

  private Result answerInternal(String question, int max, String requestId) {
    if (!slots.tryAcquire()) {
      throw new Busy();
    }
    try {
      long retrievalStarted = System.nanoTime();
      var hits =
          store.retrieve(
              models.embed(question),
              models.embeddingIdentity(),
              sources.entries().stream().map(SourceRegistry.Source::sourceKey).toList(),
              Math.min(max, policy.maxSources()),
              policy.similarityFloor());
      telemetry.recordOperation(
          com.newroniq.civicarch.domain.Telemetry.Operation.RETRIEVAL,
          com.newroniq.civicarch.domain.Telemetry.Outcome.SUCCESS,
          System.nanoTime() - retrievalStarted);
      if (hits.isEmpty()) {
        return abstain(question, requestId, 0);
      }
      StringBuilder context = new StringBuilder();
      for (int i = 0; i < hits.size(); i++) {
        context.append(i + 1).append(": ").append(hits.get(i).excerpt()).append('\n');
      }
      if (context.length() > policy.promptCharacters()) {
        return abstain(question, requestId, hits.size());
      }
      String raw = models.select(question, context.toString());
      List<Integer> selected = new ArrayList<>();
      try {
        if (raw == null || raw.length() > policy.outputCharacters()) {
          return abstain(question, requestId, hits.size());
        }
        var node =
            JsonMapper.builder()
                .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build()
                .readTree(raw);
        if (!node.isArray()) {
          return abstain(question, requestId, hits.size());
        }
        for (var id : node) {
          if (!id.isIntegralNumber()) {
            return abstain(question, requestId, hits.size());
          }
          selected.add(id.intValue());
        }
        CitationPolicy.validate(selected, hits.size());
      } catch (RuntimeException _) {
        return abstain(question, requestId, hits.size());
      }
      if (selected.isEmpty()) {
        return abstain(question, requestId, hits.size());
      }
      var citations = selected.stream().map(i -> Citation.from(i, hits.get(i - 1))).toList();
      String answer =
          "Relevant published excerpts (not a compliance assessment):\n"
              + String.join(
                  "\n\n",
                  citations.stream().map(c -> "[" + c.citationId() + "] " + c.excerpt()).toList());
      return new Result(
          question, answer, "ANSWERED", citations, retrievalMetadata(hits), requestId, DISCLAIMER);
    } finally {
      slots.release();
    }
  }

  private Map<String, Object> retrievalMetadata(java.util.List<Evidence> hits) {
    var metadata = new java.util.LinkedHashMap<String, Object>();
    metadata.put("returnedChunks", hits.size());
    metadata.put(
        "indexAsOf",
        hits.stream().map(Evidence::retrievedAt).min(java.time.Instant::compareTo).orElse(null));
    return metadata;
  }

  private Result abstain(String question, String id, int hits) {
    return new Result(
        question,
        "I could not find sufficient support for that claim in the indexed GOV.UK guidance.",
        "INSUFFICIENT_EVIDENCE",
        List.of(),
        Map.of("returnedChunks", hits),
        id,
        DISCLAIMER);
  }

  public static class Busy extends RuntimeException {}
}
