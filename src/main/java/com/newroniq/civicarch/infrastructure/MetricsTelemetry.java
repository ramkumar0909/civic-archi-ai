package com.newroniq.civicarch.infrastructure;

import com.newroniq.civicarch.domain.Telemetry;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public final class MetricsTelemetry implements Telemetry {
  private final MeterRegistry registry;

  public MetricsTelemetry(MeterRegistry registry) {
    this.registry = registry;
  }

  public void recordOperation(Operation operation, Outcome outcome, long elapsedNanos) {
    registry
        .counter("civicarch.operations", "operation", operation.name(), "outcome", outcome.name())
        .increment();
    registry
        .timer("civicarch.duration", "operation", operation.name(), "outcome", outcome.name())
        .record(elapsedNanos, TimeUnit.NANOSECONDS);
  }
}
