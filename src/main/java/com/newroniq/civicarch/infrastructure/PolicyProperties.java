package com.newroniq.civicarch.infrastructure;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "civicarch.fetch")
@Validated
public class PolicyProperties {

  @Min(1)
  @Max(3)
  private int attempts = 3;

  @Min(1000)
  @Max(60000)
  private int intervalMillis = 1000;

  @Min(1000)
  @Max(30000)
  private int connectTimeoutMillis = 5000;

  @Min(1000)
  @Max(60000)
  private int readTimeoutMillis = 20000;

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int value) {
    attempts = value;
  }

  public int getIntervalMillis() {
    return intervalMillis;
  }

  public void setIntervalMillis(int value) {
    intervalMillis = value;
  }

  public int getConnectTimeoutMillis() {
    return connectTimeoutMillis;
  }

  public void setConnectTimeoutMillis(int value) {
    connectTimeoutMillis = value;
  }

  public int getReadTimeoutMillis() {
    return readTimeoutMillis;
  }

  public void setReadTimeoutMillis(int value) {
    readTimeoutMillis = value;
  }
}
