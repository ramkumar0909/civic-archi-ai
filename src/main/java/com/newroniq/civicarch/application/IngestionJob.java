package com.newroniq.civicarch.application;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("ingest")
final class IngestionJob implements CommandLineRunner {

  private final IngestionService service;

  IngestionJob(IngestionService service) {
    this.service = service;
  }

  public void run(String... args) {
    service.ingest();
  }
}
