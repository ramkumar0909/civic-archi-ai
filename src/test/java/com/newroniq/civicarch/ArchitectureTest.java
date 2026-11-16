package com.newroniq.civicarch;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  @Test
  void applicationAndDomainDoNotDependOnAdapters() throws Exception {
    for (String layer : new String[] {"application", "domain"}) {
      try (var files = Files.walk(Path.of("src/main/java/com/newroniq/civicarch", layer))) {
        for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
          String source = Files.readString(file);
          assertThat(source)
              .doesNotContain(
                  "com.newroniq.civicarch.infrastructure",
                  "org.springframework.jdbc",
                  "org.springframework.ai");
          if (layer.equals("domain")) {
            assertThat(source).doesNotContain("org.springframework", "tools.jackson", "org.jsoup");
          }
        }
      }
    }
  }
}
