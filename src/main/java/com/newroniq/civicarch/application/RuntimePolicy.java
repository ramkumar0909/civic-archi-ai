package com.newroniq.civicarch.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("civicarch.runtime")
public record RuntimePolicy(
    @DefaultValue("0.65") double similarityFloor,
    @DefaultValue("10") int maxSources,
    @DefaultValue("20000") int promptCharacters,
    @DefaultValue("1024") int outputCharacters,
    @DefaultValue("2") int concurrentInference,
    @DefaultValue("60000") long inferenceTimeoutMillis,
    @DefaultValue("280") int chunkWords,
    @DefaultValue("40") int overlapWords,
    @DefaultValue("32768") int requestBytes,
    @DefaultValue("768") int embeddingDimensions) {

  public RuntimePolicy {
    if (similarityFloor < 0
        || similarityFloor > 1
        || !Double.isFinite(similarityFloor)
        || maxSources < 1
        || maxSources > 10
        || promptCharacters < 256
        || promptCharacters > 40000
        || outputCharacters < 16
        || outputCharacters > 8192
        || concurrentInference < 1
        || concurrentInference > 8
        || inferenceTimeoutMillis < 1
        || inferenceTimeoutMillis > 120000
        || chunkWords < 1
        || chunkWords > 500
        || overlapWords < 0
        || overlapWords >= chunkWords
        || requestBytes < 1024
        || requestBytes > 65536
        || embeddingDimensions != 768) {
      throw new IllegalArgumentException("Inconsistent runtime bounds; schema dimension is 768");
    }
  }

  public static RuntimePolicy defaults() {
    return new RuntimePolicy(.65, 10, 20000, 1024, 2, 60000, 280, 40, 32768, 768);
  }
}
