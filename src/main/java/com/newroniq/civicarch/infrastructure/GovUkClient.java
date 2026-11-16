package com.newroniq.civicarch.infrastructure;

import java.net.URI;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public final class GovUkClient {

  private final PolicyProperties policy;

  @org.springframework.beans.factory.annotation.Autowired
  public GovUkClient(PolicyProperties policy) {
    this(
        policy,
        uri -> (javax.net.ssl.HttpsURLConnection) uri.toURL().openConnection(),
        Thread::sleep);
  }

  @FunctionalInterface
  interface ConnectionFactory {
    javax.net.ssl.HttpsURLConnection open(URI uri) throws java.io.IOException;
  }

  @FunctionalInterface
  interface Sleeper {
    void sleep(long millis) throws InterruptedException;
  }

  private final ConnectionFactory connections;
  private final Sleeper sleeper;

  GovUkClient(PolicyProperties policy, ConnectionFactory connections, Sleeper sleeper) {
    this.policy = policy;
    this.connections = connections;
    this.sleeper = sleeper;
  }

  private long last;

  public synchronized byte[] fetch(String path)
      throws java.io.IOException, InterruptedException, FetchFailure {
    Sources.validatePath(path);
    for (int attempt = 0; attempt < policy.getAttempts(); attempt++) {
      long wait = policy.getIntervalMillis() - (System.currentTimeMillis() - last);
      if (wait > 0) {
        sleeper.sleep(wait);
      }
      last = System.currentTimeMillis();
      var connection = connections.open(URI.create("https://www.gov.uk/api/content" + path));
      connection.setInstanceFollowRedirects(false);
      connection.setConnectTimeout(policy.getConnectTimeoutMillis());
      connection.setReadTimeout(policy.getReadTimeoutMillis());
      connection.setRequestProperty("User-Agent", "CivicArch-AI/0.1 public-guidance-research");
      connection.setRequestProperty("Accept", "application/json");
      int status = connection.getResponseCode();
      try (var stream = status < 400 ? connection.getInputStream() : connection.getErrorStream()) {
        if (status == 200) {
          byte[] body = stream.readNBytes(2_000_001);
          if (body.length > 2_000_000) {
            throw new IllegalStateException("Content exceeds limit");
          }
          return body;
        }
        if ((status == 429 || status >= 500) && attempt + 1 < policy.getAttempts()) {
          sleeper.sleep((1L << attempt) * 1000 + ThreadLocalRandom.current().nextInt(250));
          continue;
        }
        throw new FetchFailure(status);
      } finally {
        connection.disconnect();
      }
    }
    throw new IllegalStateException("Fetch exhausted");
  }

  public static final class FetchFailure extends Exception {

    public final int status;

    FetchFailure(int status) {
      super("GOV.UK HTTP " + status);
      this.status = status;
    }
  }
}
