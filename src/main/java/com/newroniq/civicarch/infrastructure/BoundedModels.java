package com.newroniq.civicarch.infrastructure;

import com.newroniq.civicarch.application.RuntimePolicy;
import com.newroniq.civicarch.domain.Models;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public final class BoundedModels implements Models {

  private final Models delegate;

  private final RuntimePolicy policy;

  private final ThreadPoolExecutor executor;

  private final MeterRegistry metrics;

  @org.springframework.beans.factory.annotation.Autowired
  public BoundedModels(OllamaModels delegate, RuntimePolicy policy, MeterRegistry metrics) {
    this((Models) delegate, policy, metrics);
  }

  BoundedModels(Models delegate, RuntimePolicy policy, MeterRegistry metrics) {
    this.delegate = delegate;
    this.policy = policy;
    this.metrics = metrics;
    executor =
        new ThreadPoolExecutor(
            policy.concurrentInference(),
            policy.concurrentInference(),
            0,
            TimeUnit.MILLISECONDS,
            new SynchronousQueue<>(),
            Thread.ofPlatform().daemon().name("inference-", 0).factory(),
            new ThreadPoolExecutor.AbortPolicy());
  }

  private <T> T call(String operation, Callable<T> action) {
    var timer = io.micrometer.core.instrument.Timer.start(metrics);
    java.util.concurrent.Future<T> future = null;
    try {
      future = executor.submit(action);
      return future.get(policy.inferenceTimeoutMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      metrics.counter("civicarch.model.failures", "reason", "timeout").increment();
      throw new IllegalStateException("Model deadline exceeded", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Model interrupted", e);
    } catch (ExecutionException e) {
      metrics.counter("civicarch.model.failures", "reason", "dependency").increment();
      throw new IllegalStateException("Model unavailable", e.getCause());
    } catch (java.util.concurrent.RejectedExecutionException _) {
      throw new com.newroniq.civicarch.application.QuestionService.Busy();
    } finally {
      if (future != null && !future.isDone()) {
        future.cancel(true);
      }
      timer.stop(metrics.timer("civicarch.model.duration", "operation", operation));
    }
  }

  public float[] embed(String text) {
    return call("embed", () -> delegate.embed(text));
  }

  public String select(String question, String evidence) {
    return call("select", () -> delegate.select(question, evidence));
  }

  public String embeddingIdentity() {
    return call("identity", delegate::embeddingIdentity);
  }

  @PreDestroy
  public void close() {
    executor.shutdownNow();
  }
}
