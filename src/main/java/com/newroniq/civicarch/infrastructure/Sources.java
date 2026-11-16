package com.newroniq.civicarch.infrastructure;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
public final class Sources implements com.newroniq.civicarch.domain.SourceRegistry {

  private final List<Source> entries;

  @SuppressWarnings("unchecked")
  public Sources(@Value("${civicarch.sources:config/sources.yml}") String file)
      throws java.io.IOException {
    try (var input = Files.newInputStream(Path.of(file))) {
      Map<String, Object> root = new Yaml().load(input);
      var keys = new HashSet<String>();
      var paths = new HashSet<String>();
      var result = new ArrayList<Source>();
      for (var row : (List<Map<String, Object>>) root.get("sources")) {
        String key = (String) row.get("sourceKey");
        String path = (String) row.get("path");
        validatePath(path);
        if (key == null || !key.matches("[a-z0-9-]+") || !keys.add(key) || !paths.add(path)) {
          throw new IllegalArgumentException("Duplicate or invalid source");
        }
        if (!Set.of("service-standard", "technology-code-of-practice").contains(row.get("topic"))) {
          throw new IllegalArgumentException("Invalid topic");
        }
        if (Boolean.TRUE.equals(row.get("enabled"))) {
          result.add(new Source(key, path));
        }
      }
      entries = List.copyOf(result);
    }
  }

  public static void validatePath(String path) {
    if (path == null
        || !path.matches("/(guidance|service-manual)/[a-z0-9/-]+")
        || path.contains("//")
        || path.endsWith("/")) {
      throw new IllegalArgumentException("Unapproved path shape");
    }
  }

  public List<Source> entries() {
    return entries;
  }
}
