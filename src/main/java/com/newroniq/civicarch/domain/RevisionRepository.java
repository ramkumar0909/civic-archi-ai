package com.newroniq.civicarch.domain;

import java.util.List;
import java.util.UUID;

public interface RevisionRepository {

  boolean unchanged(String key, String hash);

  void refresh(String key, PublishedContent content);

  void state(SourceRegistry.Source source, String status, String error);

  void publish(
      SourceRegistry.Source source,
      PublishedContent content,
      String payloadHash,
      String hash,
      List<PublishedContent.Section> chunks,
      List<float[]> vectors,
      String model);

  UUID startRun();

  void finishRun(UUID id, int attempted, int changed, int unchanged, int skipped, int failed);
}
