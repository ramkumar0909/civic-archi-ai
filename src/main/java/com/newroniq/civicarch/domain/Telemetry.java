package com.newroniq.civicarch.domain;

public interface Telemetry {
  enum Operation {
    QUESTION,
    RETRIEVAL,
    INGESTION
  }

  enum Outcome {
    SUCCESS,
    ABSTAIN,
    FAILURE
  }

  void recordOperation(Operation operation, Outcome outcome, long elapsedNanos);
}
